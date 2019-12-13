/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.util.Name;

/** Makes sure that you are not extending a class that has @AutoValue as an annotation. */
@BugPattern(
    name = "ExtendsAutoValue",
    summary = "Do not extend an @AutoValue/@AutoOneOf class in non-generated code.",
    severity = SeverityLevel.ERROR)
public final class ExtendsAutoValue extends BugChecker implements ClassTreeMatcher {

  private static final Supplier<ImmutableSet<Name>> AUTOS =
      VisitorState.memoize(
          s ->
              ImmutableSet.of(
                  s.getName("com.google.auto.value.AutoValue"),
                  s.getName("com.google.auto.value.AutoOneOf")));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {

    if (!ASTHelpers.getGeneratedBy(ASTHelpers.getSymbol(tree), state).isEmpty()) {
      // Skip generated code. Yes, I know we can do this via a flag but we should always ignore
      // generated code, so to be sure, manually check it.
      return Description.NO_MATCH;
    }

    if (tree.getExtendsClause() == null) {
      // Doesn't extend anything, can't possibly be a violation.
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.annotationsAmong(
            ASTHelpers.getSymbol(tree.getExtendsClause()), AUTOS.get(state), state)
        .isEmpty()) {
      // Violation: one of its superclasses extends AutoValue.
      return buildDescription(tree).build();
    }

    return Description.NO_MATCH;
  }
}
