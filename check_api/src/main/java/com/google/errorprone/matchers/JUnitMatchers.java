/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasAnnotationOnAnyOverriddenMethod;
import static com.google.errorprone.matchers.Matchers.hasAnnotationWithSimpleName;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasNoParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.nestingKind;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TryTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * Matchers for code patterns which appear to be JUnit-based tests.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandillian)
 */
public final class JUnitMatchers {
  public static final String JUNIT3_TEST_CASE_CLASS = "junit.framework.TestCase";
  public static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  public static final String JUNIT5_TEST_ANNOTATION = "org.junit.jupiter.api.Test";
  public static final String JUNIT4_THEORY_ANNOTATION = "org.junit.experimental.theories.Theory";
  public static final String JUNIT_BEFORE_ANNOTATION = "org.junit.Before";
  public static final String JUNIT_AFTER_ANNOTATION = "org.junit.After";
  public static final String JUNIT_BEFORE_CLASS_ANNOTATION = "org.junit.BeforeClass";
  public static final String JUNIT_AFTER_CLASS_ANNOTATION = "org.junit.AfterClass";
  public static final String JUNIT5_BEFORE_EACH_ANNOTATION = "org.junit.jupiter.api.BeforeEach";
  public static final String JUNIT5_AFTER_EACH_ANNOTATION = "org.junit.jupiter.api.AfterEach";
  public static final String JUNIT5_BEFORE_ALL_ANNOTATION = "org.junit.jupiter.api.BeforeAll";
  public static final String JUNIT5_AFTER_ALL_ANNOTATION = "org.junit.jupiter.api.AfterAll";
  public static final String JUNIT4_IGNORE_ANNOTATION = "org.junit.Ignore";
  public static final String JUNIT5_DISABLED_ANNOTATION = "org.junit.jupiter.api.Disabled";
  public static final String JUNIT3_ASSERT_CLASS = "junit.framework.Assert";
  public static final String JUNIT4_ASSERT_CLASS = "org.junit.Assert";
  public static final String JUNIT5_ASSERT_CLASS = "org.junit.jupiter.api.Assertions";
  public static final String JUNIT4_RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";
  public static final String JUNIT4_RUNNER_CLASS = "org.junit.runners.JUnit4";

  /**
   * Checks if a method, or any overridden method, is annotated with any annotation from the
   * org.junit package.
   */
  public static boolean hasJUnitAnnotation(MethodTree tree, VisitorState state) {
    MethodSymbol methodSym = getSymbol(tree);
    if (hasJUnitAttr(methodSym)) {
      return true;
    }
    return streamSuperMethods(methodSym, state.getTypes()).anyMatch(JUnitMatchers::hasJUnitAttr);
  }

  /** Checks if a method symbol has any attribute from the org.junit package. */
  private static boolean hasJUnitAttr(MethodSymbol methodSym) {
    return methodSym.getRawAttributes().stream()
        .anyMatch(attr -> attr.type.tsym.getQualifiedName().toString().startsWith("org.junit."));
  }

  public static final Matcher<MethodTree> hasJUnit4BeforeAnnotations =
      anyOf(
          hasAnnotationOnAnyOverriddenMethod(JUNIT_BEFORE_ANNOTATION),
          hasAnnotation(JUNIT_BEFORE_CLASS_ANNOTATION));

  public static final Matcher<MethodTree> hasJUnit4AfterAnnotations =
      anyOf(
          hasAnnotationOnAnyOverriddenMethod(JUNIT_AFTER_ANNOTATION),
          hasAnnotation(JUNIT_AFTER_CLASS_ANNOTATION));

  /** Matches a class that inherits from TestCase. */
  public static final Matcher<ClassTree> isTestCaseDescendant = isSubtypeOf(JUNIT3_TEST_CASE_CLASS);

