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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.List;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.lang.model.element.Modifier;

/**
 * TODO(eaftan): Similar checkers for setUp() and tearDown().
 * TODO(eaftan): Inverse check -- JUnit 3 test that has @Test annotation but whose name doesn't
 * match JUnit 3 criteria.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "JUnit4TestNotRun",
    summary = "Test method will not be run; please add @Test annotation",
    explanation = "JUnit 3 required that test methods be named in a special way to be run as " +
        "part of a test case. JUnit 4 requires that test methods be annotated with @Test. " +
        "The test method that triggered this error is named like a JUnit 3 test, but is in a " +
        "JUnit 4 test class.  Thus, it will not be run unless you annotate it with @Test.\n\n" +
        "If you intend for this test method not to run, please add both an @Test and an " +
    	"@Ignore annotation to make it clear that you are purposely disabling it.",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class JUnit4TestNotRun extends DescribingMatcher<MethodTree> {

  private static final Collection<String> DEFAULT_TEST_RUNNERS = Arrays.asList(
      "org.junit.runners.JUnit4", "org.mockito.runners.MockitoJUnitRunner");

  private static final String JUNIT3_TEST_CASE_CLASS = "junit.framework.TestCase";
  private static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  private static final String JUNIT_BEFORE_ANNOTATION = "org.junit.Before";
  private static final String JUNIT_AFTER_ANNOTATION = "org.junit.After";
  private static final String JUNIT_BEFORE_CLASS_ANNOTATION = "org.junit.BeforeClass";
  private static final String JUNIT_AFTER_CLASS_ANNOTATION = "org.junit.AfterClass";

  /**
   * A list of valid test runners that this matcher should look for in the @RunWith annotation.
   */
  private Collection<String> testRunners;

  public JUnit4TestNotRun() {
    this.testRunners = new ArrayList<String>(DEFAULT_TEST_RUNNERS);
  }

  /**
   * Construct a matcher with additional acceptable test runners.
   *
   * @param additionalTestRunners Additional test runner classes to check for in the @RunWith annotation,
   * e.g., "org.junit.runners.BlockJUnit4ClassRunner"
   */
  public JUnit4TestNotRun(String... additionalTestRunners) {
    this();
    this.testRunners.addAll(Arrays.asList(additionalTestRunners));
  }

  /**
   * Matches an argument of type Class<T>, where T is a type listed in the testRunners field.
   *
   * TODO(eaftan): Support checking for an annotation that tells us whether this test runner
   * expects tests to be annotated with @Test.
   */
  private final Matcher<ExpressionTree> isJUnit4TestRunner = new Matcher<ExpressionTree>() {
    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      Type type = ((JCTree) t).type;
      // Expect a class type.
      if (!(type instanceof ClassType)) {
        return false;
      }
      // Expect one type argument, the type of the JUnit class runner to use.
      List<Type> typeArgs = ((ClassType) type).getTypeArguments();
      if (typeArgs.size() != 1) {
        return false;
      }
      return testRunners.contains(typeArgs.get(0).toString());
    }
  };

  @SuppressWarnings("unchecked")
  private final Matcher<ClassTree> isJUnit4TestClass = allOf(
      not(isSubtypeOf(JUNIT3_TEST_CASE_CLASS)),
      annotations(ANY, hasArgumentWithValue("value", isJUnit4TestRunner)));

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodTree> hasJUnitAnnotation = anyOf(
      hasAnnotation(JUNIT4_TEST_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_CLASS_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_CLASS_ANNOTATION));

  /**
   * Matches if:
   * 1) The method's name begins with "test".
   * 2) The method has no parameters.
   * 3) The method is public.
   * 4) The method is not annotated with @Test, @Before, @After, @BeforeClass, or @AfterClass.
   * 5) The enclosing class has an @RunWith annotation and does not extend TestCase. This marks
   *    that the test is intended to run with JUnit 4.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodTree methodTree, VisitorState state) {
    return allOf(methodNameStartsWith("test"),
        methodHasParameters(),
        methodHasModifier(Modifier.PUBLIC),
        not(hasJUnitAnnotation),
        enclosingClass(isJUnit4TestClass))
        .matches(methodTree, state);
  }

  /**
   * Add the @Test annotation.  If the method is static, also make the method non-static.
   *
   * TODO(eaftan): The static case here relies on having tree end positions available.  Come up
   * with a better way of doing this that doesn't require tree end positions.  Maybe we should
   * just not provide suggested fixes for these few cases when the javac infrastructure gets in the
   * way.
   */
  @Override
  public Description describe(MethodTree methodTree, VisitorState state) {
    if (methodHasModifier(Modifier.STATIC).matches(methodTree, state)) {
      CharSequence methodSource = state.getSourceForNode((JCMethodDecl) methodTree);
      if (methodSource != null) {
        String methodString = "@Test\n" + methodSource.toString().replaceFirst(" static ", " ");
        SuggestedFix fix = new SuggestedFix()
            .addImport(JUNIT4_TEST_ANNOTATION)
            .replace(methodTree, methodString);
        return new Description(methodTree, getDiagnosticMessage(), fix);
      }
    }
    SuggestedFix fix = new SuggestedFix()
        .addImport(JUNIT4_TEST_ANNOTATION)
        .prefixWith(methodTree, "@Test\n");
    return new Description(methodTree, getDiagnosticMessage(), fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private JUnit4TestNotRun matcher;

    public Scanner() {
      matcher = new JUnit4TestNotRun();
    }

    /**
     * Construct a scanner with additional acceptable test runners.
     *
     * @param testRunners Additional test runner classes to check for in the @RunWith annotation,
     * e.g., "org.junit.runners.BlockJUnit4ClassRunner"
     */
    public Scanner(String... testRunners) {
      matcher = new JUnit4TestNotRun(testRunners);
    }

    @Override
    public Void visitMethod(MethodTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethod(node, visitorState);
    }
  }
}
