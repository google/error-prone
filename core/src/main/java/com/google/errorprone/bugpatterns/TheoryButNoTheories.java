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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestRunner;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Name;
import java.util.stream.Stream;

/** Flags uses of {@code @Theory} (and others) in non-{@code Theories}-run tests. */
@BugPattern(
    name = "TheoryButNoTheories",
    summary =
        "This test has members annotated with @Theory, @DataPoint, or @DataPoints but is using the"
            + " default JUnit4 runner.",
    severity = ERROR)
public final class TheoryButNoTheories extends BugChecker implements ClassTreeMatcher {
  private static final String THEORIES = "org.junit.experimental.theories.Theories";

  private static final Supplier<ImmutableSet<Name>> TYPES =
      VisitorState.memoize(
          s ->
              Stream.of(
                      "org.junit.experimental.theories.Theory",
                      "org.junit.experimental.theories.DataPoint",
                      "org.junit.experimental.theories.DataPoints")
                  .map(s::getName)
                  .collect(toImmutableSet()));

  private static final MultiMatcher<Tree, AnnotationTree> USING_THEORIES_RUNNER =
      annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue("value", isJUnit4TestRunnerOfType(ImmutableSet.of(THEORIES))));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!hasJUnit4TestRunner.matches(tree, state)) {
      return NO_MATCH;
    }
    if (USING_THEORIES_RUNNER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getMembers().stream()
        .allMatch(m -> annotationsAmong(getSymbol(m), TYPES.get(state), state).isEmpty())) {
      return NO_MATCH;
    }
    AnnotationTree annotation =
        getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "RunWith");
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        annotation,
        String.format("@RunWith(%s.class)", SuggestedFixes.qualifyType(state, fix, THEORIES)));
    return describeMatch(tree, fix.build());
  }
}