  /**
   * Match a class which appears to be missing a @RunWith annotation.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The class does not have a JUnit 4 @RunWith annotation.
   *   <li>The class is concrete.
   *   <li>The class is a top-level class.
   * </ol>
   */
  public static final Matcher<ClassTree> isConcreteClassWithoutRunWith =
      allOf(
          not(hasAnnotation(JUNIT4_RUN_WITH_ANNOTATION)),
          not(Matchers.<ClassTree>hasModifier(Modifier.ABSTRACT)),
          nestingKind(TOP_LEVEL));

  /** Match a class which has one or more methods with a JUnit 4 @Test annotation. */
  public static final Matcher<ClassTree> hasJUnit4TestCases =
      hasMethod(hasAnnotationOnAnyOverriddenMethod(JUNIT4_TEST_ANNOTATION));

  /** Match a class which has one or more methods with a JUnit 5 @Test annotation. */
  public static final Matcher<ClassTree> hasJUnit5TestCases =
      hasMethod(hasAnnotation(JUNIT5_TEST_ANNOTATION));

  /** Match a method annotated with JUnit 5 @BeforeEach. */
  public static final Matcher<MethodTree> hasJUnit5BeforeEach =
      hasAnnotation(JUNIT5_BEFORE_EACH_ANNOTATION);

  /** Match a method annotated with JUnit 5 @AfterEach. */
  public static final Matcher<MethodTree> hasJUnit5AfterEach =
      hasAnnotation(JUNIT5_AFTER_EACH_ANNOTATION);

  /** Match a method annotated with JUnit 5 @BeforeAll. */
  public static final Matcher<MethodTree> hasJUnit5BeforeAll =
      hasAnnotation(JUNIT5_BEFORE_ALL_ANNOTATION);

  /** Match a method annotated with JUnit 5 @AfterAll. */
  public static final Matcher<MethodTree> hasJUnit5AfterAll =
      hasAnnotation(JUNIT5_AFTER_ALL_ANNOTATION);

  /** Match a method annotated with any JUnit 5 before annotation (@BeforeEach or @BeforeAll). */
  public static final Matcher<MethodTree> hasJUnit5BeforeAnnotations =
      anyOf(hasJUnit5BeforeEach, hasJUnit5BeforeAll);

  /** Match a method annotated with any JUnit 5 after annotation (@AfterEach or @AfterAll). */
  public static final Matcher<MethodTree> hasJUnit5AfterAnnotations =
      anyOf(hasJUnit5AfterEach, hasJUnit5AfterAll);

