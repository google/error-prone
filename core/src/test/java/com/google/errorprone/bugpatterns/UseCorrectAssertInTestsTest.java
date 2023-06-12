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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author galitch@google.com (Anton Galitch)
 */
@RunWith(JUnit4.class)
public final class UseCorrectAssertInTestsTest {

  private static final String ASSERT_THAT_IMPORT =
      "import static com.google.common.truth.Truth.assertThat;";
  private static final String ASSERT_WITH_MESSAGE_IMPORT =
      "import static com.google.common.truth.Truth.assertWithMessage;";
  private static final String INPUT = "in/FooTest.java";
  private static final String OUTPUT = "out/FooTest.java";

  private static final String TEST_ONLY = "-XepCompilingTestOnlyCode";

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UseCorrectAssertInTests.class, getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UseCorrectAssertInTests.class, getClass());

  @Test
  public void correctAssertInTest() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "assertThat(true).isTrue();"))
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noAssertInTestsFound() {
    refactoringHelper
        .addInputLines(INPUT, inputWithExpressions("int a = 1;"))
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void diagnosticIssuedAtFirstAssert() {
    compilationHelper
        .addSourceLines(
            INPUT,
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class FooTest {",
            "  void foo() {",
            "    int x = 1;",
            "    // BUG: Diagnostic contains: UseCorrectAssertInTests",
            "    assert true;",
            "    assert true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertInNonTestMethod() {
    refactoringHelper
        .addInputLines(
            INPUT,
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class FooTest {",
            "  void foo() {",
            "    assert true;",
            "  }",
            "}")
        .addOutputLines(
            OUTPUT,
            ASSERT_THAT_IMPORT,
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class FooTest {",
            "  void foo() {",
            "    assertThat(true).isTrue();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertInTestOnlyCode() {
    refactoringHelper
        .addInputLines(
            INPUT,
            "public class FooTest {", //
            "  void foo() {",
            "    assert true;",
            "  }",
            "}")
        .addOutputLines(
            OUTPUT,
            ASSERT_THAT_IMPORT,
            "public class FooTest {",
            "  void foo() {",
            "    assertThat(true).isTrue();",
            "  }",
            "}")
        .setArgs(TEST_ONLY)
        .doTest();
  }

  @Test
  public void assertInNonTestCode() {
    refactoringHelper
        .addInputLines(
            INPUT,
            "public class FooTest {", //
            "  void foo() {",
            "    assert true;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithParentheses() {
    refactoringHelper
        .addInputLines(INPUT, inputWithExpressions("assert (true);"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "assertThat(true).isTrue();"))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithoutParentheses() {
    refactoringHelper
        .addInputLines(INPUT, inputWithExpressions("assert true;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "assertThat(true).isTrue();"))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailString() {
    refactoringHelper
        .addInputLines(INPUT, inputWithExpressions("assert (true) : \"description\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_WITH_MESSAGE_IMPORT, //
                "assertWithMessage(\"description\").that(true).isTrue();"))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailStringVariable() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "String desc = \"description\";", //
                "assert (true) : desc;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_WITH_MESSAGE_IMPORT, //
                "String desc = \"description\";",
                "assertWithMessage(desc).that(true).isTrue();"))
        .doTest();
  }

  @Test
  public void wrongAssertInTestWithDetailNonStringVariable() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer desc = 1;", //
                "assert (true) : desc;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_WITH_MESSAGE_IMPORT, //
                "Integer desc = 1;",
                "assertWithMessage(desc.toString()).that(true).isTrue();"))
        .doTest();
  }

  @Test
  public void wrongAssertFalseCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "boolean a = false;", //
                "assert (!a);"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "boolean a = false;",
                "assertThat(a).isFalse();"))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "String a = \"test\";", //
                "assert a.equals(\"test\");"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "String a = \"test\";",
                "assertThat(a).isEqualTo(\"test\");"))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = null;", //
                "assert a == null;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = null;",
                "assertThat(a).isNull();"))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCaseLeftSide() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = null;", //
                "assert null == a;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = null;",
                "assertThat(a).isNull();"))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsNullCaseWithDetail() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = null;", //
                "assert a == null : \"detail\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_WITH_MESSAGE_IMPORT, //
                "Integer a = null;",
                "assertWithMessage(\"detail\").that(a).isNull();"))
        .doTest();
  }

  @Test
  public void wrongAssertNotEqualsNullCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "assert a != null;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "assertThat(a).isNotNull();"))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceSameCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "Integer b = 1;",
                "assert a == b;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "Integer b = 1;",
                "assertThat(a).isSameInstanceAs(b);"))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceWithParensCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "Integer b = 1;",
                "assert (a == b);"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "Integer b = 1;",
                "assertThat(a).isSameInstanceAs(b);"))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceNotSameCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "Integer b = 1;",
                "assert a != b;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "Integer b = 1;",
                "assertThat(a).isNotSameInstanceAs(b);"))
        .doTest();
  }

  @Test
  public void wrongAssertEqualsPrimitiveCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "assert a == 1;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "assertThat(a).isEqualTo(1);"))
        .doTest();
  }

  @Test
  public void wrongAssertNotEqualsPrimitiveCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "Integer a = 1;", //
                "assert a != 1;"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_THAT_IMPORT, //
                "Integer a = 1;",
                "assertThat(a).isNotEqualTo(1);"))
        .doTest();
  }

  @Test
  public void wrongAssertReferenceSameCaseWithDetailCase() {
    refactoringHelper
        .addInputLines(
            INPUT,
            inputWithExpressions(
                "int a = 1;", //
                "assert a == 1 : \"detail\";"))
        .addOutputLines(
            OUTPUT,
            inputWithExpressionsAndImport(
                ASSERT_WITH_MESSAGE_IMPORT, //
                "int a = 1;",
                "assertWithMessage(\"detail\").that(a).isEqualTo(1);"))
        .doTest();
  }

  private static String[] inputWithExpressionsAndImport(String importStatement, String... body) {

    return new String[] {
      importStatement,
      "import org.junit.Test;",
      "import org.junit.runner.RunWith;",
      "import org.junit.runners.JUnit4;",
      "@RunWith(JUnit4.class)",
      "public class FooTest {",
      "  @Test",
      "  void foo() {",
      stream(body).map(line -> String.format("    %s", line)).collect(joining("\n")),
      "  }",
      "}"
    };
  }

  private static String[] inputWithExpressions(String... expr) {
    return inputWithExpressionsAndImport("", expr);
  }
}
