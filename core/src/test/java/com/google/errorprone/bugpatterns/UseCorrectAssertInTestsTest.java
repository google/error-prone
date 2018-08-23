/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author galitch@google.com (Anton Galitch) */
@RunWith(JUnit4.class)
public final class UseCorrectAssertInTestsTest {

  private static final String ASSERT_THAT_IMPORT =
      "static com.google.common.truth.Truth.assertThat;";
  private static final String ASSERT_WITH_MESSAGE_IMPORT =
      "static com.google.common.truth.Truth.assertWithMessage;";
  private static final String INPUT = "in/FooTest.java";
  private static final String OUTPUT = "out/FooTest.java";

  private static final String[] INPUT_SRC_NO_TEST =
      new String[] {
        "import org.junit.runner.RunWith;",
        "import org.junit.runners.JUnit4;",
        "@RunWith(JUnit4.class)",
        "public class FooTest {",
        "  void foo() {",
        "    assert true;",
        "  }",
        "}"
      };

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new UseCorrectAssertInTests(), getClass());

  @Test
  public void correctAssertInTest() {
    helper
        .addInputLines(
            INPUT, inputWithExpressionAndImport("assertThat(true).isTrue();", ASSERT_THAT_IMPORT))
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noAssertInTestsFound() {
    helper.addInputLines(INPUT, inputWithExpression("int a = 1;")).expectUnchanged().doTest();
  }

  @Test
  public void assertInNonTestMethod() {
    helper.addInputLines(INPUT, INPUT_SRC_NO_TEST).expectUnchanged().doTest();
  }

  @Test
  public void wrongAssertInTestWithParentheses() {
    helper
        .addInputLines(INPUT, inputWithExpression("assert (true);"))
        .addOutputLines(
            OUTPUT, inputWithExpressionAndImport("assertThat(true).isTrue();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithoutParentheses() {
    helper
        .addInputLines(INPUT, inputWithExpression("assert true;"))
        .addOutputLines(
            OUTPUT, inputWithExpressionAndImport("assertThat(true).isTrue();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailString() {
    helper
        .addInputLines(INPUT, inputWithExpression("assert (true) : \"description\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionAndImport(
                "assertWithMessage(\"description\").that(true).isTrue();",
                ASSERT_WITH_MESSAGE_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailStringVariable() {
    helper
        .addInputLines(
            INPUT, inputWithExpressions("String desc = \"description\";", "assert (true) : desc;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "String desc = \"description\";",
                "assertWithMessage(desc).that(true).isTrue();",
                ASSERT_WITH_MESSAGE_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailNonStringVariable() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer desc = 1;", "assert (true) : desc;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer desc = 1;",
                "assertWithMessage(desc.toString()).that(true).isTrue();",
                ASSERT_WITH_MESSAGE_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertFalseCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("boolean a = false;", "assert (!a);"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "boolean a = false;", "assertThat(a).isFalse();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsCase() {
    helper
        .addInputLines(
            INPUT, inputWithExpressions("String a = \"test\";", "assert a.equals(\"test\");"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "String a = \"test\";", "assertThat(a).isEqualTo(\"test\");", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = null;", "assert a == null;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = null;", "assertThat(a).isNull();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCaseLeftSide() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = null;", "assert null == a;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = null;", "assertThat(a).isNull();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCaseWithDetail() {
    helper
        .addInputLines(
            INPUT, inputWithExpressions("Integer a = null;", "assert a == null : \"detail\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = null;",
                "assertWithMessage(\"detail\").that(a).isNull();",
                ASSERT_WITH_MESSAGE_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertNotEqualsNullCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = 1;", "assert a != null;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = 1;", "assertThat(a).isNotNull();", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceSameCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = 1;", "assert a == 1;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = 1;", "assertThat(a).isSameAs(1);", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceWithParensCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = 1;", "assert (a == 1);"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = 1;", "assertThat(a).isSameAs(1);", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceNotSameCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("Integer a = 1;", "assert a != 1;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "Integer a = 1;", "assertThat(a).isNotSameAs(1);", ASSERT_THAT_IMPORT))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceSameCaseWithDetailCase() {
    helper
        .addInputLines(INPUT, inputWithExpressions("int a = 1;", "assert a == 1 : \"detail\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                "int a = 1;",
                "assertWithMessage(\"detail\").that(a).isSameAs(1);",
                ASSERT_WITH_MESSAGE_IMPORT))
        .doTest();
  }

  private static String[] inputWithExpressionAndImport(String expr, String importPath) {
    return inputWithExpressionsAndImport(expr, "", importPath);
  }

  private static String[] inputWithExpressionsAndImport(
      String expr1, String expr2, String importPath) {
    return new String[] {
      importPath.isEmpty() ? "" : String.format("import %s", importPath),
      "import org.junit.Test;",
      "import org.junit.runner.RunWith;",
      "import org.junit.runners.JUnit4;",
      "@RunWith(JUnit4.class)",
      "public class FooTest {",
      "  @Test",
      "  void foo() {",
      String.format("    %s", expr1),
      expr2.isEmpty() ? "" : String.format("    %s", expr2),
      "  }",
      "}"
    };
  }

  private static String[] inputWithExpression(String expr) {
    return inputWithExpressionAndImport(expr, "");
  }

  private static String[] inputWithExpressions(String expr1, String expr2) {
    return inputWithExpressionsAndImport(expr1, expr2, "");
  }
}
