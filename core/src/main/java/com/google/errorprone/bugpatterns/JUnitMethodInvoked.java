/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Directly invoking a JUnit test method is discouraged; only the JUnit test runner should"
            + " call these methods. If you need to share logic between tests, extract a helper"
            + " method or class.",
    severity = WARNING)
public final class JUnitMethodInvoked extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree methodTree, VisitorState state) {
    MethodSymbol method = getSymbol(methodTree);
    return method != null && isJUnitMethod(method, state) ? describeMatch(methodTree) : NO_MATCH;
  }

  private static boolean isJUnitMethod(MethodSymbol method, VisitorState state) {
    return isJUnit3TestMethod(method, state) || hasJUnitAnnotation(method, state);
  }

  /**
   * Returns whether the given method is a JUnit 3 test method. JUnit 3 test methods must have the
   * following properties:
   *
   * <ul>
   *   <li>public
   *   <li>have no parameters
   *   <li>have a void return type
   *   <li>be declared in a class that extends {@code junit.framework.TestCase}
   *   <li>have a name starting with "test"
   * </ul>
   */
  private static boolean isJUnit3TestMethod(MethodSymbol method, VisitorState state) {
    if (!isSubtype(method.enclClass().type, JUNIT_FRAMEWORK_TESTCASE.get(state), state)) {
      return false;
    }
    if (!method.getModifiers().contains(Modifier.PUBLIC)) {
      return false;
    }
    // TODO(kak): consider flagging parameterized test methods too!
    if (!method.getParameters().isEmpty()) {
      return false;
    }
    if (method.getReturnType().getKind() != TypeKind.VOID) {
      return false;
    }
    String methodName = method.getSimpleName().toString();
    // TODO(kak): consider flagging setUp() and tearDown() too!
    return methodName.startsWith("test");
  }

  // TODO(kak): consider flagging additional annotations:
  // JUnit 4: @After, @AfterClass, @Before, @BeforeClass
  // JUnit 5: @AfterAll, @AfterEach, @BeforeAll, @BeforeEach,
  //         @RepeatedTest, @ParameterizedTest, @TestFactory, @TestTemplate
  private static final ImmutableSet<String> JUNIT_ANNOTATIONS =
      ImmutableSet.of(
          // JUnit 4
          "org.junit.Ignore",
          "org.junit.Test",
          // JUnit 5
          "org.junit.jupiter.api.Disabled",
          "org.junit.jupiter.api.Test");

  /** Returns whether the given method has a JUnit annotation. */
  private static boolean hasJUnitAnnotation(MethodSymbol method, VisitorState state) {
    return JUNIT_ANNOTATIONS.stream()
        .anyMatch(annotation -> hasAnnotation(method, annotation, state));
  }

  private static final Supplier<Type> JUNIT_FRAMEWORK_TESTCASE =
      VisitorState.memoize(state -> state.getTypeFromString("junit.framework.TestCase"));
}
