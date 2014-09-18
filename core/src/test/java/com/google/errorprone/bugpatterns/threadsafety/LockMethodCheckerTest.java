/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LockMethodChecker}Test */
@RunWith(JUnit4.class)
public class LockMethodCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(new LockMethodChecker());

  @Test
  public void testLocked() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  @LockMethod({\"lock1\", \"lock2\"})",
            "  void m() {",
            "    lock1.lock();",
            "    lock2.lock();",
            "  }",
            "}"
        )
    );
  }
  
  @Test
  public void testLockedAndUnlocked() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
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
            "}"
        )
    );
  }
  
  @Test
  public void testLockedRWLock() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
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
            "}"
        )
    );
  }
  
  @Test
  public void testLockedMonitor() throws Exception {
    compilationHelper.assertCompileSucceeds(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  @LockMethod(\"monitor\")",
            "  void m() {",
            "    monitor.enter();",
            "  }",
            "}"
        )
    );
  }
  
  @Test
  public void testNotLocked() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock1;",
            "  Lock lock2;",
            "  // BUG: Diagnostic contains: not acquired: this.lock1, this.lock2",
            "  @LockMethod({\"lock1\", \"lock2\"}) void m() {}",
            "}"
        )
    );
  }
  
  @Test
  public void testNotLockedRWLock() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  ReentrantReadWriteLock lock;",
            "  // BUG: Diagnostic contains: not acquired: this.lock",
            "  @LockMethod(\"lock\") void n() {}",
            "}"
        )
    );
  }
  
  @Test
  public void testNotLockedMonitor() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  Monitor monitor;",
            "  // BUG: Diagnostic contains: not acquired: this.monitor",
            "  @LockMethod(\"monitor\") void m() {}",
            "}"
        )
    );
  }
  
  @Test
  public void testBadLockExpression() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.bugpatterns.threadsafety.annotations.LockMethod;",
            "class Test {",
            "  // BUG: Diagnostic contains: Could not resolve lock expression.",
            "  @LockMethod(\"mu\") void m() {}",
            "}"
        )
    );
  }
}