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

/** Tests for {@link JodaWithDurationAddedLong}. */
@RunWith(JUnit4.class)
public class JodaWithDurationAddedLongTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaWithDurationAddedLong.class, getClass());

  // Instant

  @Test
  public void instantWithDurationAddedDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Instant NOW = Instant.now();",
            "  private static final Instant A = NOW.withDurationAdded(Duration.millis(42), 1);",
            "  private static final Instant B =",
            "      // BUG: Diagnostic contains: NOW.plus(Duration.millis(42));",
            "      NOW.withDurationAdded(Duration.millis(42).getMillis(), 1);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithDurationAddedLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Instant NOW = Instant.now();",
            "  // BUG: Diagnostic contains: NOW.withDurationAdded(Duration.millis(42), 2);",
            "  private static final Instant A = NOW.withDurationAdded(42, 2);",
            "  private static final Instant B =",
            "      // BUG: Diagnostic contains: NOW.withDurationAdded(Duration.millis(42), 2);",
            "      NOW.withDurationAdded(Duration.millis(42).getMillis(), 2);",
            "",
            "  // BUG: Diagnostic contains: NOW.plus(Duration.millis(42));",
            "  private static final Instant PLUS = NOW.withDurationAdded(42, 1);",
            "  // BUG: Diagnostic contains: NOW;",
            "  private static final Instant ZERO = NOW.withDurationAdded(42, 0);",
            "  // BUG: Diagnostic contains: NOW.minus(Duration.millis(42));",
            "  private static final Instant MINUS = NOW.withDurationAdded(42, -1);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithDurationAddedLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Instant A = Instant.now().withDurationAdded(42, 1);",
            "}")
        .doTest();
  }

  // Duration

  @Test
  public void durationWithDurationAddedDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static org.joda.time.Duration.ZERO;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration A = ZERO.withDurationAdded(Duration.millis(42), 1);",
            "  private static final Duration B =",
            "      // BUG: Diagnostic contains: ZERO.plus(Duration.millis(42));",
            "      ZERO.withDurationAdded(Duration.millis(42).getMillis(), 1);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithDurationAddedLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static org.joda.time.Duration.ZERO;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: ZERO.withDurationAdded(Duration.millis(42), 2);",
            "  private static final Duration A = ZERO.withDurationAdded(42, 2);",
            "  private static final Duration B =",
            "      // BUG: Diagnostic contains: ZERO.withDurationAdded(Duration.millis(42), 2);",
            "      ZERO.withDurationAdded(Duration.millis(42).getMillis(), 2);",
            "",
            "  // BUG: Diagnostic contains: ZERO.plus(Duration.millis(42));",
            "  private static final Duration PLUS = ZERO.withDurationAdded(42, 1);",
            "  // BUG: Diagnostic contains: ZERO;",
            "  private static final Duration ZEROX = ZERO.withDurationAdded(42, 0);",
            "  // BUG: Diagnostic contains: ZERO.minus(Duration.millis(42));",
            "  private static final Duration MINUS = ZERO.withDurationAdded(42, -1);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithDurationAddedLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "import static org.joda.time.Duration.ZERO;",
            "public class TestClass {",
            "  private static final Duration A = ZERO.withDurationAdded(42, 1);",
            "}")
        .doTest();
  }

  // DateTime

  @Test
  public void dateTimeWithDurationAddedDuration() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "import org.joda.time.DateTime;",
            "public class TestClass {",
            "  private static final DateTime NOW = DateTime.now();",
            "  private static final DateTime A = NOW.withDurationAdded(Duration.millis(42), 1);",
            "  private static final DateTime B =",
            "      // BUG: Diagnostic contains: NOW.plus(Duration.millis(42));",
            "      NOW.withDurationAdded(Duration.millis(42).getMillis(), 1);",
            "}")
        .doTest();
  }

  @Test
  public void dateTimeWithDurationAddedLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTime;",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final DateTime NOW = DateTime.now();",
            "  // BUG: Diagnostic contains: NOW.withDurationAdded(Duration.millis(42), 2);",
            "  private static final DateTime A = NOW.withDurationAdded(42, 2);",
            "  private static final DateTime B =",
            "      // BUG: Diagnostic contains: NOW.withDurationAdded(Duration.millis(42), 2);",
            "      NOW.withDurationAdded(Duration.millis(42).getMillis(), 2);",
            "",
            "  // BUG: Diagnostic contains: NOW.plus(Duration.millis(42));",
            "  private static final DateTime PLUS = NOW.withDurationAdded(42, 1);",
            "  // BUG: Diagnostic contains: NOW;",
            "  private static final DateTime ZERO = NOW.withDurationAdded(42, 0);",
            "  // BUG: Diagnostic contains: NOW.minus(Duration.millis(42));",
            "  private static final DateTime MINUS = NOW.withDurationAdded(42, -1);",
            "}")
        .doTest();
  }

  @Test
  public void dateTimeWithDurationAddedLong_insideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final DateTime A = DateTime.now().withDurationAdded(42, 1);",
            "}")
        .doTest();
  }
}
