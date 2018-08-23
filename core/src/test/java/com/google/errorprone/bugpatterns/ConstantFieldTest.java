/*
 * Copyright 2016 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ConstantField}Test */
@RunWith(JUnit4.class)
public class ConstantFieldTest {
  CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ConstantField.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: static final Object CONSTANT1 = 42;",
            "  Object CONSTANT1 = 42;",
            "  // BUG: Diagnostic contains: @Deprecated static final Object CONSTANT2 = 42;",
            "  @Deprecated Object CONSTANT2 = 42;",
            "  // BUG: Diagnostic contains: static final Object CONSTANT3 = 42;",
            "  static Object CONSTANT3 = 42;",
            "  // BUG: Diagnostic contains: static final Object CONSTANT4 = 42;",
            "  final Object CONSTANT4 = 42;",
            "  // BUG: Diagnostic contains:"
                + " @Deprecated private static final Object CONSTANT5 = 42;",
            "  @Deprecated private Object CONSTANT5 = 42;",
            "}")
        .doTest();
  }

  @Test
  public void rename() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: 'Object constantCaseName = 42;'",
            "  Object CONSTANT_CASE_NAME = 42;",
            "}")
        .doTest();
  }

  @Test
  public void typo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: PROJECT_DATA_SUBDIRECTORY",
            "  private static final String PROJECT_DATA_SUBDIREcTORY = \".project\";",
            "}")
        .doTest();
  }

  @Test
  public void snakeCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: SNAKE_CASE_VARIABLE",
            "  private static final String snake_case_variable = \"Kayla\";",
            "}")
        .doTest();
  }

  @Test
  public void skipStaticFixOnInners() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class Inner {",
            "    // BUG: Diagnostic matches: F",
            "    final String CONSTANT_CASE_NAME = \"a\";",
            "  }",
            "  enum InnerEnum {",
            "   FOO {",
            "     // BUG: Diagnostic matches: F",
            "     final String CONSTANT_CASE_NAME = \"a\";",
            "   };",
            "   // BUG: Diagnostic contains: static final String CAN_MAKE_STATIC",
            "   final String CAN_MAKE_STATIC = \"\";",
            "  }",
            "}")
        .expectErrorMessage(
            "F", d -> !d.contains("static final String") && d.contains("ConstantField"))
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final Object CONSTANT = 42;",
            "  Object nonConst = 42;",
            "  public static final Object FLAG_foo = new Object();",
            "  protected static final int FOO_BAR = 100;",
            "  static final int FOO_BAR2 = 100;",
            "  private static final int[] intArray = {0};",
            "  private static final Object mutable = new Object();",
            "}")
        .doTest();
  }

  @Test
  public void renameUsages() {
    BugCheckerRefactoringTestHelper.newInstance(new ConstantField(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object CONSTANT_CASE = 42;",
            "  void f() {",
            "    System.err.println(CONSTANT_CASE);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object constantCase = 42;",
            "  void f() {",
            "    System.err.println(constantCase);",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void primitivesAreConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:"
                + " ints are immutable, field should be named 'CONSTANT'",
            "  static final int constant = 42;",
            "  // BUG: Diagnostic contains:"
                + " Integers are immutable, field should be named 'BOXED_CONSTANT'",
            "  static final Integer boxedConstant = 42;",
            "  // BUG: Diagnostic contains:"
                + " Strings are immutable, field should be named 'STRING_CONSTANT'",
            "  static final String stringConstant = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void primitivesAreConstant_negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final int CONSTANT = 42;",
            "  static final Integer BOXED_CONSTANT = 42;",
            "  static final String STRING_CONSTANT = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void primitivesAreConstant_serializable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.ObjectStreamField;",
            "import java.io.Serializable;",
            "class Test implements Serializable {",
            "  private static final long serialVersionUID = 1L;",
            "  private static final ObjectStreamField[] serialPersistentFields = {};",
            "}")
        .doTest();
  }

  @Test
  public void positiveEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.element.ElementKind;",
            "interface Test {",
            "  // BUG: Diagnostic contains: 'ElementKind KIND = ElementKind.FIELD;'",
            "  ElementKind Kind = ElementKind.FIELD;",
            "}")
        .doTest();
  }

  @Test
  public void cppStyle() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.element.ElementKind;",
            "interface Test {",
            "  // BUG: Diagnostic contains: int MAX_FOOS = 42",
            "  static final int kMaxFoos = 42;",
            "}")
        .doTest();
  }
}
