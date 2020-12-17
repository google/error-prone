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

/** Tests for {@link JodaConstructors}. */
@RunWith(JUnit4.class)
public class JodaConstructorsTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaConstructors.class, getClass());

  @Test
  public void durationStaticFactories() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration ONE_MILLI = Duration.millis(1);",
            "  private static final Duration ONE_SEC = Duration.standardSeconds(1);",
            "  private static final Duration ONE_MIN = Duration.standardMinutes(1);",
            "  private static final Duration ONE_HOUR = Duration.standardHours(1);",
            "  private static final Duration ONE_DAY = Duration.standardDays(1);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorObject() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration ONE_MILLI = new Duration(\"1\");",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntInt() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration INTERVAL = new Duration(42, 48);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration INTERVAL = new Duration(42, 48L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongInt() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration INTERVAL = new Duration(42L, 48);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration INTERVAL = new Duration(42L, 48L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Duration ONE_MILLI = Duration.millis(1);",
            "  private static final Duration ONE_MILLI = new Duration(1);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorInteger() {
    // TODO(kak): This really should be an error too :(
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration ONE_MILLI = new Duration(Integer.valueOf(42));",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Duration ONE_MILLI = Duration.millis(1L);",
            "  private static final Duration ONE_MILLI = new Duration(1L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitiveInsideJoda() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Duration ONE_MILLI = new Duration(1L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitiveNoImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  private static final org.joda.time.Duration ONE_MILLI = ",
            "      // BUG: Diagnostic contains: Did you mean 'org.joda.time.Duration.millis(1L);'",
            "      new org.joda.time.Duration(1L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLong() {
    // TODO(kak): This really should be an error too :(
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration ONE_MILLI = new Duration(Long.valueOf(1L));",
            "}")
        .doTest();
  }

  @Test
  public void instantConstructor() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Instant NOW = Instant.now();",
            "  private static final Instant NOW = new Instant();",
            "}")
        .doTest();
  }

  @Test
  public void instantConstructor_fqcn() {
    helper
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: NOW = org.joda.time.Instant.now();",
            "  private static final org.joda.time.Instant NOW = new org.joda.time.Instant();",
            "}")
        .doTest();
  }

  @Test
  public void dateTimeConstructors() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Chronology;",
            "import org.joda.time.chrono.GregorianChronology;",
            "import org.joda.time.DateTime;",
            "import org.joda.time.DateTimeZone;",
            "public class TestClass {",
            "  private static final DateTimeZone NYC = DateTimeZone.forID(\"America/New_York\");",
            "  private static final Chronology GREG = GregorianChronology.getInstance();",
            "  // BUG: Diagnostic contains: DateTime NOW = DateTime.now();",
            "  private static final DateTime NOW = new DateTime();",
            "  // BUG: Diagnostic contains: DateTime NOW_NYC = DateTime.now(NYC);",
            "  private static final DateTime NOW_NYC = new DateTime(NYC);",
            "  // BUG: Diagnostic contains: DateTime NOW_GREG = DateTime.now(GREG);",
            "  private static final DateTime NOW_GREG = new DateTime(GREG);",
            "}")
        .doTest();
  }
}
