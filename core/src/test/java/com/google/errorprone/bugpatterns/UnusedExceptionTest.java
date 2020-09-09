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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link UnusedException} bug pattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class UnusedExceptionTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnusedException.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnusedException(), getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "      // BUG: Diagnostic contains: ",
            "    } catch (Exception e) {",
            "      throw new RuntimeException(\"foo\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      if (equals(this)) {",
            "        throw new RuntimeException(toString());",
            "      } else {",
            "        throw new RuntimeException();",
            "      }",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      if (equals(this)) {",
            "        throw new RuntimeException(toString(), e);",
            "      } else {",
            "        throw new RuntimeException(e);",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctlyWrapped() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      throw new RuntimeException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rethrown() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      throw e;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedSomehow() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      System.err.println(e.toString());",
            "      throw new RuntimeException();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedNested() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      try {",
            "      // BUG: Diagnostic contains: ",
            "      } catch (Exception e2) {",
            "        System.err.println(e.toString());",
            "        throw new RuntimeException();",
            "      }",
            "      throw new RuntimeException();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void swallowedButDoesntThrow() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      String ohNo = null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"UnusedException\")",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      throw new RuntimeException(\"foo\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressibleViaCatchBlock() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (@SuppressWarnings(\"UnusedException\") Exception e) {",
            "      throw new RuntimeException(\"foo\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressibleByCallingExceptionUnused() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception unusedException) {",
            "      throw new RuntimeException(\"foo\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClass() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      throw new RuntimeException() {};",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            "      throw new RuntimeException(e) {};",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replacementNotVisible() {
    refactoringHelper
        .addInputLines(
            "in/MyException.java",
            "class MyException extends RuntimeException {",
            "  public MyException(int a) {}",
            "  protected MyException(int a, Throwable th) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "    } catch (Exception e) {",
            // Not refactored as MyException(int, Throwable) isn't visible.
            "      throw new MyException(1);",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void interruptedException_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    try {",
            "      throw new InterruptedException();",
            "    } catch (InterruptedException e) {",
            "      throw new IllegalStateException(\"foo\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
