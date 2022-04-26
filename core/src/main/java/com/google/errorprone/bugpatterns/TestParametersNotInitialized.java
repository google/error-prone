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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;

/** Flags uses of parameters in non-parameterized tests. */
@BugPattern(
    summary =
        "This test has @TestParameter fields but is using the default JUnit4 runner. The"
            + " parameters will not be initialised beyond their default value.",
    severity = ERROR)
public final class TestParametersNotInitialized extends BugChecker implements ClassTreeMatcher {

  // TestParameterInjector also exposes a @TestParameters annotation, but it's not possible to
  // accidentally forget the runner when using this: it's used to initialise method or constructor
  // parameters, and the default JUnit4 runner would throw if there are parameters in either.
  private static final String TEST_PARAMETER =
      "com.google.testing.junit.testparameterinjector.TestParameter";

  private static final String RUNNER =
      "com.google.testing.junit.testparameterinjector.TestParameterInjector";

  private static final MultiMatcher<Tree, AnnotationTree> TEST_PARAMETER_INJECTOR =
      annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue("value", isJUnit4TestRunnerOfType(ImmutableSet.of(RUNNER))));

  private static final MultiMatcher<ClassTree, AnnotationTree> JUNIT4_RUNNER =
      annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue(
              "value", isJUnit4TestRunnerOfType(ImmutableSet.of("org.junit.runners.JUnit4"))));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!JUNIT4_RUNNER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (TEST_PARAMETER_INJECTOR.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getMembers().stream().noneMatch(m -> hasAnnotation(m, TEST_PARAMETER, state))) {
      return NO_MATCH;
    }
    AnnotationTree annotation =
        getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "RunWith");
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        annotation,
        String.format("@RunWith(%s.class)", SuggestedFixes.qualifyType(state, fix, RUNNER)));
    return describeMatch(tree, fix.build());
  }
}
