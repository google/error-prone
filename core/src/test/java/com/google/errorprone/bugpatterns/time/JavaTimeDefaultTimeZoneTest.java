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

/** Tests for {@link JavaTimeDefaultTimeZone}. */
@RunWith(JUnit4.class)
public class JavaTimeDefaultTimeZoneTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JavaTimeDefaultTimeZone.class, getClass())
          .expectErrorMessage("REPLACEME", s -> s.contains("systemDefault()"));

  @Test
  public void clock() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.Clock;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  Clock clock = Clock.systemDefaultZone();",
            "  Clock clockWithZone = Clock.system(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void localDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  LocalDate now = LocalDate.now();",
            "  LocalDate nowWithZone = LocalDate.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void localTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.LocalTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  LocalTime now = LocalTime.now();",
            "  LocalTime nowWithZone = LocalTime.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void localDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.LocalDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  LocalDateTime now = LocalDateTime.now();",
            "  LocalDateTime nowWithZone = LocalDateTime.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void monthDay() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.MonthDay;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  MonthDay now = MonthDay.now();",
            "  MonthDay nowWithZone = MonthDay.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void offsetDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.OffsetDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  OffsetDateTime now = OffsetDateTime.now();",
            "  OffsetDateTime nowWithZone = OffsetDateTime.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void offsetTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.OffsetTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  OffsetTime now = OffsetTime.now();",
            "  OffsetTime nowWithZone = OffsetTime.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void year() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.Year;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  Year now = Year.now();",
            "  Year nowWithZone = Year.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void yearMonth() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.YearMonth;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  YearMonth now = YearMonth.now();",
            "  YearMonth nowWithZone = YearMonth.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void zonedDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.ZonedDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  ZonedDateTime now = ZonedDateTime.now();",
            "  ZonedDateTime nowWithZone = ZonedDateTime.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void japaneseDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.JapaneseDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  JapaneseDate now = JapaneseDate.now();",
            "  JapaneseDate nowWithZone = JapaneseDate.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void minguoDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.MinguoDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  MinguoDate now = MinguoDate.now();",
            "  MinguoDate nowWithZone = MinguoDate.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void hijrahDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.HijrahDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  HijrahDate now = HijrahDate.now();",
            "  HijrahDate nowWithZone = HijrahDate.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void thaiBuddhistDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.ThaiBuddhistDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  ThaiBuddhistDate now = ThaiBuddhistDate.now();",
            "  ThaiBuddhistDate nowWithZone = ThaiBuddhistDate.now(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void chronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.Chronology;",
            "import java.time.chrono.ChronoLocalDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  ChronoLocalDate now = Chronology.of(\"ISO\").dateNow();",
            "  ChronoLocalDate nowWithZone = Chronology.of(\"ISO\").dateNow(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void hijrahChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.HijrahChronology;",
            "import java.time.chrono.HijrahDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  HijrahDate now = HijrahChronology.INSTANCE.dateNow();",
            "  HijrahDate nowWithZone = HijrahChronology.INSTANCE.dateNow(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void isoChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.LocalDate;",
            "import java.time.chrono.IsoChronology;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  LocalDate now = IsoChronology.INSTANCE.dateNow();",
            "  LocalDate nowWithZone = IsoChronology.INSTANCE.dateNow(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void japaneseChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.JapaneseChronology;",
            "import java.time.chrono.JapaneseDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  JapaneseDate now = JapaneseChronology.INSTANCE.dateNow();",
            "  JapaneseDate nowWithZone = JapaneseChronology.INSTANCE.dateNow(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void minguoChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.MinguoChronology;",
            "import java.time.chrono.MinguoDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  MinguoDate now = MinguoChronology.INSTANCE.dateNow();",
            "  MinguoDate nowWithZone = MinguoChronology.INSTANCE.dateNow(systemDefault());",
            "}")
        .doTest();
  }

  @Test
  public void thaiBuddhistChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.ZoneId.systemDefault;",
            "import java.time.chrono.ThaiBuddhistChronology;",
            "import java.time.chrono.ThaiBuddhistDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic matches: REPLACEME",
            "  ThaiBuddhistDate now = ThaiBuddhistChronology.INSTANCE.dateNow();",
            "  ThaiBuddhistDate nowWithZone = "
                + "ThaiBuddhistChronology.INSTANCE.dateNow(systemDefault());",
            "}")
        .doTest();
  }
}
