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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link InvalidJavaTimeConstant}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@RunWith(JUnit4.class)
public class InvalidJavaTimeConstantTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(InvalidJavaTimeConstant.class, getClass());
  }

  @Test
  public void cornerCases() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.LocalDateTime;",
            "import java.time.LocalTime;",
            "public class TestCase {",
            "  // BUG: Diagnostic contains: 0",
            "  private static final LocalDateTime LDT0 = LocalDateTime.of(0, 0, 0, 0, 0);",
            "  private static final LocalDateTime LDT1 = LocalDateTime.of(0, 1, 1, 0, 0);",
            "  private static final LocalTime LT = LocalTime.ofNanoOfDay(12345678);",
            "}")
        .doTest();
  }

  @Test
  public void localDate_areOk() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.LocalDate;",
            "import java.time.Month;",
            "public class TestCase {",
            "  private static final LocalDate LD0 = LocalDate.of(1985, 5, 31);",
            "  private static final LocalDate LD1 = LocalDate.of(1985, Month.MAY, 31);",
            // we can't catch this since it's not a literal
            "  private static final LocalDate LD2 = LocalDate.of(1985, getBadMonth(), 31);",
            "  private static int getBadMonth() { return 13; }",
            "  private static final LocalDate EPOCH_DAY_ZERO = LocalDate.ofEpochDay(0);",
            "}")
        .doTest();
  }

  @Test
  public void localDate_withBadDays() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.LocalDate;",
            "import java.time.Month;",
            "public class TestCase {",
            "  // BUG: Diagnostic contains: 32",
            "  private static final LocalDate LD0 = LocalDate.of(1985, 5, 32);",
            "  // BUG: Diagnostic contains: 32",
            "  private static final LocalDate LD1 = LocalDate.of(1985, Month.MAY, 32);",
            "}")
        .doTest();
  }

  @Test
  public void localDate_withBadMonths() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.LocalDate;",
            "public class TestCase {",
            "  // BUG: Diagnostic contains: -1",
            "  private static final LocalDate LD0 = LocalDate.of(1985, -1, 31);",
            "  // BUG: Diagnostic contains: 0",
            "  private static final LocalDate LD1 = LocalDate.of(1985, 0, 31);",
            "  // BUG: Diagnostic contains: 13",
            "  private static final LocalDate LD2 = LocalDate.of(1985, 13, 31);",
            "}")
        .doTest();
  }

  @Test
  public void localDate_withBadYears() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.LocalDate;",
            "import java.time.Year;",
            "public class TestCase {",
            "  // BUG: Diagnostic contains: -1000000000",
            "  private static final LocalDate LD0 = LocalDate.of(Year.MIN_VALUE - 1, 5, 31);",
            "  // BUG: Diagnostic contains: 1000000000",
            "  private static final LocalDate LD1 = LocalDate.of(Year.MAX_VALUE + 1, 5, 31);",
            "}")
        .doTest();
  }
}
