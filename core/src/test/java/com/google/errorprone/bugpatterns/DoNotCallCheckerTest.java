/*
 * Copyright 2017 The Error Prone Authors.
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

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.util.RuntimeVersion;
import java.sql.Date;
import java.sql.Time;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DoNotCallChecker}Test */
@SuppressWarnings("DoNotCall")
@RunWith(JUnit4.class)
public class DoNotCallCheckerTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DoNotCallChecker.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "class Test {",
            "  @DoNotCall(\"satisfying explanation\") final void f() {}",
            "  @DoNotCall final void g() {}",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    // This method should not be called: satisfying explanation",
            "    f();",
            "    // BUG: Diagnostic contains:",
            "    // This method should not be called, see its documentation for details",
            "    g();",
            "    // BUG: Diagnostic contains:",
            "    Runnable r = this::g;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concreteFinal() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public class Test {",
            "  // BUG: Diagnostic contains: should be final",
            "  @DoNotCall public void f() {}",
            "  @DoNotCall public final void g() {}",
            "}")
        .doTest();
  }

  @Test
  public void requiredOverride() {
    testHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface A {",
            "  @DoNotCall public void f();",
            "}")
        .addSourceLines(
            "B.java",
            "public class B implements A {",
            "  // BUG: Diagnostic contains: overrides f in A which is annotated",
            "  @Override public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void annotatedOverride() {
    testHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface A {",
            "  @DoNotCall public void f();",
            "}")
        .addSourceLines(
            "B.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public class B implements A {",
            "  @DoNotCall @Override public final void f() {}",
            "}")
        .doTest();
  }

  // The interface tries to make Object#toString @DoNotCall, and because
  // the declaration in B is implicit it doesn't get checked.
  // In practice, making default Object methods @DoNotCall isn't super
  // useful - typically users interface with the interface directly
  // (e.g. Hasher) or there's an override that has unwanted behaviour (Localizable).
  // TODO(cushon): check class declarations for super-interfaces that do this?
  @Test
  public void interfaceRedeclaresObjectMethod() {
    testHelper
        .addSourceLines(
            "I.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public interface I {",
            "  @DoNotCall public String toString();",
            "}")
        .addSourceLines(
            "B.java", //
            "public class B implements I {",
            "}")
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  void f(B b) {",
            "    b.toString();",
            "    I i = b;",
            "    // BUG: Diagnostic contains:",
            "    i.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public final class Test {",
            "  @DoNotCall public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void privateMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public final class Test {",
            "  // BUG: Diagnostic contains: private method",
            "  @DoNotCall private void f() {}",
            "}")
        .doTest();
  }

  /** Test class containing a method annotated with @DNC. */
  public static class DNCTest {
    @DoNotCall
    public static void f() {}
  }

  @Test
  public void noDNConClasspath() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: This method should not be called",
            "    com.google.errorprone.bugpatterns.DoNotCallCheckerTest.DNCTest.f();",
            "  }",
            "}")
        .withClasspath(DNCTest.class, DoNotCallCheckerTest.class)
        .doTest();
  }

  @Test
  public void thirdParty() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.Assert;",
            "public class Test {",
            "  public void foo() {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    Assert.assertEquals(2.0, 2.0);",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    Assert.assertEquals(\"msg\", 2.0, 2.0);",
            // These are OK since they pass a tolerance
            "    Assert.assertEquals(2.0, 2.0, 0.01);",
            "    Assert.assertEquals(\"msg\", 2.0, 2.0, 0.01);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlDate_toInstant() {
    assertThrows(UnsupportedOperationException.class, () -> new Date(1234567890L).toInstant());
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Date;",
            "import java.time.Instant;",
            "public class TestClass {",
            "  public void badApis(Date date) {",
            "    // BUG: Diagnostic contains: toLocalDate()",
            "    Instant instant = date.toInstant();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlDate_timeGetters() {
    Date date = new Date(1234567890L);
    assertThrows(IllegalArgumentException.class, () -> date.getHours());
    assertThrows(IllegalArgumentException.class, () -> date.getMinutes());
    assertThrows(IllegalArgumentException.class, () -> date.getSeconds());
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Date;",
            "public class TestClass {",
            "  public void badApis(Date date) {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int hour = date.getHours();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int mins = date.getMinutes();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int secs = date.getSeconds();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlDate_timeSetters() {
    Date date = new Date(1234567890L);
    assertThrows(IllegalArgumentException.class, () -> date.setHours(1));
    assertThrows(IllegalArgumentException.class, () -> date.setMinutes(1));
    assertThrows(IllegalArgumentException.class, () -> date.setSeconds(1));
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Date;",
            "public class TestClass {",
            "  public void badApis(Date date) {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    date.setHours(1);",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    date.setMinutes(1);",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    date.setSeconds(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlDate_staticallyTypedAsJavaUtilDate() {
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void badApis() {",
            "    Date date = new java.sql.Date(1234567890L);",
            "    Instant instant = date.toInstant();",
            "    int hour = date.getHours();",
            "    int mins = date.getMinutes();",
            "    int secs = date.getSeconds();",
            "    date.setHours(1);",
            "    date.setMinutes(1);",
            "    date.setSeconds(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlTime_toInstant() {
    assertThrows(UnsupportedOperationException.class, () -> new Time(1234567890L).toInstant());
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Time;",
            "import java.time.Instant;",
            "public class TestClass {",
            "  public void badApis(Time time) {",
            "    // BUG: Diagnostic contains: toLocalTime()",
            "    Instant instant = time.toInstant();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlTime_dateGetters() {
    Time time = new Time(1234567890L);
    assertThrows(IllegalArgumentException.class, () -> time.getYear());
    assertThrows(IllegalArgumentException.class, () -> time.getMonth());
    assertThrows(IllegalArgumentException.class, () -> time.getDay());
    assertThrows(IllegalArgumentException.class, () -> time.getDate());
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Time;",
            "public class TestClass {",
            "  public void badApis(Time time) {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int year = time.getYear();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int month = time.getMonth();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int day = time.getDay();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    int date = time.getDate();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlTime_dateSetters() {
    Time time = new Time(1234567890L);
    assertThrows(IllegalArgumentException.class, () -> time.setYear(1));
    assertThrows(IllegalArgumentException.class, () -> time.setMonth(1));
    assertThrows(IllegalArgumentException.class, () -> time.setDate(1));
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.sql.Time;",
            "public class TestClass {",
            "  public void badApis(Time time) {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    time.setYear(1);",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    time.setMonth(1);",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    time.setDate(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaSqlTime_staticallyTypedAsJavaUtilDate() {
    testHelper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void badApis() {",
            "    Date time = new java.sql.Time(1234567890L);",
            "    Instant instant = time.toInstant();",
            "    int year = time.getYear();",
            "    int month = time.getMonth();",
            "    int date = time.getDate();",
            "    int day = time.getDay();",
            "    time.setYear(1);",
            "    time.setMonth(1);",
            "    time.setDate(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void readLock_newCondition() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> new ReentrantReadWriteLock().readLock().newCondition());
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.locks.ReentrantReadWriteLock;",
            "public class Test {",
            "  public void foo() {",
            "    ReentrantReadWriteLock.ReadLock lock = new ReentrantReadWriteLock().readLock();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    lock.newCondition();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void threadLocalRandom_setSeed() {
    assertThrows(
        UnsupportedOperationException.class, () -> ThreadLocalRandom.current().setSeed(42));
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.ThreadLocalRandom;",
            "public class Test {",
            "  public void foo() {",
            "    ThreadLocalRandom random = ThreadLocalRandom.current();",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    random.setSeed(42L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thread_stop() {
    // Thread.stop(Throwable) was removed in JDK11:
    //   https://bugs.openjdk.java.net/browse/JDK-8204243
    assumeTrue(RuntimeVersion.isAtMost10());
    testHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public void foo() {",
            "    // BUG: Diagnostic contains: DoNotCall",
            "    Thread.currentThread().stop(new Throwable());",
            "  }",
            "}")
        .doTest();
  }
}
