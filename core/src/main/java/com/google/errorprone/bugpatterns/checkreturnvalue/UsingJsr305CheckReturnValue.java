/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.code.Type;

/**
 * Checker that recommends using ErrorProne's version of {@code @CheckReturnValue} over the version
 * in JSR305 (which is defunct).
 */
@BugPattern(
    summary = "Prefer ErrorProne's @CheckReturnValue over JSR305's version.",
    severity = WARNING)
public final class UsingJsr305CheckReturnValue extends BugChecker implements ImportTreeMatcher {
  private static final String EP_CRV = "com.google.errorprone.annotations.CheckReturnValue";
  private static final String JSR305_CRV = "javax.annotation.CheckReturnValue";

  private static final Supplier<Type> JSR305_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString(JSR305_CRV));

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    Type jsr305Type = JSR305_TYPE.get(state);
    if (isSameType(getType(tree.getQualifiedIdentifier()), jsr305Type, state)) {
      // TODO(kak): the JSR305 version of @CheckReturnValue has an element named `when` and the
      // ErrorProne version does not. Technically, this means the import swap is not 100% safe,
      // but we have never actually seen `when` get used in practice.
      SuggestedFix fix = SuggestedFix.builder().removeImport(JSR305_CRV).addImport(EP_CRV).build();
      return describeMatch(tree, fix);
    }
    return Description.NO_MATCH;
  }

  // TODO(kak): we also may want to match on fully qualified JSR305 CRV annotations, for example:
  //   @javax.annotation.CheckReturnValue public String frobber() { ... }
  // To do so, we'd need to look for IdentifierTrees and MemberSelectTrees.
  // However, that style is very very uncommon.
}
