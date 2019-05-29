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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.CompilationTestHelper;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link InstantTemporalUnit}. */
@RunWith(JUnit4.class)
public final class InstantTemporalUnitTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InstantTemporalUnit.class, getClass());

  @Test
  public void invalidTemporalUnitsMatchesEnumeratedList() throws Exception {
    // This list comes from the Instant javadocs, but the checker builds the list based on the
    // definition in Instant.isSupported(): unit.isTimeBased() || unit == DAYS;
    assertThat(InstantTemporalUnit.INVALID_TEMPORAL_UNITS)
        .containsExactlyElementsIn(
            EnumSet.complementOf(
                EnumSet.of(
                    ChronoUnit.NANOS,
                    ChronoUnit.MICROS,
                    ChronoUnit.MILLIS,
                    ChronoUnit.SECONDS,
                    ChronoUnit.MINUTES,
                    ChronoUnit.HOURS,
                    ChronoUnit.HALF_DAYS,
                    ChronoUnit.DAYS)));
  }

  @Test
  public void instantPlus_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final Instant I0 = Instant.EPOCH.plus(1, ChronoUnit.DAYS);",
            "  private static final Instant I1 = Instant.EPOCH.plus(1, ChronoUnit.HALF_DAYS);",
            "  private static final Instant I2 = Instant.EPOCH.plus(1, ChronoUnit.HOURS);",
            "  private static final Instant I3 = Instant.EPOCH.plus(1, ChronoUnit.MICROS);",
            "  private static final Instant I4 = Instant.EPOCH.plus(1, ChronoUnit.MILLIS);",
            "  private static final Instant I5 = Instant.EPOCH.plus(1, ChronoUnit.MINUTES);",
            "  private static final Instant I6 = Instant.EPOCH.plus(1, ChronoUnit.NANOS);",
            "  private static final Instant I7 = Instant.EPOCH.plus(1, ChronoUnit.SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void instantPlus_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I0 = Instant.EPOCH.plus(1, ChronoUnit.CENTURIES);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I1 = Instant.EPOCH.plus(1, ChronoUnit.DECADES);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I2 = Instant.EPOCH.plus(1, ChronoUnit.ERAS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I3 = Instant.EPOCH.plus(1, ChronoUnit.FOREVER);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I4 = Instant.EPOCH.plus(1, ChronoUnit.MILLENNIA);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I5 = Instant.EPOCH.plus(1, ChronoUnit.MONTHS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I6 = Instant.EPOCH.plus(1, ChronoUnit.WEEKS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I7 = Instant.EPOCH.plus(1, ChronoUnit.YEARS);",
            "}")
        .doTest();
  }

  @Test
  public void instantMinus_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final Instant I0 = Instant.EPOCH.minus(1, ChronoUnit.DAYS);",
            "  private static final Instant I1 = Instant.EPOCH.minus(1, ChronoUnit.HALF_DAYS);",
            "  private static final Instant I2 = Instant.EPOCH.minus(1, ChronoUnit.HOURS);",
            "  private static final Instant I3 = Instant.EPOCH.minus(1, ChronoUnit.MICROS);",
            "  private static final Instant I4 = Instant.EPOCH.minus(1, ChronoUnit.MILLIS);",
            "  private static final Instant I5 = Instant.EPOCH.minus(1, ChronoUnit.MINUTES);",
            "  private static final Instant I6 = Instant.EPOCH.minus(1, ChronoUnit.NANOS);",
            "  private static final Instant I7 = Instant.EPOCH.minus(1, ChronoUnit.SECONDS);",
            "}")
        .doTest();
  }

  @Test
  public void instantMinus_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I0 = Instant.EPOCH.minus(1, ChronoUnit.CENTURIES);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I1 = Instant.EPOCH.minus(1, ChronoUnit.DECADES);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I2 = Instant.EPOCH.minus(1, ChronoUnit.ERAS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I3 = Instant.EPOCH.minus(1, ChronoUnit.FOREVER);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I4 = Instant.EPOCH.minus(1, ChronoUnit.MILLENNIA);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I5 = Instant.EPOCH.minus(1, ChronoUnit.MONTHS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I6 = Instant.EPOCH.minus(1, ChronoUnit.WEEKS);",
            "  // BUG: Diagnostic contains: InstantTemporalUnit",
            "  private static final Instant I7 = Instant.EPOCH.minus(1, ChronoUnit.YEARS);",
            "}")
        .doTest();
  }
}
