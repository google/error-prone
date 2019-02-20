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

/** Tests for {@link JodaPlusMinusLong}. */
@RunWith(JUnit4.class)
public class JodaPlusMinusLongTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaPlusMinusLong.class, getClass());

  // Instant

  @Test
  public void instantPlusMinusDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Instant PLUS = Instant.now().plus(Duration.millis(42));",
            "  private static final Instant MINUS = Instant.now().minus(Duration.millis(42));",
            "}")
        .doTest();
  }

  @Test
  public void instantPlusMinusLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Duration D = Duration.ZERO;",
            "  // BUG: Diagnostic contains: Instant.now().plus(Duration.millis(42L));",
            "  private static final Instant PLUS = Instant.now().plus(42L);",
            "  // BUG: Diagnostic contains: Instant.now().plus(D);",
            "  private static final Instant PLUS2 = Instant.now().plus(D.getMillis());",
            "  // BUG: Diagnostic contains: Instant.now().minus(Duration.millis(42L));",
            "  private static final Instant MINUS = Instant.now().minus(42L);",
            "  // BUG: Diagnostic contains: Instant.now().minus(D);",
            "  private static final Instant MINUS2 = Instant.now().minus(D.getMillis());",
            "}")
        .doTest();
  }

  @Test
  public void instantPlusMinusLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Instant PLUS = Instant.now().plus(42L);",
            "  private static final Instant MINUS = Instant.now().minus(42L);",
            "}")
        .doTest();
  }

  // Duration

  @Test
  public void durationPlusMinusDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Duration PLUS = Duration.ZERO.plus(Duration.millis(42));",
            "  private static final Duration MINUS = Duration.ZERO.minus(Duration.millis(42));",
            "}")
        .doTest();
  }

  @Test
  public void durationPlusMinusLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration D = Duration.ZERO;",
            "  // BUG: Diagnostic contains: Duration.ZERO.plus(Duration.millis(42L));",
            "  private static final Duration PLUS = Duration.ZERO.plus(42L);",
            "  // BUG: Diagnostic contains: Duration.ZERO.plus(D);",
            "  private static final Duration PLUS2 = Duration.ZERO.plus(D.getMillis());",
            "  // BUG: Diagnostic contains: Duration.ZERO.minus(Duration.millis(42L));",
            "  private static final Duration MINUS = Duration.ZERO.minus(42L);",
            "  // BUG: Diagnostic contains: Duration.ZERO.minus(D);",
            "  private static final Duration MINUS2 = Duration.ZERO.minus(D.getMillis());",
            "}")
        .doTest();
  }

  @Test
  public void durationPlusMinusLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Duration PLUS = Duration.ZERO.plus(42L);",
            "  private static final Duration MINUS = Duration.ZERO.minus(42L);",
            "}")
        .doTest();
  }

  // DateTime

  @Test
  public void dateTimePlusMinusDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTime;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final DateTime PLUS = DateTime.now().plus(Duration.millis(42));",
            "  private static final DateTime MINUS = DateTime.now().minus(Duration.millis(42));",
            "}")
        .doTest();
  }

  @Test
  public void dateTimePlusMinusLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTime;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration D = Duration.ZERO;",
            "  // BUG: Diagnostic contains: DateTime.now().plus(Duration.millis(42L));",
            "  private static final DateTime PLUS = DateTime.now().plus(42L);",
            "  // BUG: Diagnostic contains: DateTime.now().plus(D);",
            "  private static final DateTime PLUS2 = DateTime.now().plus(D.getMillis());",
            "  // BUG: Diagnostic contains: DateTime.now().minus(Duration.millis(42L));",
            "  private static final DateTime MINUS = DateTime.now().minus(42L);",
            "  // BUG: Diagnostic contains: DateTime.now().minus(D);",
            "  private static final DateTime MINUS2 = DateTime.now().minus(D.getMillis());",
            "}")
        .doTest();
  }

  @Test
  public void dateTimePlusMinusLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final DateTime PLUS = DateTime.now().plus(42L);",
            "  private static final DateTime MINUS = DateTime.now().minus(42L);",
            "}")
        .doTest();
  }

  // DateMidnight

  @Test
  public void dateMidnightPlusMinusDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateMidnight;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final DateMidnight PLUS = ",
            "      DateMidnight.now().plus(Duration.millis(42));",
            "  private static final DateMidnight MINUS = ",
            "      DateMidnight.now().minus(Duration.millis(42));",
            "}")
        .doTest();
  }

  @Test
  public void dateMidnightPlusMinusLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateMidnight;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration D = Duration.ZERO;",
            "  // BUG: Diagnostic contains: DateMidnight.now().plus(Duration.millis(42L));",
            "  private static final DateMidnight PLUS = DateMidnight.now().plus(42L);",
            "  // BUG: Diagnostic contains: DateMidnight.now().plus(D);",
            "  private static final DateMidnight PLUS2 = DateMidnight.now().plus(D.getMillis());",
            "  // BUG: Diagnostic contains: DateMidnight.now().minus(Duration.millis(42L));",
            "  private static final DateMidnight MINUS = DateMidnight.now().minus(42L);",
            "  // BUG: Diagnostic contains: DateMidnight.now().minus(D);",
            "  private static final DateMidnight MINUS2 = DateMidnight.now().minus(D.getMillis());",
            "}")
        .doTest();
  }

  @Test
  public void dateMidnightPlusMinusLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final DateMidnight PLUS = DateMidnight.now().plus(42L);",
            "  private static final DateMidnight MINUS = DateMidnight.now().minus(42L);",
            "}")
        .doTest();
  }
}
