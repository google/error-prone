/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for Parameter
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@RunWith(JUnit4.class)
public class ParameterTest {

  /**
   * A {@link BugChecker} that prints whether the type of first argument is assignable to the type
   * of the second one.
   */
  @BugPattern(
    name = "IsFirstAssignableToSecond",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Print whether the type of the first argument is assignable to the second one"
  )
  public static class IsFirstAssignableToSecond extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<Parameter> arguments = Parameter.createListFromExpressionTrees(tree.getArguments());
      if (arguments.size() != 2) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(String.valueOf(arguments.get(0).isAssignableTo(arguments.get(1), state)))
          .build();
    }
  }

  @Test
  public void isAssignableTo_returnsTrue_assigningIntegerToInt() {
    CompilationTestHelper.newInstance(IsFirstAssignableToSecond.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void target(Integer integer, int i) {",
            "    // BUG: Diagnostic contains: true",
            "    target(integer, i);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isAssignableTo_returnsFalse_assigningObjectToInteger() {
    CompilationTestHelper.newInstance(IsFirstAssignableToSecond.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void target(Object obj, Integer integer) {",
            "    // BUG: Diagnostic contains: false",
            "    target(obj, integer);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isAssignableTo_returnsTrue_assigningIntegerToObject() {
    CompilationTestHelper.newInstance(IsFirstAssignableToSecond.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void target(Integer integer, Object obj) {",
            "    // BUG: Diagnostic contains: true",
            "    target(integer, obj);",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints the name extracted for the first argument */
  @BugPattern(
    name = "PrintNameOfFirstArgument",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Print the name of the first argument"
  )
  public static class PrintNameOfFirstArgument extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<? extends ExpressionTree> arguments = tree.getArguments();
      if (arguments.isEmpty()) {
        return Description.NO_MATCH;
      }
      ExpressionTree argument = Iterables.getFirst(arguments, null);
      return buildDescription(tree).setMessage(Parameter.getArgumentName(argument)).build();
    }
  }

  @Test
  public void getName_usesEnclosingClass_withThisPointer() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: Test",
            "    target(this);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesConstructedClass_withNewClass() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: String",
            "    target(new String());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_stripsPrefix_fromMethodWithGetPrefix() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object getValue();",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: Value",
            "    target(getValue());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesEnclosingClass_fromMethodOnlyContainingGetWithNoReceiver() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object get();",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: Test",
            "    target(get());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesReceiver_fromMethodOnlyContainingGet() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Other {",
            "  abstract Object get();",
            "}",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test(Other otherObject) {",
            "    // BUG: Diagnostic contains: otherObject",
            "    target(otherObject.get());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesOwner_fromAnonymousClass() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Other {",
            "  abstract Object get();",
            "}",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: Object",
            "    target(new Object() {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesOwner_fromThisInAnonymousClass() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Other {",
            "  abstract Object get();",
            "}",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    new Object() {",
            "      void test() {",
            "        // BUG: Diagnostic contains: Object",
            "        target(this);",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_usesOwner_fromGetMethodInAnonymousClass() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Other {",
            "  abstract Object get();",
            "}",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    new Object() {",
            "      Integer get() {",
            "        return 1;",
            "      }",
            "      void test() {",
            "        // BUG: Diagnostic contains: Object",
            "        target(get());",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_returnsNull_withNullLiteral() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: " + Parameter.NAME_NULL,
            "    target(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_returnsUnknown_withTerneryIf() {
    CompilationTestHelper.newInstance(PrintNameOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test(boolean flag) {",
            "    // BUG: Diagnostic contains: " + Parameter.NAME_NOT_PRESENT,
            "    target(flag ? 1 : 0);",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints whether the first argument is constant */
  @BugPattern(
    name = "PrintIsConstantFirstArgument",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Print whether the first argument is constant"
  )
  public static class PrintIsConstantFirstArgument extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<? extends ExpressionTree> arguments = tree.getArguments();
      if (arguments.isEmpty()) {
        return Description.NO_MATCH;
      }
      List<Parameter> parameters = Parameter.createListFromExpressionTrees(arguments);
      Parameter first = Iterables.getFirst(parameters, null);
      return buildDescription(tree).setMessage(String.valueOf(first.constant())).build();
    }
  }

  @Test
  public void getName_returnsConstant_withConstant() {
    CompilationTestHelper.newInstance(PrintIsConstantFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: true",
            "    target(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_returnsConstant_withConstantFromOtherClass() {
    CompilationTestHelper.newInstance(PrintIsConstantFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test() {",
            "    // BUG: Diagnostic contains: true",
            "    target(Double.MAX_VALUE);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getName_returnsNotConstant_withVariable() {
    CompilationTestHelper.newInstance(PrintIsConstantFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object o);",
            "  void test(Object o) {",
            "    // BUG: Diagnostic contains: false",
            "    target(o);",
            "  }",
            "}")
        .doTest();
  }
}
