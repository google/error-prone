/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.variableType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Modifier;

/**
 * Bug pattern to recognize attempts to mock final types.
 *
 * @author Louis Wasserman
 */
@BugPattern(
  name = "CannotMockFinalClass",
  summary = "Mockito cannot mock final classes",
  explanation =
      "Mockito cannot mock final classes. See "
          + "https://github.com/mockito/mockito/wiki/FAQ for details.",
  category = Category.MOCKITO,
  severity = SeverityLevel.WARNING
)
public class CannotMockFinalClass extends BugChecker
    implements MethodInvocationTreeMatcher, VariableTreeMatcher {
  // TODO(lowasser): consider stopping mocks of primitive types here or in its own checker

  // Runners like GwtMockito allow mocking final types, so we conservatively stick to JUnit4.
  private static final Matcher<AnnotationTree> runWithJunit4 =
      allOf(
          isType("org.junit.runner.RunWith"),
          hasArgumentWithValue("value", classLiteral(isSameType("org.junit.runners.JUnit4"))));

  private static final Matcher<Tree> enclosingClassIsJunit4Test =
      enclosingClass(Matchers.<ClassTree>annotations(AT_LEAST_ONE, runWithJunit4));

  private static final Matcher<VariableTree> variableOfFinalClassAnnotatedMock =
      allOf(
          variableType(hasModifier(Modifier.FINAL)),
          hasAnnotation("org.mockito.Mock"),
          enclosingClassIsJunit4Test);

  private static final Matcher<MethodInvocationTree> creationOfMockForFinalClass =
      allOf(
          staticMethod().onClass("org.mockito.Mockito").named("mock"),
          argument(0, classLiteral(hasModifier(Modifier.FINAL))),
          enclosingClassIsJunit4Test);

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return variableOfFinalClassAnnotatedMock.matches(tree, state)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return creationOfMockForFinalClass.matches(tree, state)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }
}
