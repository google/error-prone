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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/** {@link GuardedByChecker}Test */
@RunWith(JUnit4.class)
public class GuardedByCheckerTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new GuardedByChecker(), getClass());
  }

  @Test
  public void testLocked() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  final Lock lock = null;",
            "  @GuardedBy(\"lock\")",
            "  int x;",
            "  void m() {",
            "    lock.lock();",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.lock'",
            "    x++;",
            "    try {",
            "      x++;",
            "    } catch (Exception e) {",
            "      x--;",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.lock'",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * "static synchronized method() { ... }" == "synchronized (MyClass.class) { ... }"
   */
  @Test
  public void testStaticLocked() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  @GuardedBy(\"Test.class\")",
            "  static int x;",
            "  static synchronized void m() {",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMonitor() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.common.util.concurrent.Monitor;",
            "class Test {",
            "  final Monitor monitor = null;",
            "  @GuardedBy(\"monitor\")",
            "  int x;",
            "  void m() {",
            "    monitor.enter();",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.monitor'",
            "    x++;",
            "    try {",
            "      x++;",
            "    } finally {",
            "      monitor.leave();",
            "    }",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.monitor'",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testWrongLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  final Lock lock1 = null;",
            "  final Lock lock2 = null;",
            "  @GuardedBy(\"lock1\")",
            "  int x;",
            "  void m() {",
            "    lock2.lock();",
            "    try {",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.lock1'",
            "      x++;",
            "    } finally {",
            "      lock2.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testGuardedStaticFieldAccess_1() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  public static final Object lock = new Object();",
            "  @GuardedBy(\"lock\")",
            "  public static int x;",
            "  void m() {",
            "    synchronized (Test.lock) {",
            "      Test.x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testGuardedStaticFieldAccess_2() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  public static final Object lock = new Object();",
            "  @GuardedBy(\"lock\")",
            "  public static int x;",
            "  void m() {",
            "    synchronized (lock) {",
            "      Test.x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testGuardedStaticFieldAccess_3() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  public static final Object lock = new Object();",
            "  @GuardedBy(\"lock\")",
            "  public static int x;",
            "  void m() {",
            "    synchronized (Test.lock) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testGuardedStaticFieldAccess_EnclosingClass() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"Test.class\")",
            "  public static int x;",
            "  synchronized static void n() {",
            "    Test.x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBadStaticFieldAccess() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  public static final Object lock = new Object();",
            "  @GuardedBy(\"lock\")",
            "  public static int x;",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'Test.lock'",
            "    Test.x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBadGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains: Invalid @GuardedBy expression",
            "  @GuardedBy(\"foo\") int y;",
            "}")
        .doTest();
  }

  @Test
  public void testUnheldInstanceGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  final Object mu = new Object();",
            "  @GuardedBy(\"mu\") int y;",
            "}",
            "class Main {",
            "  void m(Test t) {",
            "    // BUG: Diagnostic contains:",
            "      // should be guarded by 't.mu'",
            "    t.y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCtor() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"this\") int x;",
            "  public Test() {",
            "    this.x = 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBadGuardMethodAccess() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"this\") void x() {}",
            "  void m() {",
            "    // BUG: Diagnostic contains: this",
            "    x();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTransitiveGuardMethodAccess() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {",
            "  @GuardedBy(\"this\") void x() {}",
            "  @GuardedBy(\"this\") void m() {",
            "    x();",
            "  }",
            "}")
        .doTest();
  }

  @Ignore // TODO(user): support read/write lock copies
  @Test
  public void testReadWriteLockCopy() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety.Test",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();",
            "  final Lock readLock = lock.readLock();",
            "  final Lock writeLock = lock.writeLock();",
            "  @GuardedBy(\"lock\") boolean b = false;",
            "  void m() {",
            "    readLock.lock();",
            "    try {",
            "      b = true;",
            "    } finally {",
            "      readLock.unlock();",
            "    }",
            "  }",
            "  void n() {",
            "    writeLock.lock();",
            "    try {",
            "      b = true;",
            "    } finally {",
            "      writeLock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReadWriteLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();",
            "  @GuardedBy(\"lock\") boolean b = false;",
            "  void m() {",
            "    lock.readLock().lock();",
            "    try {",
            "      b = true;",
            "    } finally {",
            "      lock.readLock().unlock();",
            "    }",
            "  }",
            "  void n() {",
            "    lock.writeLock().lock();",
            "    try {",
            "      b = true;",
            "    } finally {",
            "      lock.writeLock().unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // Test that ReadWriteLocks are currently ignored.
  @Test
  public void testReadWriteLockIsIgnored() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "class Test {",
            "  final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();",
            "  @GuardedBy(\"lock\") boolean b = false;",
            "  void m() {",
            "    try {",
            "      b = true;",
            "    } finally {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInnerClass_enclosingClassLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  final Object mu = new Object();",
            "  @GuardedBy(\"mu\") boolean b = false;",
            "  private final class Baz {",
            "    public void m() {",
            "      synchronized (mu) {",
            "        n();",
            "      }",
            "    }",
            "    @GuardedBy(\"Test.this.mu\")",
            "    private void n() {",
            "      b = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // notice lexically enclosing owner, use NamedThis!
  @Test
  public void testInnerClass_thisLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  @GuardedBy(\"this\") boolean b = false;",
            "  private final class Baz {",
            "    private synchronized void n() {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'Test.this'",
            "      b = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnonymousClass() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  @GuardedBy(\"this\") boolean b = false;",
            "  private synchronized void n() {",
            "    b = true;",
            "    new Object() {",
            "      void m() {",
            "        // BUG: Diagnostic contains:",
            "        // should be guarded by 'Test.this'",
            "        b = true;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInheritedLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  final Object lock = new Object();",
            "}",
            "class B extends A {",
            "  @GuardedBy(\"lock\") boolean b = false;",
            "  void m() {",
            "    synchronized (lock) {",
            "      b = true;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testEnclosingSuperAccess() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  final Object lock = new Object();",
            "  @GuardedBy(\"lock\") boolean flag = false;",
            "}",
            "class B extends A {",
            "  void m() {",
            "    new Object() {",
            "      @GuardedBy(\"lock\")",
            "      void n() {",
            "        flag = true;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuperAccess_this() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  final Object lock = new Object();",
            "  @GuardedBy(\"this\") boolean flag = false;",
            "}",
            "class B extends A {",
            "  synchronized void m() {",
            "    flag = true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuperAccess_lock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  final Object lock = new Object();",
            "  @GuardedBy(\"lock\") boolean flag = false;",
            "}",
            "class B extends A {",
            "  void m() {",
            "    synchronized (lock) {",
            "      flag = true;",
            "    }",
            "    synchronized (this.lock) {",
            "      flag = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuperAccess_staticLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "}",
            "class B extends A {",
            "  void m() {",
            "    synchronized (A.lock) {",
            "      flag = true;",
            "    }",
            "    synchronized (B.lock) {",
            "      flag = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOtherClass_bad_staticLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "}",
            "class B {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "  void m() {",
            "    synchronized (B.lock) {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'A.lock'",
            "      A.flag = true;",
            "    }",
            "    synchronized (A.lock) {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'B.lock'",
            "      B.flag = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOtherClass_bad_staticLock_alsoSub() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "}",
            "class B extends A {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "  void m() {",
            "    synchronized (B.lock) {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'A.lock'",
            "      A.flag = true;",
            "    }",
            "    synchronized (A.lock) {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'B.lock'",
            "      B.flag = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOtherClass_staticLock() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class A {",
            "  static final Object lock = new Object();",
            "  @GuardedBy(\"lock\") static boolean flag = false;",
            "}",
            "class B {",
            "  void m() {",
            "    synchronized (A.lock) {",
            "      A.flag = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceAccess_instanceGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class InstanceAccess_InstanceGuard {",
            "  class A {",
            "    final Object lock = new Object();",
            "    @GuardedBy(\"lock\")",
            "    int x;",
            "  }",
            "",
            "class B extends A {",
            "  void m() {",
            "    synchronized (this.lock) {",
            "      this.x++;",
            "    }",
            "    // BUG: Diagnostic contains:",
            "    // should be guarded by 'this.lock'",
            "    this.x++;",
            "  }",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void instanceAccess_lexicalGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class InstanceAccess_LexicalGuard {",
            "  class Outer {",
            "    final Object lock = new Object();",
            "    class Inner {",
            "      @GuardedBy(\"lock\")",
            "      int x;",
            "      void m() {",
            "        synchronized (Outer.this.lock) {",
            "          this.x++;",
            "        }",
            "        // BUG: Diagnostic contains:",
            "        // should be guarded by 'Outer.this.lock'",
            "        this.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lexicalAccess_instanceGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class LexicalAccess_InstanceGuard {",
            "  class Outer {",
            "    final Object lock = new Object();",
            "    @GuardedBy(\"lock\")",
            "    int x;",
            "    class Inner {",
            "      void m() {",
            "        synchronized (Outer.this.lock) {",
            "          Outer.this.x++;",
            "        }",
            "        // BUG: Diagnostic contains:",
            "        // should be guarded by 'Outer.this.lock'",
            "        Outer.this.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lexicalAccess_lexicalGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class LexicalAccess_LexicalGuard {",
            "  class Outer {",
            "    final Object lock = new Object();",
            "    class Inner {",
            "      @GuardedBy(\"lock\")",
            "      int x;",
            "      class InnerMost {",
            "        void m() {",
            "          synchronized (Outer.this.lock) {",
            "            Inner.this.x++;",
            "          }",
            "          // BUG: Diagnostic contains:",
            "          // should be guarded by 'Outer.this.lock'",
            "          Inner.this.x++;",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceAccess_thisGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class InstanceAccess_ThisGuard {",
            "  class A {",
            "    @GuardedBy(\"this\")",
            "    int x;",
            "  }",
            "  class B extends A {",
            "    void m() {",
            "      synchronized (this) {",
            "        this.x++;",
            "      }",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'this'",
            "      this.x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceAccess_namedThisGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class InstanceAccess_NamedThisGuard {",
            "  class Outer {",
            "    class Inner {",
            "      @GuardedBy(\"Outer.this\")",
            "      int x;",
            "      void m() {",
            "        synchronized (Outer.this) {",
            "          x++;",
            "        }",
            "        // BUG: Diagnostic contains:",
            "      // should be guarded by 'Outer.this'",
            "        x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lexicalAccess_thisGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class LexicalAccess_ThisGuard {",
            "  class Outer {",
            "    @GuardedBy(\"this\")",
            "    int x;",
            "    class Inner {",
            "      void m() {",
            "        synchronized (Outer.this) {",
            "          Outer.this.x++;",
            "        }",
            "        // BUG: Diagnostic contains:",
            "      // should be guarded by 'Outer.this'",
            "        Outer.this.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lexicalAccess_namedThisGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class LexicalAccess_NamedThisGuard {",
            "  class Outer {",
            "    class Inner {",
            "      @GuardedBy(\"Outer.this\")",
            "      int x;",
            "      class InnerMost {",
            "        void m() {",
            "          synchronized (Outer.this) {",
            "            Inner.this.x++;",
            "          }",
            "          // BUG: Diagnostic contains:",
            "          // should be guarded by 'Outer.this'",
            "          Inner.this.x++;",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // Test that the analysis doesn't crash on lock expressions it doesn't recognize.
  // Note: there's currently no way to use @GuardedBy to specify that the guard is a specific array
  // element.
  @Test
  public void complexLockExpression() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class ComplexLockExpression {",
            "  final Object[] xs = {};",
            "  final int[] ys = {};",
            "  void m(int i) {",
            "    synchronized (xs[i]) {",
            "      ys[i]++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // TODO(user): make the diagnostic comprehensible...
  @Test
  public void wrongInnerClassInstance() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class WrongInnerClassInstance {",
            "  final Object lock = new Object();",
            "  class Inner {",
            "    @GuardedBy(\"lock\") int x = 0;",
            "    void m(Inner i) {",
            "      synchronized (WrongInnerClassInstance.this.lock) {",
            "        // BUG: Diagnostic contains:",
            "        // should be guarded by 'WrongInnerClassInstance.this.lock'; instead found:"
            + " 'WrongInnerClassInstance.this.lock'",
            "        i.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // (This currently passes because the analysis ignores try-with-resources, not because it
  // understands why this example is safe.)
  @Ignore // TODO(user): support try-with-resources
  @Test
  public void tryWithResources() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock;",
            "  @GuardedBy(\"lock\")",
            "  int x;",
            "  static class LockCloser implements AutoCloseable {",
            "    Lock lock;",
            "    LockCloser(Lock lock) {",
            "      this.lock = lock;",
            "      this.lock.lock();",
            "    }",
            "    @Override",
            "    public void close() throws Exception {",
            "      lock.unlock();",
            "    }",
            "  }",
            "  void m() throws Exception {",
            "    try (LockCloser _ = new LockCloser(lock)) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // Test that the contents of try-with-resources are ignored (for now).
  // TODO(user): support try-with-resources
  @Test
  public void tryWithResourcesAreUnsupported() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  Lock lock;",
            "  @GuardedBy(\"lock\")",
            "  int x;",
            "  void m(AutoCloseable c) throws Exception {",
            "    try (AutoCloseable unused = c) {",
            "      x++;  // should be an error!",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLexicalScopingExampleOne() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Transaction {",
            "  @GuardedBy(\"this\")",
            "  int x;",
            "  interface Handler {",
            "    void apply();",
            "  }",
            "  public void handle() {",
            "    runHandler(new Handler() {",
            "      public void apply() {",
            "        // BUG: Diagnostic contains:",
            "        // should be guarded by 'Transaction.this'",
            "        x++;",
            "      }",
            "    });",
            "  }",
            "  private synchronized void runHandler(Handler handler) {",
            "    handler.apply();",
            "  }",
            "}")
        .doTest();
  }

  // TODO(user): allowing @GuardedBy on overridden methods is unsound.
  @Test
  public void testLexicalScopingExampleTwo() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Transaction {",
            "  @GuardedBy(\"this\")",
            "  int x;",
            "  interface Handler {",
            "    void apply();",
            "  }",
            "  public void handle() {",
            "    runHandler(new Handler() {",
            "      @GuardedBy(\"Transaction.this\")",
            "      public void apply() {",
            "        x++;",
            "      }",
            "    });",
            "  }",
            "  private synchronized void runHandler(Handler handler) {",
            "    // This isn't safe...",
            "    handler.apply();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAliasing() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Names {",
            "  @GuardedBy(\"this\")",
            "  List<String> names = new ArrayList<>();",
            "  public void addName(String name) {",
            "    List<String> copyOfNames;",
            "    synchronized (this) {",
            "      copyOfNames = names;  // OK: access of 'names' guarded by 'this'",
            "    }",
            "    copyOfNames.add(name);  // should be an error: this access is not thread-safe!",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMonitorGuard() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.common.util.concurrent.Monitor;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  final Monitor monitor = new Monitor();",
            "  @GuardedBy(\"monitor\") int x;",
            "  final Monitor.Guard guard = new Monitor.Guard(monitor) {",
            "    @Override public boolean isSatisfied() {",
            "      x++;",
            "      return true;",
            "    }",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void testSemaphore() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.Semaphore;",
            "class Test {",
            "  final Semaphore semaphore = null;",
            "  @GuardedBy(\"semaphore\")",
            "  int x;",
            "  void m() throws InterruptedException {",
            "    semaphore.acquire();",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.semaphore'",
            "    x++;",
            "    try {",
            "      x++;",
            "    } finally {",
            "      semaphore.release();",
            "    }",
            "    // BUG: Diagnostic contains:",
            "    // access should be guarded by 'this.semaphore'",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void synchronizedOnLockMethod_negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            // do not remove, regression test for a bug when RWL is on the classpath
            "import java.util.concurrent.locks.ReadWriteLock;",
            "class Test {",
            "  Object lock() { return null; }",
            "  @GuardedBy(\"lock()\")",
            "  int x;",
            "  void m() {",
            "    synchronized (lock()) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressLocalVariable() throws Exception {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  final Lock lock = null;",
            "  @GuardedBy(\"lock\")",
            "  int x;",
            "  void m() {",
            "    @SuppressWarnings(\"GuardedBy\")",
            "    int z = x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void serializable() throws IOException {
    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(new GuardedByChecker());
  }
}
