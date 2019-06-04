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

/** Tests for {@link LocalDateTemporalAmount}. */
@RunWith(JUnit4.class)
public final class LocalDateTemporalAmountTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(LocalDateTemporalAmount.class, getClass());

  @Test
  public void localDatePlus_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.LocalDate;",
            "import java.time.Period;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  private static final Period PERIOD = Period.ofDays(1);",
            "  private static final LocalDate LD1 = LD.plus(PERIOD);",
            "  private static final LocalDate LD2 = LD.plus(1, ChronoUnit.DAYS);",
            "}")
        .doTest();
  }

  @Test
  public void localDatePlus_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  private static final Duration DURATION = Duration.ofDays(1);",
            "  // BUG: Diagnostic contains: LocalDateTemporalAmount",
            "  private static final LocalDate LD0 = LD.plus(DURATION);",
            "}")
        .doTest();
  }

  @Test
  public void localDatePlus_zero() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  // BUG: Diagnostic contains: LocalDateTemporalAmount",
            // This call technically doesn't throw, but we don't currently special case it
            "  private static final LocalDate LD0 = LD.plus(Duration.ZERO);",
            "}")
        .doTest();
  }

  @Test
  public void localDateMinus_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.LocalDate;",
            "import java.time.Period;",
            "import java.time.temporal.ChronoUnit;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  private static final Period PERIOD = Period.ofDays(1);",
            "  private static final LocalDate LD1 = LD.minus(PERIOD);",
            "  private static final LocalDate LD2 = LD.minus(1, ChronoUnit.DAYS);",
            "}")
        .doTest();
  }

  @Test
  public void localDateMinus_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  private static final Duration DURATION = Duration.ofDays(1);",
            "  // BUG: Diagnostic contains: LocalDateTemporalAmount",
            "  private static final LocalDate LD0 = LD.minus(DURATION);",
            "}")
        .doTest();
  }

  @Test
  public void localDateMinus_zero() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  private static final LocalDate LD = LocalDate.of(1985, 5, 31);",
            "  // BUG: Diagnostic contains: LocalDateTemporalAmount",
            // This call technically doesn't throw, but we don't currently special case it
            "  private static final LocalDate LD0 = LD.minus(Duration.ZERO);",
            "}")
        .doTest();
  }
}
