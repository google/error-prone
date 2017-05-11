/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JMOCK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isField;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

@BugPattern(
  name = "JMockTestWithoutRunWithOrRuleAnnotation",
  summary =
      "jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must "
          + "have a @Rule JUnit annotation",
  explanation =
      "jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field "
          + "must have a @Rule JUnit annotation. If this is not done, then all of your jMock tests "
          + "will run and pass, but none of your assertions will actually be evaluated. Your tests "
          + "will pass even if they shouldn't.",
  category = JMOCK,
  severity = ERROR
)
public class JMockTestWithoutRunWithOrRuleAnnotation extends BugChecker
    implements VariableTreeMatcher {

  private static final String JMOCK_TEST_RUNNER_CLASS = "org.jmock.integration.junit4.JMock";

  private static final Matcher<VariableTree> fieldIsMockery =
      allOf(isSubtypeOf("org.jmock.Mockery"), isField());

  private static final Matcher<VariableTree> fieldHasRuleAnnotation =
      hasAnnotation("org.junit.Rule");

  private static final Matcher<Tree> enclosingClassRunsWithJMockTestRunner =
      enclosingClass(
          allOf(
              hasAnnotation(JUnitMatchers.JUNIT4_RUN_WITH_ANNOTATION),
              Matchers.<ClassTree>annotations(
                  AT_LEAST_ONE,
                  hasArgumentWithValue(
                      "value", classLiteral(isSameType(JMOCK_TEST_RUNNER_CLASS))))));

  private static final Matcher<VariableTree> BUG_PATTERN_MATCHER =
      allOf(
          fieldIsMockery,
          not(anyOf(fieldHasRuleAnnotation, enclosingClassRunsWithJMockTestRunner)));

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (BUG_PATTERN_MATCHER.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
