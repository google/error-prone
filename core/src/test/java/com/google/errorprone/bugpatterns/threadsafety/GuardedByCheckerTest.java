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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link GuardedByChecker}Test */
@RunWith(JUnit4.class)
public class GuardedByCheckerTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(GuardedByChecker.class, getClass());
  }

  @Test
  public void testLocked() {
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

  /** "static synchronized method() { ... }" == "synchronized (MyClass.class) { ... }" */
  @Test
  public void testStaticLocked() {
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
  public void testMonitor() {
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
  public void testWrongLock() {
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
  public void testGuardedStaticFieldAccess_1() {
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
  public void testGuardedStaticFieldAccess_2() {
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
  public void testGuardedStaticFieldAccess_3() {
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
  public void testGuardedStaticFieldAccess_EnclosingClass() {
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
  public void testBadStaticFieldAccess() {
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
  public void testBadGuard() {
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
  public void testErrorProneAnnotation() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.concurrent.GuardedBy;",
            "class Test {",
            "  // BUG: Diagnostic contains: Invalid @GuardedBy expression",
            "  @GuardedBy(\"foo\") int y;",
            "}")
        .doTest();
  }

  @Test
  public void testUnheldInstanceGuard() {
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
  public void testUnheldItselfGuard() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Itself {",
            "  @GuardedBy(\"itself\")",
            "  int x;",
            "  void incrementX() {",
            "    // BUG: Diagnostic contains:",
            "    // should be guarded by 'this.x'",
            "    x++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void i541() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import java.util.List;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Itself {",
            "  @GuardedBy(\"itself\")",
            "  List<String> xs;",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    // should be guarded by 'this.xs'",
            "    this.xs.add(\"\");",
            "    synchronized (this.xs) { this.xs.add(\"\"); }",
            "    synchronized (this.xs) { xs.add(\"\"); }",
            "    synchronized (xs) { this.xs.add(\"\"); }",
            "    synchronized (xs) { xs.add(\"\"); }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCtor() {
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
  public void testBadGuardMethodAccess() {
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
  public void testTransitiveGuardMethodAccess() {
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

  @Ignore // TODO(cushon): support read/write lock copies
  @Test
  public void testReadWriteLockCopy() {
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
  public void testReadWriteLock() {
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
  public void testReadWriteLockIsIgnored() {
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
  public void testInnerClass_enclosingClassLock() {
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
  public void testInnerClass_thisLock() {
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
  public void testAnonymousClass() {
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
  public void testInheritedLock() {
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
  public void testEnclosingSuperAccess() {
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
  public void testSuperAccess_this() {
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
  public void testSuperAccess_lock() {
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
  public void testSuperAccess_staticLock() {
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
  public void testOtherClass_bad_staticLock() {
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
  public void testOtherClass_bad_staticLock_alsoSub() {
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
  public void testOtherClass_staticLock() {
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
  public void instanceAccess_instanceGuard() {
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
  public void instanceAccess_lexicalGuard() {
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
  public void lexicalAccess_instanceGuard() {
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
  public void lexicalAccess_lexicalGuard() {
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
  public void instanceAccess_thisGuard() {
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
  public void instanceAccess_namedThisGuard() {
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
  public void lexicalAccess_thisGuard() {
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
            "        // should be guarded by 'Outer.this'",
            "        Outer.this.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lexicalAccess_namedThisGuard() {
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
  public void complexLockExpression() {
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

  @Test
  public void wrongInnerClassInstance() {
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
            "        // guarded by 'lock' in enclosing instance"
                + " 'threadsafety.WrongInnerClassInstance' of 'i'",
            "        i.x++;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // (This currently passes because the analysis ignores try-with-resources, not because it
  // understands why this example is safe.)
  @Ignore // TODO(cushon): support try-with-resources
  @Test
  public void tryWithResources() {
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

  // Test that the contents of try-with-resources block are ignored (for now), but the catch and
  // finally blocks are checked.
  // TODO(cushon): support try-with-resources block.
  @Test
  public void tryWithResourcesAreNotFullyUnsupported() {
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
            "    } catch (Exception e) {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'this.lock'",
            "      x++;",
            "      throw e;",
            "    } finally {",
            "      // BUG: Diagnostic contains:",
            "      // should be guarded by 'this.lock'",
            "      x++;",
            "    }",
            "  }",
            "",
            "  void n(AutoCloseable c) throws Exception {",
            "    lock.lock();",
            "    try (AutoCloseable unused = c) {",
            "    } catch (Exception e) {",
            "      x++;",
            "    } finally {",
            "      lock.unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testLexicalScopingExampleOne() {
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

  // TODO(cushon): allowing @GuardedBy on overridden methods is unsound.
  @Test
  public void testLexicalScopingExampleTwo() {
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
  public void testAliasing() {
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
  public void testMonitorGuard() {
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
  public void testSemaphore() {
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
  public void synchronizedOnLockMethod_negative() {
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
  public void suppressLocalVariable() {
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

  // regression test for issue 387
  @Test
  public void enclosingBlockScope() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public final Object mu = new Object();",
            "  @GuardedBy(\"mu\") int x = 1;",
            "  {",
            "    new Object() {",
            "      void f() {",
            "        synchronized (mu) {",
            "          x++;",
            "        }",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/26834754") // fix resolution of qualified type names
  @Test
  public void qualifiedType() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static class Inner {",
            "    public static final Object mu = new Object();",
            "  }",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public final Object mu = new Object();",
            "  @GuardedBy(\"lib.Lib.Inner.mu\") int x = 1;",
            "  void f() {",
            "    synchronized (lib.Lib.Inner.mu) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/26834754") // fix resolution of qualified type names
  @Test
  public void innerClassTypeQualifier() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static class Inner {",
            "    public static final Object mu = new Object();",
            "  }",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import lib.Lib;",
            "public class Test {",
            "  public final Object mu = new Object();",
            "  @GuardedBy(\"Lib.Inner.mu\") int x = 1;",
            "  void f() {",
            "    synchronized (Lib.Inner.mu) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceInitializersAreUnchecked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public final Object mu1 = new Object();",
            "  public final Object mu2 = new Object();",
            "  @GuardedBy(\"mu1\") int x = 1;",
            "  {",
            "    synchronized (mu2) {",
            "      x++;",
            "    }",
            "    synchronized (mu1) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void classInitializersAreUnchecked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public static final Object mu1 = new Object();",
            "  public static final Object mu2 = new Object();",
            "  @GuardedBy(\"mu1\") static int x = 1;",
            "  static {",
            "    synchronized (mu2) {",
            "      x++;",
            "    }",
            "    synchronized (mu1) {",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticFieldInitializersAreUnchecked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public static final Object mu = new Object();",
            "  @GuardedBy(\"mu\") static int x0 = 1;",
            "  static int x1 = x0++;",
            "}")
        .doTest();
  }

  @Test
  public void instanceFieldInitializersAreUnchecked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public final Object mu = new Object();",
            "  @GuardedBy(\"mu\") int x0 = 1;",
            "  int x1 = x0++;",
            "}")
        .doTest();
  }

  @Test
  public void innerClassMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  public final Object mu = new Object();",
            "  class Inner {",
            "    @GuardedBy(\"mu\") int x;",
            "    @GuardedBy(\"Test.this\") int y;",
            "  }",
            "  void f(Inner i) {",
            "    synchronized (mu) {",
            "      // BUG: Diagnostic contains:",
            "      // guarded by 'mu' in enclosing instance 'threadsafety.Test' of 'i'",
            "      i.x++;",
            "    }",
            "  }",
            "  synchronized void g(Inner i) {",
            "      // BUG: Diagnostic contains:",
            "      // guarded by enclosing instance 'threadsafety.Test' of 'i'",
            "    i.y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void innerClassMethod_classBoundary() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Outer.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Outer {",
            "  public final Object mu = new Object();",
            "  class Inner {",
            "    @GuardedBy(\"mu\") int x;",
            "    @GuardedBy(\"Outer.this\") int y;",
            "  }",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  void f() {",
            "    Outer a = new Outer();",
            "    Outer b = new Outer();",
            "    Outer.Inner ai = a.new Inner();",
            "    synchronized (b.mu) {",
            "      // BUG: Diagnostic contains:",
            "      // Access should be guarded by 'mu' in enclosing instance 'threadsafety.Outer'"
                + " of 'ai', which is not accessible in this scope; instead found: 'b.mu'",
            "      ai.x++;",
            "    }",
            "    synchronized (b) {",
            "      // BUG: Diagnostic contains:",
            "      // Access should be guarded by enclosing instance 'threadsafety.Outer' of 'ai',"
                + " which is not accessible in this scope; instead found: 'b'",
            "      ai.y++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void regression_b27686620() {
    compilationHelper
        .addSourceLines(
            "A.java", //
            "class A extends One {",
            "  void g() {}",
            "}")
        .addSourceLines(
            "B.java",
            "import javax.annotation.concurrent.GuardedBy;",
            "class One {",
            "  @GuardedBy(\"One.class\") static int x = 1;",
            "  static void f() { synchronized (One.class) { x++; } }",
            "}",
            "class Two {",
            "  @GuardedBy(\"Two.class\") static int x = 1;",
            "  static void f() { synchronized (Two.class) { x++; } }",
            "}")
        .addSourceLines(
            "C.java", //
            "class B extends Two {",
            "  void g() {}",
            "}")
        .doTest();
  }

  @Ignore // TODO(cushon): clean up existing instances and re-enable
  @Test
  public void qualifiedMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  @GuardedBy(\"this\") void f() {}",
            "  void main() {",
            "    // BUG: Diagnostic contains: 'this', which could not be resolved",
            "    new Test().f();",
            "    Test t = new Test();",
            "    // BUG: Diagnostic contains: guarded by 't'",
            "    t.f();",
            "  }",
            "}")
        .doTest();
  }

  // regression test for #426
  @Test
  public void noSuchMethod() {
    compilationHelper
        .addSourceLines(
            "Foo.java", //
            "public class Foo {}")
        .addSourceLines(
            "Test.java",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  Foo foo;",
            "  // BUG: Diagnostic contains: could not resolve guard",
            "  @GuardedBy(\"foo.get()\") Object o = null;",
            "}")
        .doTest();
  }

  // regression test for b/34251959
  @Test
  public void lambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  @GuardedBy(\"this\") int x;",
            "  synchronized void f() {",
            "    Runnable r = () -> {",
            "      // BUG: Diagnostic contains: should be guarded by 'this',",
            "      x++;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleLocks() {
    compilationHelper
        .addSourceLines(
            "GuardedBy.java", //
            "@interface GuardedBy {",
            "  String[] value() default {};",
            "}")
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  private final Object mu = new Object();",
            "  @GuardedBy({\"this\", \"mu\"}) int x;",
            "  void f() {",
            "    synchronized (this) {",
            "      synchronized (mu) {",
            "        x++;",
            "      }",
            "    }",
            "    synchronized (this) {",
            "      // BUG: Diagnostic contains: should be guarded by 'this.mu'",
            "      x++;",
            "    }",
            "    synchronized (mu) {",
            "      // BUG: Diagnostic contains: should be guarded by 'this'",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // Ensure sure outer instance handling doesn't accidentally include enclosing classes of
  // static member classes.
  @Test
  public void testStaticMemberClass_enclosingInstanceLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  final Object mu = new Object();",
            "  private static final class Baz {",
            "    // BUG: Diagnostic contains: could not resolve guard",
            "    @GuardedBy(\"mu\") int x;",
            "  }",
            "  public void m(Baz b) {",
            "    synchronized (mu) {",
            "      b.x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // Ensure sure outer instance handling doesn't accidentally include enclosing classes of
  // static member classes.
  @Test
  public void testStaticMemberClass_staticOuterClassLock() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "public class Test {",
            "  static final Object mu = new Object();",
            "  private static final class Baz {",
            "    @GuardedBy(\"mu\") int x;",
            "  }",
            "  public void m(Baz b) {",
            "    synchronized (mu) {",
            "      b.x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
