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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link PeriodTimeMath} */
@RunWith(JUnit4.class)
public class PeriodTimeMathTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(PeriodTimeMath.class, getClass());

  @SuppressWarnings("PeriodTimeMath")
  @Test
  public void testFailures() {
    Period p = Period.ZERO;
    assertThrows(DateTimeException.class, () -> p.plus(Duration.ZERO));
    assertThrows(DateTimeException.class, () -> p.minus(Duration.ZERO));
    assertThrows(DateTimeException.class, () -> p.plus(Duration.ofHours(48)));
    assertThrows(DateTimeException.class, () -> p.minus(Duration.ofHours(48)));
    assertThrows(DateTimeException.class, () -> p.plus(Duration.ofDays(2)));
  }

  @Test
  public void periodMath() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.Period;",
            "import java.time.temporal.TemporalAmount;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: PeriodTimeMath",
            "  private final Period zero = Period.ZERO.plus(Duration.ZERO);",
            "  private final Period oneDay = Period.ZERO.plus(Period.ofDays(1));",
            "  // BUG: Diagnostic contains: PeriodTimeMath",
            "  private final Period twoDays = Period.ZERO.minus(Duration.ZERO);",
            "  private final TemporalAmount temporalAmount = Duration.ofDays(3);",
            // don't trigger when it is not statically known to be a Duration/Period
            "  private final Period threeDays = Period.ZERO.plus(temporalAmount);",
            "}")
        .doTest();
  }

  @Test
  public void strictPeriodMath() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.Period;",
            "import java.time.temporal.TemporalAmount;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: PeriodTimeMath",
            "  private final Period zero = Period.ZERO.plus(Duration.ZERO);",
            "  private final Period oneDay = Period.ZERO.plus(Period.ofDays(1));",
            "  // BUG: Diagnostic contains: PeriodTimeMath",
            "  private final Period twoDays = Period.ZERO.minus(Duration.ZERO);",
            "  private final TemporalAmount temporalAmount = Duration.ofDays(3);",
            // In strict mode, trigger when it's not known to be a Period
            "  // BUG: Diagnostic contains: PeriodTimeMath",
            "  private final Period threeDays = Period.ZERO.plus(temporalAmount);",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:PeriodTimeMath:RequireStaticPeriodArgument"))
        .doTest();
  }
}
