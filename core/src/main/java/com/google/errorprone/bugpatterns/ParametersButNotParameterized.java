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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestRunner;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;

/** Flags uses of parameters in non-parameterized tests. */
@BugPattern(
    summary =
        "This test has @Parameters but is using the default JUnit4 runner. The parameters will"
            + " have no effect.",
    severity = ERROR)
public final class ParametersButNotParameterized extends BugChecker implements ClassTreeMatcher {
  private static final String PARAMETERIZED = "org.junit.runners.Parameterized";
  private static final String PARAMETER = "org.junit.runners.Parameterized.Parameter";
  private static final String PARAMETERS = "org.junit.runners.Parameterized.Parameters";

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!hasJUnit4TestRunner.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getMembers().stream()
        .noneMatch(
            m -> hasAnnotation(m, PARAMETER, state) || hasAnnotation(m, PARAMETERS, state))) {
      return NO_MATCH;
    }
    AnnotationTree annotation =
        getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "RunWith");
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        annotation,
        String.format("@RunWith(%s.class)", SuggestedFixes.qualifyType(state, fix, PARAMETERIZED)));
    return describeMatch(tree, fix.build());
  }
}
