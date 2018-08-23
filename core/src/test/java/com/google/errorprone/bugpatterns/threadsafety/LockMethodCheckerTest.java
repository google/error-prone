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

/** {@link LockMethodChecker}Test */
@RunWith(JUnit4.class)
public class LockMethodCheckerTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(LockMethodChecker.class, getClass());
  }

  @Test
  public void testLocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  @LockMethod({\"lock1\", \"lock2\"})",
            "  void m() {",
            "    lock1.lock();",
            "    lock2.lock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLockedAndUnlocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  // BUG: Diagnostic contains: not acquired: this.lock1, this.lock2",
            "  @LockMethod({\"lock1\", \"lock2\"}) void m() {",
            "    lock1.lock();",
            "    lock2.lock();",
            "    lock1.unlock();",
            "    lock2.unlock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLockedRWLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  ReentrantReadWriteLock lock;",
            "  @LockMethod(\"lock\")",
            "  void m() {",
            "    lock.readLock().lock();",
            "  }",
            "  @LockMethod(\"lock\")",
            "  void n() {",
            "    lock.writeLock().lock();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLockedMonitor() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  @LockMethod(\"monitor\")",
            "  void m() {",
            "    monitor.enter();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNotLocked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  // BUG: Diagnostic contains: not acquired: this.lock1, this.lock2",
            "  @LockMethod({\"lock1\", \"lock2\"}) void m() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNotLockedRWLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  ReentrantReadWriteLock lock;",
            "  // BUG: Diagnostic contains: not acquired: this.lock",
            "  @LockMethod(\"lock\") void n() {}",
            "}")
        .doTest();
  }

  @Test
  public void testNotLockedMonitor() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  // BUG: Diagnostic contains: not acquired: this.monitor",
            "  @LockMethod(\"monitor\") void m() {}",
            "}")
        .doTest();
  }

  @Test
  public void testBadLockExpression() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "class Test {",
            "  // BUG: Diagnostic contains: Could not resolve lock expression.",
            "  @LockMethod(\"mu\") void m() {}",
            "}")
        .doTest();
  }
}
