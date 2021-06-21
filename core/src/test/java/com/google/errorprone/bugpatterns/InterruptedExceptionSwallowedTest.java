/*
 * Copyright 2019 The Error Prone Authors.
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

/** Unit tests for {@link InterruptedExceptionSwallowed}. */
@RunWith(JUnit4.class)
public final class InterruptedExceptionSwallowedTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InterruptedExceptionSwallowed.class, getClass())
          .addSourceLines(
              "Thrower.java",
              "class Thrower implements AutoCloseable {",
              "  public void close() throws InterruptedException {}",
              "}");

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(InterruptedExceptionSwallowed.class, getClass());

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      throw new Exception();",
            "    } catch (Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNestedCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "       try {",
            "         future.get();",
            "       } catch (InterruptedException e) {}",
            "    } catch (Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveRethrown() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "       try {",
            "         future.get();",
            "       } catch (InterruptedException e) {",
            "         throw e;",
            "       }",
            "    // BUG: Diagnostic contains:",
            "    } catch (Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thrownByClose_throwsClauseTooBroad() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  void test() throws Exception {",
            "    try (Thrower t = new Thrower()) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thrownByClose_caughtByOuterCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test() {",
            "    try {",
            "      try (Thrower t = new Thrower()) {",
            "      }",
            "    // BUG: Diagnostic contains:",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_fieldNamedClose() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  class Mischief implements AutoCloseable {",
            "    public int close = 1;",
            "    public void close() {}",
            "  }",
            "  void test() {",
            "    try (Mischief m = new Mischief()) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_rethrown() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test() throws InterruptedException, Exception {",
            "    try {",
            "      try (Thrower t = new Thrower()) {",
            "      }",
            "    } catch (Exception e) {",
            "      throw e;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thrownByClose_inherited() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  class ThrowingParent implements AutoCloseable {",
            "    public void close() throws InterruptedException {}",
            "  }",
            "  class ThrowingChild extends ThrowingParent {}",
            "  // BUG: Diagnostic contains:",
            "  void test() throws Exception {",
            "    try (ThrowingChild t = new ThrowingChild()) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thrownByClose_swallowedSilently() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test() {",
            "    try (Thrower t = new Thrower()) {",
            "    // BUG: Diagnostic contains:",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveThrowFromCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      try {",
            "        future.get();",
            "      } catch (ExecutionException e) {",
            "        if (e.getCause() instanceof IllegalStateException) {",
            "          throw new InterruptedException();",
            "        }",
            "      } catch (InterruptedException e) {",
            "        Thread.currentThread().interrupt();",
            "        throw new IllegalStateException(e);",
            "      }",
            "    // BUG: Diagnostic contains:",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void checkedViaInstanceof_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      throw new Exception();",
            "    } catch (Exception e) {",
            "      if (e instanceof InterruptedException) {",
            "        Thread.currentThread().interrupt();",
            "      }",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSimpleCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    // BUG: Diagnostic contains:",
            "    } catch (Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (Exception e) {",
            "      if (e instanceof InterruptedException) {",
            "        Thread.currentThread().interrupt();",
            "      }",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveRefactoringEmptyCatch() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (Exception e) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (Exception e) {",
            "      if (e instanceof InterruptedException) {",
            "        Thread.currentThread().interrupt();",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeExplicitlyListed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (ExecutionException | InterruptedException e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) {",
            "    try {",
            "      future.get();",
            "    } catch (@SuppressWarnings(\"InterruptedExceptionSwallowed\") Exception e) {",
            "      throw new IllegalStateException(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hiddenInMethodThrows() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  void test(Future<?> future) throws Exception {",
            "    future.get();",
            "    throw new IllegalStateException();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  void test(Future<?> future) throws ExecutionException, InterruptedException {",
            "    future.get();",
            "    throw new IllegalStateException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hiddenInMethodThrows_butActuallyThrowsException_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) throws Exception {",
            "    future.get();",
            "    throw new Exception();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hiddenInMethodThrows_throwsSimplified() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.io.FileNotFoundException;",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  // BUG: Diagnostic contains: ExecutionException, IOException, InterruptedException",
            "  void test(Future<?> future) throws Exception {",
            "    future.get();",
            "    if (true) {",
            "      throw new IOException();",
            "    } else {",
            "      throw new FileNotFoundException();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hiddenInMethodThrows_bailsIfTooManySpecificExceptions() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "import java.util.concurrent.TimeoutException;",
            "class Test {",
            "  void test(Future<?> future) throws Exception {",
            "    future.get();",
            "    if (hashCode() == 0) {",
            "      throw new A();",
            "     }",
            "    if (hashCode() == 0) {",
            "      throw new B();",
            "     }",
            "    if (hashCode() == 0) {",
            "      throw new C();",
            "     }",
            "    if (hashCode() == 0) {",
            "      throw new D();",
            "     }",
            "    if (hashCode() == 0) {",
            "      throw new E();",
            "     }",
            "  }",
            "  static class A extends Exception {}",
            "  static class B extends Exception {}",
            "  static class C extends Exception {}",
            "  static class D extends Exception {}",
            "  static class E extends Exception {}",
            "}")
        .doTest();
  }

  @Test
  public void throwsExceptionButNoSignOfInterrupted() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) throws Exception {",
            "    throw new Exception();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void declaredInMethodThrows() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void test(Future<?> future) throws InterruptedException, ExecutionException {",
            "    future.get();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void declaredInMain() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ExecutionException;",
            "import java.util.concurrent.Future;",
            "public class Test {",
            "  private static final Future<?> future = null;",
            "  public static void main(String[] argv) throws Exception {",
            "    future.get();",
            "  }",
            "}")
        .doTest();
  }
}
