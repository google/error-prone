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

import static com.google.common.truth.Truth.assertThat;
import static java.time.Month.DECEMBER;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;

import com.google.errorprone.CompilationTestHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DateChecker}. */
@RunWith(JUnit4.class)
@SuppressWarnings({"DateChecker", "JdkObsolete", "deprecation"})
public class DateCheckerTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DateChecker.class, getClass());

  @Test
  public void testBadBehavior() {
    assertThat(toLocalDate(2020, 6, 10)).isEqualTo(LocalDate.of(2020 + 1900, 6 + 1, 10));

    assertThat(toLocalDate(120, 0, 20)).isEqualTo(LocalDate.of(2020, JANUARY, 20));
    assertThat(toLocalDate(120, 12, 20)).isEqualTo(LocalDate.of(2021, JANUARY, 20));
    assertThat(toLocalDate(120, 13, 20)).isEqualTo(LocalDate.of(2021, FEBRUARY, 20));
    assertThat(toLocalDate(120, -1, 20)).isEqualTo(LocalDate.of(2019, DECEMBER, 20));
    assertThat(toLocalDate(121, -7, 20)).isEqualTo(LocalDate.of(2020, JUNE, 20));
    assertThat(toLocalDate(121, -7, 0)).isEqualTo(LocalDate.of(2020, MAY, 31));
    assertThat(toLocalDate(121, -7, -1)).isEqualTo(LocalDate.of(2020, MAY, 30));

    LocalDateTime expected = LocalDateTime.of(2020, JUNE, 20, 16, 36, 13);
    // normal (well, for some definition of "normal")
    assertThat(toLocalDateTime(120, 5, 20, 16, 36, 13)).isEqualTo(expected);
    // hours = 40 (wrap around)
    assertThat(toLocalDateTime(120, 5, 19, 40, 36, 13)).isEqualTo(expected);
    // minutes = 96 (wrap around)
    assertThat(toLocalDateTime(120, 5, 20, 15, 96, 13)).isEqualTo(expected);
    // seconds = 73 (wrap around)
    assertThat(toLocalDateTime(120, 5, 20, 16, 35, 73)).isEqualTo(expected);
  }

  private static LocalDate toLocalDate(int year, int month, int day) {
    return new Date(year, month, day).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private static LocalDateTime toLocalDateTime(
      int year, int month, int day, int hour, int minute, int second) {
    return new Date(year, month, day, hour, minute, second)
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  @Test
  public void constructor_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.Calendar.JULY;",
            "import java.util.Date;",
            "public class TestClass {",
            "  Date good = new Date(120, JULY, 10);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_nonConstantMonth() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Use Calendar.MAY instead of 4 to represent the month.",
            "  Date good = new Date(120, 4, 10);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_constants() {
    // TODO(kak): We may want to consider warning on this case as well.
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  private static final int MAY = 4;",
            "  Date good = new Date(120, MAY, 31);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_nonConstants() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  Date good = new Date(getYear(), getMonth(), getDay());",
            "  int getYear() { return 120; }",
            "  int getMonth() { return 0; }",
            "  int getDay() { return 1; }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_allBad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: "
                + "The 1900-based year value (2020) is out of bounds [1..150].  "
                + "The 0-based month value (13) is out of bounds [0..11].",
            "  Date bad1 = new Date(2020, 13, 31);",
            "  // BUG: Diagnostic contains: "
                + "The 1900-based year value (2020) is out of bounds [1..150].  "
                + "The 0-based month value (13) is out of bounds [0..11].  "
                + "The hours value (-2) is out of bounds [0..23].  "
                + "The minutes value (61) is out of bounds [0..59].",
            "  Date bad2 = new Date(2020, 13, 31, -2, 61);",
            "  // BUG: Diagnostic contains: "
                + "The 1900-based year value (2020) is out of bounds [1..150].  "
                + "The 0-based month value (13) is out of bounds [0..11].  "
                + "The hours value (-2) is out of bounds [0..23].  "
                + "The minutes value (61) is out of bounds [0..59].  "
                + "The seconds value (75) is out of bounds [0..59].",
            "  Date bad3 = new Date(2020, 13, 31, -2, 61, 75);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_badYear() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.Calendar.JULY;",
            "import java.util.Date;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: "
                + "The 1900-based year value (2020) is out of bounds [1..150].",
            "  Date bad = new Date(2020, JULY, 10);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_badMonth() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: The 0-based month value (12) is out of bounds [0..11].",
            "  Date bad1 = new Date(120, 12, 10);",
            "  // BUG: Diagnostic contains: The 0-based month value (13) is out of bounds [0..11].",
            "  Date bad2 = new Date(120, 13, 10);",
            "  // BUG: Diagnostic contains: The 0-based month value (-1) is out of bounds [0..11].",
            "  Date bad3 = new Date(120, -1, 10);",
            "  // BUG: Diagnostic contains: The 0-based month value (-13) is out of bounds"
                + " [0..11].",
            "  Date bad4 = new Date(120, -13, 10);",
            "  // BUG: Diagnostic contains: Use Calendar.MAY instead of 4 to represent the month.",
            "  Date bad5 = new Date(120, 4, 10);",
            "}")
        .doTest();
  }

  @Test
  public void constructor_badDay() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.Calendar.JULY;",
            "import java.util.Date;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: The day value (32) is out of bounds [1..31].",
            "  Date bad1 = new Date(120, JULY, 32);",
            "  // BUG: Diagnostic contains: The day value (0) is out of bounds [1..31].",
            "  Date bad2 = new Date(120, JULY, 0);",
            "  // BUG: Diagnostic contains: The day value (-32) is out of bounds [1..31].",
            "  Date bad3 = new Date(120, JULY, -32);",
            "}")
        .doTest();
  }

  @Test
  public void setters_good() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.Calendar.*;",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    date.setYear(1);",
            "    date.setYear(120);",
            "    date.setYear(150);",
            "    date.setMonth(JANUARY);",
            "    date.setMonth(FEBRUARY);",
            "    date.setMonth(MARCH);",
            "    date.setMonth(APRIL);",
            "    date.setMonth(MAY);",
            "    date.setMonth(JUNE);",
            "    date.setMonth(JULY);",
            "    date.setMonth(AUGUST);",
            "    date.setMonth(SEPTEMBER);",
            "    date.setMonth(OCTOBER);",
            "    date.setMonth(NOVEMBER);",
            "    date.setMonth(DECEMBER);",
            "    date.setDate(1);",
            "    date.setDate(15);",
            "    date.setDate(31);",
            "    date.setHours(0);",
            "    date.setHours(12);",
            "    date.setHours(23);",
            "    date.setMinutes(0);",
            "    date.setMinutes(30);",
            "    date.setMinutes(59);",
            "    date.setSeconds(0);",
            "    date.setSeconds(30);",
            "    date.setSeconds(59);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badYears() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: "
                + "The 1900-based year value (0) is out of bounds [1..150].",
            "    date.setYear(0);",
            "    // BUG: Diagnostic contains: "
                + "The 1900-based year value (-1) is out of bounds [1..150].",
            "    date.setYear(-1);",
            "    // BUG: Diagnostic contains: "
                + "The 1900-based year value (2020) is out of bounds [1..150].",
            "    date.setYear(2020);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badMonths() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: The 0-based month value (-13) is out of bounds"
                + " [0..11].",
            "    date.setMonth(-13);",
            "    // BUG: Diagnostic contains: The 0-based month value (-1) is out of bounds"
                + " [0..11].",
            "    date.setMonth(-1);",
            "    // BUG: Diagnostic contains: The 0-based month value (12) is out of bounds"
                + " [0..11].",
            "    date.setMonth(12);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badDays() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: The day value (-32) is out of bounds [1..31].",
            "    date.setDate(-32);",
            "    // BUG: Diagnostic contains: The day value (0) is out of bounds [1..31].",
            "    date.setDate(0);",
            "    // BUG: Diagnostic contains: The day value (32) is out of bounds [1..31].",
            "    date.setDate(32);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badHours() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: The hours value (-1) is out of bounds [0..23].",
            "    date.setHours(-1);",
            "    // BUG: Diagnostic contains: The hours value (24) is out of bounds [0..23].",
            "    date.setHours(24);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badMinutes() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: The minutes value (-1) is out of bounds [0..59].",
            "    date.setMinutes(-1);",
            "    // BUG: Diagnostic contains: The minutes value (60) is out of bounds [0..59].",
            "    date.setMinutes(60);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setters_badSeconds() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.Date;",
            "public class TestClass {",
            "  public void foo(Date date) {",
            "    // BUG: Diagnostic contains: The seconds value (-1) is out of bounds [0..59].",
            "    date.setSeconds(-1);",
            "    // BUG: Diagnostic contains: The seconds value (60) is out of bounds [0..59].",
            "    date.setSeconds(60);",
            "  }",
            "}")
        .doTest();
  }
}
