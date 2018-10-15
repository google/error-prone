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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DurationToLongTimeUnit}. */
@RunWith(JUnit4.class)
public class DurationToLongTimeUnitTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DurationToLongTimeUnit.class, getClass());

  @Test
  public void correspondingUnitsAreOk() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "import java.util.concurrent.Future;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  void javaTime(Future<String> f, java.time.Duration d) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(d.toNanos(), TimeUnit.NANOSECONDS);",
            "    f.get(d.toMillis(), TimeUnit.MILLISECONDS);",
            "    f.get(d.getSeconds(), TimeUnit.SECONDS);",
            "    f.get(d.toMinutes(), TimeUnit.MINUTES);",
            "    f.get(d.toHours(), TimeUnit.HOURS);",
            "    f.get(d.toDays(), TimeUnit.DAYS);",
            "  }",
            "  void javaTime(Future<String> f, java.time.Instant i) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(i.toEpochMilli(), TimeUnit.MILLISECONDS);",
            "    f.get(i.getEpochSecond(), TimeUnit.SECONDS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Duration d) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(d.getMillis(), TimeUnit.MILLISECONDS);",
            "    f.get(d.getStandardSeconds(), TimeUnit.SECONDS);",
            "    f.get(d.getStandardMinutes(), TimeUnit.MINUTES);",
            "    f.get(d.getStandardHours(), TimeUnit.HOURS);",
            "    f.get(d.getStandardDays(), TimeUnit.DAYS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Instant i) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(i.getMillis(), TimeUnit.MILLISECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Duration d) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(d.getSeconds(), TimeUnit.SECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Timestamp t) throws Exception {",
            "    f.get(42L, TimeUnit.SECONDS);",
            "    f.get(t.getSeconds(), TimeUnit.SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correspondingUnitsAreOk_staticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.DAYS;",
            "import static java.util.concurrent.TimeUnit.HOURS;",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "import static java.util.concurrent.TimeUnit.MINUTES;",
            "import static java.util.concurrent.TimeUnit.NANOSECONDS;",
            "import static java.util.concurrent.TimeUnit.SECONDS;",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "import java.util.concurrent.Future;",
            "public class TestClass {",
            "  void javaTime(Future<String> f, java.time.Duration d) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(d.toNanos(), NANOSECONDS);",
            "    f.get(d.toMillis(), MILLISECONDS);",
            "    f.get(d.getSeconds(), SECONDS);",
            "    f.get(d.toMinutes(), MINUTES);",
            "    f.get(d.toHours(), HOURS);",
            "    f.get(d.toDays(), DAYS);",
            "  }",
            "  void javaTime(Future<String> f, java.time.Instant i) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(i.toEpochMilli(), MILLISECONDS);",
            "    f.get(i.getEpochSecond(), SECONDS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Duration d) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(d.getMillis(), MILLISECONDS);",
            "    f.get(d.getStandardSeconds(), SECONDS);",
            "    f.get(d.getStandardMinutes(), MINUTES);",
            "    f.get(d.getStandardHours(), HOURS);",
            "    f.get(d.getStandardDays(), DAYS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Instant i) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(i.getMillis(), MILLISECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Duration d) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(d.getSeconds(), SECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Timestamp t) throws Exception {",
            "    f.get(42L, SECONDS);",
            "    f.get(t.getSeconds(), SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void conflictingUnitsFail() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "import java.util.concurrent.Future;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  void javaTime(Future<String> f, java.time.Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.toNanos(), TimeUnit.NANOSECONDS)",
            "    f.get(d.toNanos(), TimeUnit.MILLISECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.toMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(d.toMillis(), TimeUnit.NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getSeconds(), TimeUnit.MINUTES);",
            "    // BUG: Diagnostic contains: f.get(d.toMinutes(), TimeUnit.MINUTES)",
            "    f.get(d.toMinutes(), TimeUnit.SECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.toHours(), TimeUnit.HOURS)",
            "    f.get(d.toHours(), TimeUnit.DAYS);",
            "    // BUG: Diagnostic contains: f.get(d.toDays(), TimeUnit.DAYS)",
            "    f.get(d.toDays(), TimeUnit.HOURS);",
            "  }",
            "  void javaTime(Future<String> f, java.time.Instant i) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(i.toEpochMilli(), TimeUnit.MILLISECONDS)",
            "    f.get(i.toEpochMilli(), TimeUnit.NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(i.getEpochSecond(), TimeUnit.SECONDS)",
            "    f.get(i.getEpochSecond(), TimeUnit.MINUTES);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.getMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(d.getMillis(), TimeUnit.NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getStandardSeconds(), TimeUnit.MINUTES);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardMinutes(), TimeUnit.MINUTES)",
            "    f.get(d.getStandardMinutes(), TimeUnit.SECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardHours(), TimeUnit.HOURS)",
            "    f.get(d.getStandardHours(), TimeUnit.DAYS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardDays(), TimeUnit.DAYS)",
            "    f.get(d.getStandardDays(), TimeUnit.HOURS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Instant i) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(i.getMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(i.getMillis(), TimeUnit.NANOSECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getSeconds(), TimeUnit.MINUTES);",
            "  }",
            "  void protoTime(Future<String> f, Timestamp t) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(t.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(t.getSeconds(), TimeUnit.MINUTES);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void conflictingUnitsFail_staticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.DAYS;",
            "import static java.util.concurrent.TimeUnit.HOURS;",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "import static java.util.concurrent.TimeUnit.MINUTES;",
            "import static java.util.concurrent.TimeUnit.NANOSECONDS;",
            "import static java.util.concurrent.TimeUnit.SECONDS;",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "import java.util.concurrent.Future;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  void javaTime(Future<String> f, java.time.Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.toNanos(), TimeUnit.NANOSECONDS)",
            "    f.get(d.toNanos(), MILLISECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.toMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(d.toMillis(), NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getSeconds(), MINUTES);",
            "    // BUG: Diagnostic contains: f.get(d.toMinutes(), TimeUnit.MINUTES)",
            "    f.get(d.toMinutes(), SECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.toHours(), TimeUnit.HOURS)",
            "    f.get(d.toHours(), DAYS);",
            "    // BUG: Diagnostic contains: f.get(d.toDays(), TimeUnit.DAYS)",
            "    f.get(d.toDays(), HOURS);",
            "  }",
            "  void javaTime(Future<String> f, java.time.Instant i) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(i.toEpochMilli(), TimeUnit.MILLISECONDS)",
            "    f.get(i.toEpochMilli(), NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(i.getEpochSecond(), TimeUnit.SECONDS)",
            "    f.get(i.getEpochSecond(), MINUTES);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.getMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(d.getMillis(), NANOSECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getStandardSeconds(), MINUTES);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardMinutes(), TimeUnit.MINUTES)",
            "    f.get(d.getStandardMinutes(), SECONDS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardHours(), TimeUnit.HOURS)",
            "    f.get(d.getStandardHours(), DAYS);",
            "    // BUG: Diagnostic contains: f.get(d.getStandardDays(), TimeUnit.DAYS)",
            "    f.get(d.getStandardDays(), HOURS);",
            "  }",
            "  void jodaTime(Future<String> f, org.joda.time.Instant i) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(i.getMillis(), TimeUnit.MILLISECONDS)",
            "    f.get(i.getMillis(), NANOSECONDS);",
            "  }",
            "  void protoTime(Future<String> f, Duration d) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(d.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(d.getSeconds(), MINUTES);",
            "  }",
            "  void protoTime(Future<String> f, Timestamp t) throws Exception {",
            "    // BUG: Diagnostic contains: f.get(t.getSeconds(), TimeUnit.SECONDS)",
            "    f.get(t.getSeconds(), MINUTES);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonTimeUnitVariablesAreOk() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.protobuf.Duration;",
            "import com.google.protobuf.Timestamp;",
            "public class TestClass {",
            "  private enum MyEnum { DAYS };",
            "  void javaTime(java.time.Duration d) {",
            "    myMethod(d.toNanos(), MyEnum.DAYS);",
            "  }",
            "  void javaTime(java.time.Instant i) {",
            "    myMethod(i.toEpochMilli(), MyEnum.DAYS);",
            "  }",
            "  void jodaTime(org.joda.time.Duration d) {",
            "    myMethod(d.getMillis(), MyEnum.DAYS);",
            "  }",
            "  void jodaTime(org.joda.time.Instant i) {",
            "    myMethod(i.getMillis(), MyEnum.DAYS);",
            "  }",
            "  void protoTime(Duration d) {",
            "    myMethod(d.getSeconds(), MyEnum.DAYS);",
            "  }",
            "  void protoTime(Timestamp t) {",
            "    myMethod(t.getSeconds(), MyEnum.DAYS);",
            "  }",
            "  void myMethod(long value, MyEnum myEnum) {",
            "    // no op",
            "  }",
            "}")
        .doTest();
  }
}
