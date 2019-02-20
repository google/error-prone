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
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PeriodFrom}. */
@RunWith(JUnit4.class)
public class PeriodFromTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(PeriodFrom.class, getClass());

  @SuppressWarnings("PeriodFrom")
  @Test
  public void testFailures() {
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ZERO));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofNanos(1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofNanos(-1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofMillis(1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofMillis(-1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofSeconds(1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofSeconds(-1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofMinutes(1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofMinutes(-1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofDays(1)));
    assertThrows(DateTimeException.class, () -> Period.from(Duration.ofDays(-1)));

    TemporalAmount temporalAmount = Duration.ofDays(3);
    assertThrows(DateTimeException.class, () -> Period.from(temporalAmount));
  }

  @Test
  public void periodFrom() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.Period;",
            "import java.time.temporal.TemporalAmount;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: PeriodFrom",
            "  private final Period oneDay = Period.from(Duration.ofDays(1));",
            "  // BUG: Diagnostic contains: Period.ofDays(2)",
            "  private final Period twoDays = Period.from(Period.ofDays(2));",
            "  private final TemporalAmount temporalAmount = Duration.ofDays(3);",
            // don't trigger when it is not statically known to be a Duration/Period
            "  private final Duration threeDays = Duration.from(temporalAmount);",
            "}")
        .doTest();
  }
}
