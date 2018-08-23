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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link SubstringOfZero}. */
@RunWith(JUnit4.class)
public class SubstringOfZeroTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new SubstringOfZero(), getClass());

  @Test
  public void positiveJustVars() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String x = \"hello\";",
            "    String y = x.substring(0);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String x = \"hello\";",
            "    String y = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveVarsWithMethods() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "   String x = \"HELLO\";",
            "   String y = x.toLowerCase().substring(0);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "   String x = \"HELLO\";",
            "   String y = x.toLowerCase();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeJustVarsOneArg() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String x = \"hello\";",
            "    String y = x.substring(1);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeJustVarsTwoArg() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String x = \"hello\";",
            "    String y = x.substring(1,3);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeVarsWithMethodsOneArg() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "   String x = \"HELLO\";",
            "   String y = x.toLowerCase().substring(1);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeVarsWithMethodsTwoArg() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "   String x = \"HELLO\";",
            "   String y = x.toLowerCase().substring(1,3);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeVarsWithDifferentMethod() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "   String x = \"HELLO\";",
            "   char y = x.charAt(0);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveStringLiteral() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public static final int MY_CONSTANT = 0;",
            "  void f() {",
            "    String x = \"hello\".substring(MY_CONSTANT);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  public static final int MY_CONSTANT = 0;",
            "  void f() {",
            "    String x = \"hello\";",
            "  }",
            "}")
        .doTest();
  }
}
