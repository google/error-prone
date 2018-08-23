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
 * Tests for {@link LockNotBeforeTry} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class LockNotBeforeTryTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LockNotBeforeTry.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new LockNotBeforeTry(), getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      lock.lock();",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    lock.lock();",
            "    try {",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresMultipleLocks() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lockA, ReentrantLock lockB) {",
            "    try {",
            "      lockA.lock();",
            "      lockB.lock();",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lockA.unlock();",
            "      lockB.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorToBefore() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    try {",
            "      lock.lock();",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    lock.lock();",
            "    try {",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorIntermediate() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    lock.lock();",
            "    System.out.println(\"hi\");",
            "    try {",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    System.out.println(\"hi\");",
            "    lock.lock();",
            "    try {",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactorUnlockOutsideTry() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    lock.lock();",
            "    System.out.println(\"hi\");",
            "    lock.unlock();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantLock;",
            "class Test {",
            "  private void test(ReentrantLock lock) {",
            "    lock.lock();",
            "    try {",
            "      System.out.println(\"hi\");",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
