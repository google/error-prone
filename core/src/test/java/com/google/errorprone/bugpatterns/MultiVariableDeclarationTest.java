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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MultiVariableDeclaration}Test */
@RunWith(JUnit4.class)
public class MultiVariableDeclarationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MultiVariableDeclaration.class, getClass());

  @Test
  public void positivePosition() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "package a;",
            "public class A {",
            "  int a;",
            "  // BUG: Diagnostic contains:",
            "  int x = 1, y = 2;",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "public class A {",
            "  int x = 1, y = 2;",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "public class A {",
            "  int x = 1; int y = 2;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveWithNeighbours() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java",
            "package a;",
            "public class A {",
            "  int a = 1;",
            "  int x = 1, y = 2;",
            "  int b = 1;",
            "}")
        .addOutputLines(
            "out/A.java",
            "package a;",
            "public class A {",
            "  int a = 1;",
            "  int x = 1; int y = 2;",
            "  int b = 1;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveWithNeighbouringScopes() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java",
            "package a;",
            "public class A {",
            "  {",
            "    int a = 1;",
            "  }",
            "  int x = 1, y = 2;",
            "  {",
            "    int a = 1;",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "package a;",
            "public class A {",
            "  {",
            "    int a = 1;",
            "  }",
            "  int x = 1; int y = 2;",
            "  {",
            "    int a = 1;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveCinit() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "public class A {",
            "  { int x = 1, y = 2; }",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "public class A {",
            "  { int x = 1; int y = 2; }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "a/A.java", //
            "package a;",
            "public class A {",
            "  int x = 1;",
            "int y = 2;",
            "}")
        .doTest();
  }

  @Test
  public void negativeForLoop() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  void f() {",
            "    for (int x = 1, y = 2;;) { }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveAnnotation() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "public class A {",
            "  @Deprecated int x = 1, y = 2;",
            "}")
        .addOutputLines(
            "out/A.java",
            "package a;",
            "public class A {",
            // javac's pretty printer uses the system line separator
            "  @Deprecated()"
                + System.lineSeparator()
                + "int x = 1; @Deprecated()"
                + System.lineSeparator()
                + "int y = 2;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveArrayDimensions() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "public class A {",
            "  int[] x = {0}, y[] = {{0}};",
            "}")
        .addOutputLines(
            "out/A.java",
            "package a;",
            "public class A {",
            "  int[] x = {0}; int[][] y = {{0}};",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveNoInitializer() {
    BugCheckerRefactoringTestHelper.newInstance(new MultiVariableDeclaration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "public class A {",
            "  int x, y;",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "public class A {",
            "  int x; int y;",
            "}")
        .doTest(TEXT_MATCH);
  }
}
