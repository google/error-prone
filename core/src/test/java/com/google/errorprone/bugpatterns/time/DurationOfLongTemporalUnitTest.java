/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link DurationOfLongTemporalUnit}. */
@RunWith(JUnit4.class)
public class DurationOfLongTemporalUnitTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DurationOfLongTemporalUnit.class, getClass());

  @Test
  public void durationOf_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final Duration D0 = Duration.of(1, ChronoUnit.DAYS);",
            "  private static final Duration D1 = Duration.of(1, ChronoUnit.HALF_DAYS);",
            "  private static final Duration D2 = Duration.of(1, ChronoUnit.HOURS);",
            "  private static final Duration D3 = Duration.of(1, ChronoUnit.MICROS);",
            "  private static final Duration D4 = Duration.of(1, ChronoUnit.MILLIS);",
            "  private static final Duration D5 = Duration.of(1, ChronoUnit.MINUTES);",
            "  private static final Duration D6 = Duration.of(1, ChronoUnit.NANOS);",
            "  private static final Duration D7 = Duration.of(1, ChronoUnit.SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void durationOf_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D0 = Duration.of(1, ChronoUnit.CENTURIES);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D1 = Duration.of(1, ChronoUnit.DECADES);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D2 = Duration.of(1, ChronoUnit.ERAS);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D3 = Duration.of(1, ChronoUnit.FOREVER);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D4 = Duration.of(1, ChronoUnit.MILLENNIA);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D5 = Duration.of(1, ChronoUnit.MONTHS);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D6 = Duration.of(1, ChronoUnit.WEEKS);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D7 = Duration.of(1, ChronoUnit.YEARS);",
            "}")
        .doTest();
  }

  @Test
  public void durationOfStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.NANOS;",
            "import static java.time.temporal.ChronoUnit.SECONDS;",
            "import static java.time.temporal.ChronoUnit.YEARS;",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private static final Duration D1 = Duration.of(1, SECONDS);",
            "  private static final Duration D2 = Duration.of(1, NANOS);",
            "  // BUG: Diagnostic contains: DurationOfLongTemporalUnit",
            "  private static final Duration D3 = Duration.of(1, YEARS);",
            "}")
        .doTest();
  }

  @Test
  public void durationOfWithRandomTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.SECONDS;",
            "import static java.time.temporal.ChronoUnit.YEARS;",
            "import java.time.Duration;",
            "import java.time.temporal.TemporalUnit;",
            "import java.util.Random;",
            "public class TestClass {",
            "  private static final TemporalUnit random = ",
            "      new Random().nextBoolean() ? YEARS : SECONDS;",
            // Since we don't know at compile time what 'random' is, we can't flag this
            "  private static final Duration D1 = Duration.of(1, random);",
            "}")
        .doTest();
  }

  @Test
  public void durationOfWithAliasedTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.YEARS;",
            "import java.time.Duration;",
            "import java.time.temporal.Temporal;",
            "import java.time.temporal.TemporalUnit;",
            "public class TestClass {",
            "  private static final TemporalUnit SECONDS = YEARS;",
            // This really should be flagged, but isn't :(
            "  private static final Duration D1 = Duration.of(1, SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void durationOfWithCustomTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.temporal.Temporal;",
            "import java.time.temporal.TemporalUnit;",
            "public class TestClass {",
            "  private static class BadTemporalUnit implements TemporalUnit {",
            "    @Override",
            "    public long between(Temporal t1, Temporal t2) {",
            "      throw new AssertionError();",
            "    }",
            "    @Override",
            "    public <R extends Temporal> R addTo(R temporal, long amount) {",
            "      throw new AssertionError();",
            "    }",
            "    @Override",
            "    public boolean isTimeBased() {",
            "      throw new AssertionError();",
            "    }",
            "    @Override",
            "    public boolean isDateBased() {",
            "      throw new AssertionError();",
            "    }",
            "    @Override",
            "    public boolean isDurationEstimated() {",
            "      throw new AssertionError();",
            "    }",
            "    @Override",
            "    public Duration getDuration() {",
            "      throw new AssertionError();",
            "    }",
            "  }",
            "  private static final TemporalUnit MINUTES = new BadTemporalUnit();",
            "  private static final TemporalUnit SECONDS = new BadTemporalUnit();",
            // This really should be flagged, but isn't :(
            "  private static final Duration D1 = Duration.of(1, SECONDS);",
            "  private static final Duration D2 = Duration.of(1, MINUTES);",
            "}")
        .doTest();
  }
}
