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
      CompilationTestHelper.newInstance(JavaTimeDefaultTimeZone.class, getClass());

  @Test
  public void clock() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.Clock;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Clock.systemDefaultZone() is not allowed",
            "  // Clock clock = Clock.system(unsafeDefaultZoneId());",
            "  Clock clock = Clock.systemDefaultZone();",
            "  Clock clockWithZone = Clock.system(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void localDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.LocalDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: LocalDate.now() is not allowed",
            "  // LocalDate now = LocalDate.now(unsafeDefaultZoneId());",
            "  LocalDate now = LocalDate.now();",
            "  LocalDate nowWithZone = LocalDate.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void localTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.LocalTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: LocalTime.now() is not allowed",
            "  // LocalTime now = LocalTime.now(unsafeDefaultZoneId());",
            "  LocalTime now = LocalTime.now();",
            "  LocalTime nowWithZone = LocalTime.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void localDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.LocalDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: LocalDateTime.now() is not allowed",
            "  // LocalDateTime now = LocalDateTime.now(unsafeDefaultZoneId());",
            "  LocalDateTime now = LocalDateTime.now();",
            "  LocalDateTime nowWithZone = LocalDateTime.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void monthDay() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.MonthDay;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: MonthDay.now() is not allowed",
            "  // MonthDay now = MonthDay.now(unsafeDefaultZoneId());",
            "  MonthDay now = MonthDay.now();",
            "  MonthDay nowWithZone = MonthDay.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void offsetDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.OffsetDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: OffsetDateTime.now() is not allowed",
            "  // OffsetDateTime now = OffsetDateTime.now(unsafeDefaultZoneId());",
            "  OffsetDateTime now = OffsetDateTime.now();",
            "  OffsetDateTime nowWithZone = OffsetDateTime.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void offsetTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.OffsetTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: OffsetTime.now() is not allowed",
            "  // OffsetTime now = OffsetTime.now(unsafeDefaultZoneId());",
            "  OffsetTime now = OffsetTime.now();",
            "  OffsetTime nowWithZone = OffsetTime.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void year() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.Year;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Year.now() is not allowed",
            "  // Year now = Year.now(unsafeDefaultZoneId());",
            "  Year now = Year.now();",
            "  Year nowWithZone = Year.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void yearMonth() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.YearMonth;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: YearMonth.now() is not allowed",
            "  // YearMonth now = YearMonth.now(unsafeDefaultZoneId());",
            "  YearMonth now = YearMonth.now();",
            "  YearMonth nowWithZone = YearMonth.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void zonedDateTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.ZonedDateTime;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: ZonedDateTime.now() is not allowed",
            "  // ZonedDateTime now = ZonedDateTime.now(unsafeDefaultZoneId());",
            "  ZonedDateTime now = ZonedDateTime.now();",
            "  ZonedDateTime nowWithZone = ZonedDateTime.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void japaneseDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.JapaneseDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: JapaneseDate.now() is not allowed",
            "  // JapaneseDate now = JapaneseDate.now(unsafeDefaultZoneId());",
            "  JapaneseDate now = JapaneseDate.now();",
            "  JapaneseDate nowWithZone = JapaneseDate.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void minguoDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.MinguoDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: MinguoDate.now() is not allowed",
            "  // MinguoDate now = MinguoDate.now(unsafeDefaultZoneId());",
            "  MinguoDate now = MinguoDate.now();",
            "  MinguoDate nowWithZone = MinguoDate.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void hijrahDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.HijrahDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: HijrahDate.now() is not allowed",
            "  // HijrahDate now = HijrahDate.now(unsafeDefaultZoneId());",
            "  HijrahDate now = HijrahDate.now();",
            "  HijrahDate nowWithZone = HijrahDate.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void thaiBuddhistDate() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.ThaiBuddhistDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: ThaiBuddhistDate.now() is not allowed",
            "  // ThaiBuddhistDate now = ThaiBuddhistDate.now(unsafeDefaultZoneId());",
            "  ThaiBuddhistDate now = ThaiBuddhistDate.now();",
            "  ThaiBuddhistDate nowWithZone = ThaiBuddhistDate.now(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void chronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.Chronology;",
            "import java.time.chrono.ChronoLocalDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Chronology.dateNow() is not allowed",
            "  // ChronoLocalDate now = Chronology.of(\"ISO\").dateNow(unsafeDefaultZoneId());",
            "  ChronoLocalDate now = Chronology.of(\"ISO\").dateNow();",
            "  ChronoLocalDate nowWithZone = Chronology.of(\"ISO\").dateNow(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void hijrahChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.HijrahChronology;",
            "import java.time.chrono.HijrahDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: HijrahChronology.dateNow() is not allowed",
            "  // HijrahDate now = HijrahChronology.INSTANCE.dateNow(unsafeDefaultZoneId());",
            "  HijrahDate now = HijrahChronology.INSTANCE.dateNow();",
            "  HijrahDate nowWithZone = HijrahChronology.INSTANCE.dateNow(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void isoChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.LocalDate;",
            "import java.time.chrono.IsoChronology;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: IsoChronology.dateNow() is not allowed",
            "  // LocalDate now = IsoChronology.INSTANCE.dateNow(unsafeDefaultZoneId());",
            "  LocalDate now = IsoChronology.INSTANCE.dateNow();",
            "  LocalDate nowWithZone = IsoChronology.INSTANCE.dateNow(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void japaneseChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.JapaneseChronology;",
            "import java.time.chrono.JapaneseDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: JapaneseChronology.dateNow() is not allowed",
            "  // JapaneseDate now = JapaneseChronology.INSTANCE.dateNow(unsafeDefaultZoneId());",
            "  JapaneseDate now = JapaneseChronology.INSTANCE.dateNow();",
            "  JapaneseDate nowWithZone = JapaneseChronology.INSTANCE.dateNow(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void minguoChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.MinguoChronology;",
            "import java.time.chrono.MinguoDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: MinguoChronology.dateNow() is not allowed",
            "  // MinguoDate now = MinguoChronology.INSTANCE.dateNow(unsafeDefaultZoneId());",
            "  MinguoDate now = MinguoChronology.INSTANCE.dateNow();",
            "  MinguoDate nowWithZone = MinguoChronology.INSTANCE.dateNow(googleZoneId());",
            "}")
        .doTest();
  }

  @Test
  public void thaiBuddhistChronology() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static com.google.common.time.ZoneIds.googleZoneId;",
            "import java.time.chrono.ThaiBuddhistChronology;",
            "import java.time.chrono.ThaiBuddhistDate;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: ThaiBuddhistChronology.dateNow() is not allowed",
            "  // ThaiBuddhistDate now = "
                + "ThaiBuddhistChronology.INSTANCE.dateNow(unsafeDefaultZoneId());",
            "  ThaiBuddhistDate now = ThaiBuddhistChronology.INSTANCE.dateNow();",
            "  ThaiBuddhistDate nowWithZone = "
                + "ThaiBuddhistChronology.INSTANCE.dateNow(googleZoneId());",
            "}")
        .doTest();
  }
}
