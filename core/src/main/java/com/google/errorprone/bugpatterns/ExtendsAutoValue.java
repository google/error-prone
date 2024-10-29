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

import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Name;
import java.util.stream.Stream;
import javax.inject.Inject;

/** Makes sure that you are not extending a class that has @AutoValue as an annotation. */
@BugPattern(
    summary = "Do not extend an @AutoValue-like classes in non-generated code.",
    severity = SeverityLevel.ERROR)
public final class ExtendsAutoValue extends BugChecker implements ClassTreeMatcher {

  private static final Supplier<ImmutableSet<Name>> AUTOS =
      VisitorState.memoize(
          s ->
              ImmutableSet.of(
                  s.getName("com.google.auto.value.AutoOneOf"),
                  s.getName("com.google.auto.value.AutoValue")));

  private static final Supplier<ImmutableSet<Name>> AUTOS_AND_BUILDERS =
      VisitorState.memoize(
          s ->
              ImmutableSet.of(
                  s.getName("com.google.auto.value.AutoBuilder"),
                  s.getName("com.google.auto.value.AutoOneOf"),
                  s.getName("com.google.auto.value.AutoValue"),
                  s.getName("com.google.auto.value.AutoValue$Builder")));

  private final boolean testBuilders;
  private final Supplier<ImmutableSet<Name>> autos;

  @Inject
  ExtendsAutoValue(ErrorProneFlags flags) {
    this.testBuilders = flags.getBoolean("ExtendsAutoValue:builders").orElse(true);
    this.autos = testBuilders ? AUTOS_AND_BUILDERS : AUTOS;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getExtendsClause() == null
        && (!testBuilders || tree.getImplementsClause().isEmpty())) {
      return Description.NO_MATCH;
    }

    Stream<Tree> parents =
        Stream.concat(
            Stream.ofNullable(tree.getExtendsClause()),
            testBuilders ? tree.getImplementsClause().stream() : Stream.empty());

    return parents
        .map(parent -> annotationsAmong(getSymbol(parent), autos.get(state), state))
        .filter(annotations -> !annotations.isEmpty())
        .findFirst()
        .filter(unused -> !isInGeneratedCode(state))
        .map(
            annotations -> {
              String name = annotations.iterator().next().toString();
              name = name.substring(name.lastIndexOf('.') + 1); // Strip package
              name = name.replace('$', '.'); // AutoValue$Builder -> AutoValue.Builder
              return buildDescription(tree)
                  .setMessage(
                      String.format("Do not extend an @%s class in non-generated code.", name))
                  .build();
            })
        .orElse(Description.NO_MATCH);
  }

  private static boolean isInGeneratedCode(VisitorState state) {
    // Skip generated code. Yes, I know we can do this via a flag but we should always ignore
    // generated code, so to be sure, manually check it.
    return !ASTHelpers.getGeneratedBy(state).isEmpty();
  }
}
