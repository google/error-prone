/*
 * Copyright 2023 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class UnnecessaryAsyncTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnnecessaryAsync.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryAsync.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "class Test {",
            "  int test() {",
            "    // BUG: Diagnostic contains:",
            "    var ai = new AtomicInteger();",
            "    ai.set(1);",
            "    return ai.get();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringInteger() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "class Test {",
            "  int test() {",
            "    var ai = new AtomicInteger();",
            "    ai.set(1);",
            "    return ai.get();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "class Test {",
            "  int test() {",
            "    int ai = 0;",
            "    ai = 1;",
            "    return ai;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringReference() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  String test() {",
            "    var ar = new AtomicReference<String>(null);",
            "    ar.compareAndSet(null, \"foo\");",
            "    return ar.get();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  String test() {",
            "    String ar = null;",
            "    ar = \"foo\";",
            "    return ar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_unfixable_noAttempt() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  String test() {",
            "    var ar = new AtomicReference<String>(null);",
            "    return ar.toString();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void refactoring_rawType() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  Object test() {",
            "    var ar = new AtomicReference();",
            "    ar.set(\"foo\");",
            "    return ar.get();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  Object test() {",
            "    Object ar = null;",
            "    ar = \"foo\";",
            "    return ar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_escapesScope() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "class Test {",
            "  AtomicInteger test() {",
            "    var ai = new AtomicInteger();",
            "    ai.set(1);",
            "    return ai;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_passedToAnotherMethod() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "class Test {",
            "  void test() {",
            "    var ai = new AtomicInteger();",
            "    ai.set(1);",
            "    frobnicate(ai);",
            "  }",
            "  void frobnicate(Number n) {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_uselessConcurrentMap() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ConcurrentHashMap;",
            "class Test {",
            "  int test() {",
            "    // BUG: Diagnostic contains:",
            "    var chm = new ConcurrentHashMap<>();",
            "    chm.put(1, 2);",
            "    return chm.size();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_capturedByLambda() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "import java.util.List;",
            "class Test {",
            "  long test(List<String> xs) {",
            "    var ai = new AtomicInteger();",
            "    return xs.stream().mapToLong(x -> ai.getAndIncrement()).sum();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void atomicReference_unfixable() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: String result = null;",
            "    AtomicReference<String> result = new AtomicReference<>();",
            "  }",
            "}")
        .doTest();
  }
}
