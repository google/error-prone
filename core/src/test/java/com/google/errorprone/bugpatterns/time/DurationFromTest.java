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

import static org.junit.Assert.assertThrows;

import com.google.errorprone.CompilationTestHelper;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.time.temporal.UnsupportedTemporalTypeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DurationFrom}. */
@RunWith(JUnit4.class)
public class DurationFromTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DurationFrom.class, getClass());

  @SuppressWarnings("DurationFrom")
  @Test
  public void testFailures() {
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ZERO));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofDays(1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofDays(-1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofWeeks(1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofWeeks(-1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofMonths(1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofMonths(-1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofYears(1)));
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(Period.ofYears(-1)));

    TemporalAmount temporalAmount = Period.ofDays(3);
    assertThrows(UnsupportedTemporalTypeException.class, () -> Duration.from(temporalAmount));
  }

  @Test
  public void durationFrom() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.Period;",
            "import java.time.temporal.TemporalAmount;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: DurationFrom",
            "  private final Duration oneDay = Duration.from(Period.ofDays(1));",
            "  // BUG: Diagnostic contains: Duration.ofDays(2)",
            "  private final Duration twoDays = Duration.from(Duration.ofDays(2));",
            "  private final TemporalAmount temporalAmount = Duration.ofDays(3);",
            // don't trigger when it is not statically known to be a Duration/Period
            "  private final Duration threeDays = Duration.from(temporalAmount);",
            "}")
        .doTest();
  }
}
