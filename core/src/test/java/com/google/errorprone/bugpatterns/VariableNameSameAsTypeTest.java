/*
 * Copyright 2018 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kayco@google.com (Kayla Walker) & seibelsabrina@google.com (Sabrina Seibel) */
@RunWith(JUnit4.class)
public class VariableNameSameAsTypeTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(VariableNameSameAsType.class, getClass());

  @Test
  public void positiveInsideMethod() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: VariableNameSameAsType",
            "    String String; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveLambda() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: Variable named String has the type java.lang.String",
            "    Predicate<String> p = (String) -> String.isEmpty(); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInitialized() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: VariableNameSameAsType",
            "    String String = \"Kayla\"; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInitializedSeparate() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: VariableNameSameAsType",
            "    String String; ",
            "    String = \"Kayla\"; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveField() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: VariableNameSameAsType",
            "  String String;",
            "  void f() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLowerCase() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    String string;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInitializedLowerCase() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    String string = \"Kayla\"; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInitializedSeparate() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String string; ",
            "    string = \"Kayla\"; ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOther() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    String t; ",
            "  }",
            "}")
        .doTest();
  }
}
