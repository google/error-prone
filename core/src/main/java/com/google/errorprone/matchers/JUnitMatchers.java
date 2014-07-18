/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.*;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Collection;

import javax.lang.model.element.Modifier;

/**
 * Matchers for code patterns which appear to be JUnit-based tests.
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandillian)
 */
public class JUnitMatchers {
  private static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  private static final String JUNIT_BEFORE_ANNOTATION = "org.junit.Before";
  private static final String JUNIT_AFTER_ANNOTATION = "org.junit.After";
  private static final String JUNIT_BEFORE_CLASS_ANNOTATION = "org.junit.BeforeClass";
  private static final String JUNIT_AFTER_CLASS_ANNOTATION = "org.junit.AfterClass";
  private static final String JUNIT3_TEST_CASE_CLASS = "junit.framework.TestCase";
  private static final String JUNIT4_RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";
  private static final String JUNIT4_IGNORE_ANNOTATION = "org.junit.Ignore";


  @SuppressWarnings("unchecked")
  public static final Matcher<MethodTree> hasJUnitAnnotation = anyOf(
      hasAnnotation(JUNIT4_TEST_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_CLASS_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_CLASS_ANNOTATION));

  /**
   * Match a class which appears to be a JUnit 3 test class.
   *
   * Matches if:
   * 1) The class doesn't extend from TestCase
   * 2) There are no JUnit4 @RunWith annotations
   * 3) The class is concrete
   */
  @SuppressWarnings("unchecked")
  public static final Matcher<ClassTree> isJUnit3TestClass = allOf(
      isSubtypeOf(JUNIT3_TEST_CASE_CLASS),
      not(hasAnnotation(JUNIT4_RUN_WITH_ANNOTATION)),
      not(Matchers.<ClassTree>hasModifier(Modifier.ABSTRACT)));

  /**
   * Match a method which appears to be a JUnit 3 test case.
   *
   * Matches if:
   * 1) The method's name begins with "test".
   * 2) The method has no parameters.
   * 3) The method is public.
   */
  @SuppressWarnings("unchecked")
  public static final Matcher<MethodTree> isJunit3TestCase = allOf(
      methodNameStartsWith("test"),
      methodHasParameters(),
      Matchers.<MethodTree>hasModifier(Modifier.PUBLIC)
  );

  /**
   * Matches a method annotated with @Test but not @Ignore.
   */
  @SuppressWarnings("unchecked")
  public static final Matcher<MethodTree> wouldRunInJUnit4 = allOf(
      hasAnnotation(JUNIT4_TEST_ANNOTATION),
      not(hasAnnotation(JUNIT4_IGNORE_ANNOTATION)));

  public static class JUnit4TestClassMatcher implements Matcher<ClassTree> {

    /**
     * A list of test runners that this matcher should look for in the @RunWith annotation.
     * Subclasses of the test runners are also matched.
     */
    private static final Collection<String> TEST_RUNNERS = Arrays.asList(
        "org.mockito.runners.MockitoJUnitRunner",
        "org.junit.runners.BlockJUnit4ClassRunner");

    /**
     * Matches an argument of type Class<T>, where T is a subtype of one of the test runners listed
     * in the TEST_RUNNERS field.
     *
     * TODO(eaftan): Support checking for an annotation that tells us whether this test runner
     * expects tests to be annotated with @Test.
     */
    private static final Matcher<ExpressionTree> isJUnit4TestRunner =
        new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree t, VisitorState state) {
        Type type = ((JCTree) t).type;
        // Expect a class type.
        if (!(type instanceof ClassType)) {
          return false;
        }
        // Expect one type argument, the type of the JUnit class runner to use.
        com.sun.tools.javac.util.List<Type> typeArgs = ((ClassType) type).getTypeArguments();
        if (typeArgs.size() != 1) {
          return false;
        }
        Type runnerType = typeArgs.get(0);
        for (String testRunner : TEST_RUNNERS) {
          Symbol parent = state.getSymbolFromString(testRunner);
          if (parent == null) {
            continue;
          }
          if (runnerType.tsym.isSubClass(parent, state.getTypes())) {
            return true;
          }
        }
        return false;
      }
    };

    @SuppressWarnings("unchecked")
    private static final Matcher<ClassTree> isJUnit4TestClass = allOf(
        not(isSubtypeOf(JUNIT3_TEST_CASE_CLASS)),
        annotations(ANY, hasArgumentWithValue("value", isJUnit4TestRunner)));

    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      return isJUnit4TestClass.matches(classTree, state);
    }
  }
}
