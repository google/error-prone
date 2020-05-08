/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.StandardSystemProperty.JAVA_VERSION;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FromTemporalAccessor}. */
@RunWith(JUnit4.class)
public class FromTemporalAccessorTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(FromTemporalAccessor.class, getClass());

  @Test
  public void typeFromTypeIsBad() {
    helper
        .addSourceLines(
            "TestClass.java",
            // TODO(kak): Should we just use a wildcard import instead?
            "import java.time.DayOfWeek;",
            "import java.time.Instant;",
            "import java.time.LocalDate;",
            "import java.time.LocalDateTime;",
            "import java.time.LocalTime;",
            "import java.time.Month;",
            "import java.time.MonthDay;",
            "import java.time.OffsetDateTime;",
            "import java.time.OffsetTime;",
            "import java.time.Year;",
            "import java.time.YearMonth;",
            "import java.time.ZonedDateTime;",
            "import java.time.ZoneOffset;",
            "public class TestClass {",
            "  void from(DayOfWeek value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    DayOfWeek.from(value);",
            "  }",
            "  void from(Instant value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    Instant.from(value);",
            "  }",
            "  void from(LocalDate value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    LocalDate.from(value);",
            "  }",
            "  void from(LocalDateTime value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    LocalDateTime.from(value);",
            "  }",
            "  void from(LocalTime value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    LocalTime.from(value);",
            "  }",
            "  void from(Month value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    Month.from(value);",
            "  }",
            "  void from(MonthDay value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    MonthDay.from(value);",
            "  }",
            "  void from(OffsetDateTime value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    OffsetDateTime.from(value);",
            "  }",
            "  void from(OffsetTime value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    OffsetTime.from(value);",
            "  }",
            "  void from(Year value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    Year.from(value);",
            "  }",
            "  void from(YearMonth value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    YearMonth.from(value);",
            "  }",
            "  void from(ZonedDateTime value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    ZonedDateTime.from(value);",
            "  }",
            "  void from(ZoneOffset value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    ZoneOffset.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromTemporalAccessor_knownGood() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.DayOfWeek;",
            "import java.time.Instant;",
            "import java.time.LocalDate;",
            "import java.time.LocalDateTime;",
            "import java.time.LocalTime;",
            "import java.time.Month;",
            "import java.time.MonthDay;",
            "import java.time.OffsetDateTime;",
            "import java.time.OffsetTime;",
            "import java.time.Year;",
            "import java.time.YearMonth;",
            "import java.time.ZonedDateTime;",
            "import java.time.ZoneOffset;",
            "import java.time.temporal.TemporalAccessor;",
            "public class TestClass {",
            "  void from(TemporalAccessor value) {",
            "    DayOfWeek.from(value);",
            "    Instant.from(value);",
            "    LocalDate.from(value);",
            "    LocalDateTime.from(value);",
            "    LocalTime.from(value);",
            "    Month.from(value);",
            "    MonthDay.from(value);",
            "    OffsetDateTime.from(value);",
            "    OffsetTime.from(value);",
            "    Year.from(value);",
            "    YearMonth.from(value);",
            "    ZonedDateTime.from(value);",
            "    ZoneOffset.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_knownGood() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.LocalDate;",
            "import java.time.Year;",
            "public class TestClass {",
            "  void from(LocalDate value) {",
            "    Year.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_threeTenExtra_knownGood() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.LocalTime;",
            "import org.threeten.extra.AmPm;",
            "public class TestClass {",
            "  void from(LocalTime value) {",
            "    AmPm.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_threeTenExtra_self() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.threeten.extra.AmPm;",
            "public class TestClass {",
            "  void from(AmPm value) {",
            "    // BUG: Diagnostic contains: Did you mean 'value;'",
            "    AmPm.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_customType() {
    helper
        .addSourceLines(
            "Frobber.java",
            "import java.time.DateTimeException;",
            "import java.time.temporal.TemporalAccessor;",
            "import java.time.temporal.TemporalField;",
            "public class Frobber implements TemporalAccessor {",
            "  static Frobber from(TemporalAccessor temporalAccessor) {",
            "    if (temporalAccessor instanceof Frobber) {",
            "      return (Frobber) temporalAccessor;",
            "    }",
            "    throw new DateTimeException(\"failure\");",
            "  }",
            "  @Override public long getLong(TemporalField field) { return 0; }",
            "  @Override public boolean isSupported(TemporalField field) { return false; }",
            "}")
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  void from(Frobber value) {",
            "    Frobber frobber = Frobber.from(value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_knownBadConversions() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.LocalDate;",
            "import java.time.LocalDateTime;",
            "import org.threeten.extra.Quarter;",
            "public class TestClass {",
            "  void from(LocalDate localDate, Quarter quarter) {",
            "    // BUG: Diagnostic contains: FromTemporalAccessor",
            "    LocalDateTime.from(localDate);",
            "    // BUG: Diagnostic contains: FromTemporalAccessor",
            "    LocalDateTime.from(quarter);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeFromType_knownBadConversions_insideJavaTime() {
    // this test doesn't pass under JDK11 because of modules
    if (JAVA_VERSION.value().startsWith("1.")) {
      helper
          .addSourceLines(
              "TestClass.java",
              "package java.time;",
              "import java.time.LocalDate;",
              "import java.time.LocalDateTime;",
              "public class TestClass {",
              "  void from(LocalDate localDate) {",
              "    LocalDateTime.from(localDate);",
              "  }",
              "}")
          .doTest();
    }
  }

  @Test
  public void typeFromType_knownBadConversions_insideThreeTenExtra() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.threeten.extra;",
            "import java.time.LocalDateTime;",
            "public class TestClass {",
            "  void from(Quarter quarter) {",
            "    LocalDateTime.from(quarter);",
            "  }",
            "}")
        .doTest();
  }
}
