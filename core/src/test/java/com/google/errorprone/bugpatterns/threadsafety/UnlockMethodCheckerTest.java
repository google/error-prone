/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnlockMethodChecker}Test */
@RunWith(JUnit4.class)
public class UnlockMethodCheckerTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(UnlockMethodChecker.class, getClass());
  }

  @Test
  public void testUnlocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  @UnlockMethod({\"lock1\", \"lock2\"}) void m() {",
            "    lock1.unlock();",
            "    lock2.unlock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testUnlockedAndLocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  // BUG: Diagnostic contains: not released: this.lock1, this.lock2",
            "  @UnlockMethod({\"lock1\", \"lock2\"}) void m() {",
            "    lock1.unlock();",
            "    lock2.unlock();",
            "    lock1.lock();",
            "    lock2.lock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testUnlockedRWLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  ReentrantReadWriteLock lock;",
            "  @UnlockMethod(\"lock\")",
            "  void m() {",
            "    lock.readLock().unlock();",
            "  }",
            "  @UnlockMethod(\"lock\")",
            "  void n() {",
            "    lock.writeLock().unlock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testUnlockedMonitor() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  @UnlockMethod(\"monitor\")",
            "  void m() {",
            "    monitor.leave();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNotUnlocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  // BUG: Diagnostic contains: not released: this.lock1, this.lock2",
            "  @UnlockMethod({\"lock1\", \"lock2\"}) void m() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNotUnlockedRWLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  ReentrantReadWriteLock lock;",
            "  // BUG: Diagnostic contains: not released: this.lock",
            "  @UnlockMethod(\"lock\") void n() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNotUnlockedMonitor() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  // BUG: Diagnostic contains: not released: this.monitor",
            "  @UnlockMethod(\"monitor\") void m() {}",
            "}")
        .doTest();
  }

  @Test
  public void testBadLockExpression() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "class Test {",
            "  // BUG: Diagnostic contains: Could not resolve lock expression.",
            "  @UnlockMethod(\"mu\") void m() {}",
            "}")
        .doTest();
  }
}
