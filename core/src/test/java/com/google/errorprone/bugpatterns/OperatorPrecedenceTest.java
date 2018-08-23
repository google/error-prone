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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link OperatorPrecedence}Test */
@RunWith(JUnit4.class)
public class OperatorPrecedenceTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(OperatorPrecedence.class, getClass());

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new OperatorPrecedence(), getClass());

  @Test
  public void positive() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(boolean a, boolean b, boolean c) {",
            "    // BUG: Diagnostic contains: (a && b) || c",
            "    boolean r = a && b || c;",
            "    // BUG: Diagnostic contains: a || (b && c)",
            "    r = a || b && c;",
            "    // BUG: Diagnostic contains: a || (b && c) || !(b && c)",
            "    r = a || b && c || !(b && c);",
            "    return r;",
            "  }",
            "  int f(int a, int b) {",
            "    // BUG: Diagnostic contains: (a + b) << 2",
            "    return a + b << 2;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int f(int a, int b) {",
            "    int r = a + a * b;",
            "    return r;",
            "  }",
            "  boolean f(boolean a, boolean b) {",
            "    boolean r = (a && b) || (!a && !b);",
            "    r = (a = a && b);",
            "    return r;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveNotSpecialParenthesisCase() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "    boolean r = a || (b && c) && (d && e);",
            "    return r;",
            "  }",
            "  int f2(int a, int b, int c, int d) {",
            "    int e = a << (b + c) + d;",
            "    return e;",
            "  }",
            "  boolean f3(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "    boolean r = a || b && c;",
            "    return r;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  boolean f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "    boolean r = a || ((b && c) && (d && e));",
            "    return r;",
            "  }",
            "  int f2(int a, int b, int c, int d) {",
            "    int e = a << (b + c + d);",
            "    return e;",
            "  }",
            "  boolean f3(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "    boolean r = a || (b && c);",
            "    return r;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extraParenthesis() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "   boolean g = (a || (b && c && d) && e);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "   boolean g = (a || (b && c && d && e));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rightAndParenthesis() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d) {",
            "   boolean g = a || b && (c && d);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d) {",
            "   boolean g = a || (b && c && d);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void leftAndParenthesis() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d) {",
            "   boolean g = a || (b && c) && d;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d) {",
            "   boolean g = a || (b && c && d);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void aLotOfParenthesis() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "   boolean g = (a || (b && c && d) && e);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            " void f(boolean a, boolean b, boolean c, boolean d, boolean e) {",
            "   boolean g = (a || (b && c && d && e));",
            "  }",
            "}")
        .doTest();
  }
}
