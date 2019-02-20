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

/** Tests for {@link PeriodGetTemporalUnit}. */
@RunWith(JUnit4.class)
public class PeriodGetTemporalUnitTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(PeriodGetTemporalUnit.class, getClass());

  @Test
  public void periodGetTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Period;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final long years = Period.ZERO.get(ChronoUnit.YEARS);",
            "  private static final long months = Period.ZERO.get(ChronoUnit.MONTHS);",
            "  private static final long days = Period.ZERO.get(ChronoUnit.DAYS);",
            "  // BUG: Diagnostic contains: PeriodGetTemporalUnit",
            "  private static final long seconds = Period.ZERO.get(ChronoUnit.SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void periodGetTemporalUnitStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.DAYS;",
            "import static java.time.temporal.ChronoUnit.MONTHS;",
            "import static java.time.temporal.ChronoUnit.SECONDS;",
            "import static java.time.temporal.ChronoUnit.YEARS;",
            "import java.time.Period;",
            "public class TestClass {",
            "  private static final long years = Period.ZERO.get(YEARS);",
            "  private static final long months = Period.ZERO.get(MONTHS);",
            "  private static final long days = Period.ZERO.get(DAYS);",
            "  // BUG: Diagnostic contains: PeriodGetTemporalUnit",
            "  private static final long seconds = Period.ZERO.get(SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void periodGetWithRandomTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.DAYS;",
            "import static java.time.temporal.ChronoUnit.SECONDS;",
            "import java.time.Period;",
            "import java.time.temporal.TemporalUnit;",
            "import java.util.Random;",
            "public class TestClass {",
            "  private final TemporalUnit random = new Random().nextBoolean() ? DAYS : SECONDS;",
            // Since we don't know at compile time what 'random' is, we can't flag this
            "  private final long mightWork = Period.ZERO.get(random);",
            "}")
        .doTest();
  }

  @Test
  public void periodGetWithAliasedTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoUnit.SECONDS;",
            "import java.time.Period;",
            "import java.time.temporal.Temporal;",
            "import java.time.temporal.TemporalUnit;",
            "public class TestClass {",
            "  private static final TemporalUnit DAYS = SECONDS;",
            // This really should be flagged, but isn't :(
            "  private static final long seconds = Period.ZERO.get(DAYS);",
            "}")
        .doTest();
  }

  @Test
  public void periodGetWithCustomTemporalUnit() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.Period;",
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
            "  private static final TemporalUnit SECONDS = new BadTemporalUnit();",
            "  private static final TemporalUnit DAYS = new BadTemporalUnit();",
            // This really should be flagged, but isn't :(
            "  private static final long seconds = Period.ZERO.get(DAYS);",
            "  // BUG: Diagnostic contains: PeriodGetTemporalUnit",
            "  private static final long days = Period.ZERO.get(SECONDS);",
            "}")
        .doTest();
  }
}
