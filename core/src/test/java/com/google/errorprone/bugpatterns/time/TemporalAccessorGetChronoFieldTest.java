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

/** Tests for {@link TemporalAccessorGetChronoField}. */
@RunWith(JUnit4.class)
public class TemporalAccessorGetChronoFieldTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TemporalAccessorGetChronoField.class, getClass());

  @Test
  public void temporalAccessor_getLong_noStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.DayOfWeek.MONDAY;",
            "import java.time.temporal.ChronoField;",
            "public class TestClass {",
            "  private static final long value1 = MONDAY.getLong(ChronoField.DAY_OF_WEEK);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final long value2 = MONDAY.getLong(ChronoField.NANO_OF_DAY);",
            "}")
        .doTest();
  }

  @Test
  public void temporalAccessor_getLong_staticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.DayOfWeek.MONDAY;",
            "import static java.time.temporal.ChronoField.DAY_OF_WEEK;",
            "import static java.time.temporal.ChronoField.NANO_OF_DAY;",
            "public class TestClass {",
            "  private static final long value1 = MONDAY.getLong(DAY_OF_WEEK);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final long value2 = MONDAY.getLong(NANO_OF_DAY);",
            "}")
        .doTest();
  }

  @Test
  public void temporalAccessor_get_noStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.DayOfWeek.MONDAY;",
            "import java.time.temporal.ChronoField;",
            "public class TestClass {",
            "  private static final int value1 = MONDAY.get(ChronoField.DAY_OF_WEEK);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final int value2 = MONDAY.get(ChronoField.NANO_OF_DAY);",
            "}")
        .doTest();
  }

  @Test
  public void temporalAccessor_get_staticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.DayOfWeek.MONDAY;",
            "import static java.time.temporal.ChronoField.DAY_OF_WEEK;",
            "import static java.time.temporal.ChronoField.NANO_OF_DAY;",
            "public class TestClass {",
            "  private static final int value1 = MONDAY.get(DAY_OF_WEEK);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final int value2 = MONDAY.get(NANO_OF_DAY);",
            "}")
        .doTest();
  }

  @Test
  public void temporalAccessor_realCode() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.time.temporal.ChronoField.MICRO_OF_SECOND;",
            "import static java.time.temporal.ChronoField.DAY_OF_WEEK;",
            "import java.time.Instant;",
            "public class TestClass {",
            "  private static final int value1 = Instant.now().get(MICRO_OF_SECOND);",
            "  private static final long value2 = Instant.now().getLong(MICRO_OF_SECOND);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final int value3 = Instant.now().get(DAY_OF_WEEK);",
            "  // BUG: Diagnostic contains: TemporalAccessorGetChronoField",
            "  private static final long value4 = Instant.now().getLong(DAY_OF_WEEK);",
            "}")
        .doTest();
  }
}