  /**
   * Returns {@code true} if the enclosing class of the given state is a JUnit 5 test class.
   */
  public static boolean isJUnit5TestClass(VisitorState state) {
    for (com.sun.source.tree.Tree ancestor : state.getPath()) {
      if (ancestor instanceof ClassTree classTree
          && hasJUnit5TestCases.matches(classTree, state)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Match a class which appears to be a JUnit 3 test class.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The class does inherit from TestCase.
   *   <li>The class does not have a JUnit 4 {@code @RunWith} annotation nor any methods annotated
   *       {@code @Test}.
   *   <li>The class is concrete.
   *   <li>This class is a top-level class.
   * </ol>
   */
  public static final Matcher<ClassTree> isJUnit3TestClass =
      allOf(isTestCaseDescendant, isConcreteClassWithoutRunWith, not(hasJUnit4TestCases));

  /**
   * Match a method which appears to be a JUnit 3 test case.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The method's name begins with "test".
   *   <li>The method has no parameters.
   *   <li>The method is public.
   *   <li>The method returns void.
   * </ol>
   */
  public static final Matcher<MethodTree> isJunit3TestCase =
      allOf(
          methodNameStartsWith("test"),
          methodHasNoParameters(),
          Matchers.<MethodTree>hasModifier(Modifier.PUBLIC),
          methodReturns(VOID_TYPE));

  /** Common matcher for possible JUnit setUp/tearDown methods. */
  private static final Matcher<MethodTree> looksLikeJUnitSetUpOrTearDown =
      allOf(
          methodHasNoParameters(),
          anyOf(
              methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
              methodHasVisibility(MethodVisibility.Visibility.PROTECTED)),
          not(Matchers.<MethodTree>hasModifier(Modifier.ABSTRACT)),
          not(Matchers.<MethodTree>hasModifier(Modifier.STATIC)),
          methodReturns(VOID_TYPE));

  /**
   * Match a method which appears to be a JUnit 3 setUp method
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The method is named "setUp".
   *   <li>The method has no parameters.
   *   <li>The method is a public or protected instance method that is not abstract.
   *   <li>The method returns void.
   * </ol>
   */
  public static final Matcher<MethodTree> looksLikeJUnit3SetUp =
      allOf(methodIsNamed("setUp"), looksLikeJUnitSetUpOrTearDown);

  /**
   * Matches a method which appears to be a JUnit4 @Before method.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The method is annotated {@code Before}.
   *   <li>The method has no parameters.
   *   <li>The method is a public or protected instance method that is not abstract.
   *   <li>The method returns void.
   * </ol>
   */
  public static final Matcher<MethodTree> looksLikeJUnit4Before =
      allOf(hasAnnotationWithSimpleName("Before"), looksLikeJUnitSetUpOrTearDown);

  /**
   * Match a method which appears to be a JUnit 3 tearDown method
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The method is named "tearDown".
   *   <li>The method has no parameters.
   *   <li>The method is a public or protected instance method that is not abstract.
   *   <li>The method returns void.
   * </ol>
   */
  public static final Matcher<MethodTree> looksLikeJUnit3TearDown =
      allOf(methodIsNamed("tearDown"), looksLikeJUnitSetUpOrTearDown);

  /**
   * Matches a method which appears to be a JUnit4 @After method.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The method is annotated {@code After}.
   *   <li>The method has no parameters.
   *   <li>The method is a public or protected instance method that is not abstract.
   *   <li>The method returns void.
   * </ol>
   */
  public static final Matcher<MethodTree> looksLikeJUnit4After =
      allOf(hasAnnotationWithSimpleName("After"), looksLikeJUnitSetUpOrTearDown);

  /** Matches a method annotated with @Test but not @Ignore. */
  public static final Matcher<MethodTree> wouldRunInJUnit4 =
      allOf(
          hasAnnotationOnAnyOverriddenMethod(JUNIT4_TEST_ANNOTATION),
          not(hasAnnotationOnAnyOverriddenMethod(JUNIT4_IGNORE_ANNOTATION)));

  /** Matches a JUnit 3, 4, or 5 test case. */
  public static final Matcher<MethodTree> TEST_CASE =
      anyOf(
          isJunit3TestCase,
          hasAnnotation(JUNIT4_TEST_ANNOTATION),
          hasAnnotation(JUNIT4_THEORY_ANNOTATION),
          hasAnnotation(JUNIT5_TEST_ANNOTATION));

  /**
   * A list of test runners that this matcher should look for in the @RunWith annotation. Subclasses
   * of the test runners are also matched.
   */
  private static final ImmutableList<String> TEST_RUNNERS =
      ImmutableList.of(
          "org.mockito.junit.MockitoJUnitRunner", "org.junit.runners.BlockJUnit4ClassRunner");

  /**
   * Matches an argument of type {@code Class<T>}, where T is a subtype of one of the test runners
   * listed in the TEST_RUNNERS field.
   *
   * <p>TODO(eaftan): Support checking for an annotation that tells us whether this test runner
   * expects tests to be annotated with @Test.
   */
  public static Matcher<ExpressionTree> isJUnit4TestRunnerOfType(Iterable<String> runnerTypes) {
    return (ExpressionTree t, VisitorState state) -> {
      Type type = ASTHelpers.getType(t);
      // Expect a class type.
      if (!(type instanceof ClassType)) {
        return false;
      }
      // Expect one type argument, the type of the JUnit class runner to use.
      com.sun.tools.javac.util.List<Type> typeArgs = type.getTypeArguments();
      if (typeArgs.size() != 1) {
        return false;
      }
      Type runnerType = getOnlyElement(typeArgs);
      for (String testRunner : runnerTypes) {
        Symbol parent = state.getSymbolFromString(testRunner);
        if (parent == null) {
          continue;
        }
        if (runnerType.tsym.isSubClass(parent, state.getTypes())) {
          return true;
        }
      }
      return false;
    };
  }

  public static final MultiMatcher<ClassTree, AnnotationTree> hasJUnit4TestRunner =
      annotations(
          AT_LEAST_ONE, hasArgumentWithValue("value", isJUnit4TestRunnerOfType(TEST_RUNNERS)));

  /**
   * Matches classes which have attributes of only JUnit4 test classes.
   *
   * <p>Matches if:
   *
   * <ol>
   *   <li>The class is non-abstract.
   *   <li>The class does not inherit from JUnit3 {@code TestCase}.
   *   <li>The class is annotated with {@code @RunWith} or any method therein is annotated with
   *       {@code @Test}.
   * </ol>
   */
  public static final Matcher<ClassTree> isJUnit4TestClass =
      allOf(
          not(isTestCaseDescendant),
          not(enclosingClass(hasModifier(Modifier.ABSTRACT))),
          anyOf(hasJUnit4TestRunner, hasJUnit4TestCases));

  /**
   * Matches classes which have attributes of both JUnit 3 and 4 classes.
   *
   * <p>Matches if the class:
   *
   * <ol>
   *   <li>Inherits from JUnit 3 {@code TestCase}.
   *   <li>
   *       <ol>
   *         <li>Has a JUnit4 test runner annotation, or
   *         <li>Has any methods annotated {@code @Test}.
   *       </ol>
   * </ol>
   *
   * <p>As currently implemented, classes with ambiguous version will match neither {@code
   * isJUnit4TestClass} nor {@code isJUnit3TestClass}.
   */
  public static final Matcher<ClassTree> isAmbiguousJUnitVersion =
      allOf(isTestCaseDescendant, anyOf(hasJUnit4TestRunner, hasJUnit4TestCases));

  /**
   * Returns {@code true} if the given method invocation is a call to a JUnit 5 assertion method,
   * determined by the symbol owner being {@code org.junit.jupiter.api.Assertions}.
   *
   * <p>This is more robust than import scanning: it works with fully-qualified calls, imported
   * calls, and star imports, and answers per-call rather than per-file.
   */
  public static boolean isJUnit5AssertionCall(ExpressionTree tree) {
    Symbol sym = getSymbol(tree);
    return sym != null
        && sym.owner.getQualifiedName().toString().equals(JUNIT5_ASSERT_CLASS);
  }

  /**
   * Returns the assertion class name appropriate for the enclosing test class: {@link
   * #JUNIT5_ASSERT_CLASS} for JUnit 5 tests, {@link #JUNIT4_ASSERT_CLASS} for JUnit 4 and
   * earlier.
   */
  public static String getAssertionClassName(VisitorState state) {
    return isJUnit5TestClass(state) ? JUNIT5_ASSERT_CLASS : JUNIT4_ASSERT_CLASS;
  }

  /**
   * Returns the assertion class name appropriate for the given assertion call: {@link
   * #JUNIT5_ASSERT_CLASS} if the call is to a JUnit 5 assertion, {@link #JUNIT4_ASSERT_CLASS}
   * otherwise.
   */
  public static String getAssertionClassName(ExpressionTree tree) {
    return isJUnit5AssertionCall(tree) ? JUNIT5_ASSERT_CLASS : JUNIT4_ASSERT_CLASS;
  }

  /**
   * Scans the try block of a {@link TryTree} for a {@code fail()} call statement.
   *
   * @return the {@code fail()} invocation, or empty if not found
   */
  public static Optional<MethodInvocationTree> findFailCallInTry(TryTree tryTree) {
    for (StatementTree statement : tryTree.getBlock().getStatements()) {
      if (statement instanceof ExpressionStatementTree est
          && est.getExpression() instanceof MethodInvocationTree mit) {
        Symbol sym = getSymbol(mit);
        if (sym != null
            && sym.getSimpleName().contentEquals("fail")
            && sym.isStatic()) {
          return Optional.of(mit);
        }
      }
    }
    return Optional.empty();
  }

  private JUnitMatchers() {}
}
