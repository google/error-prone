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

/** Tests for {@link JodaNewPeriod}. */
@RunWith(JUnit4.class)
public final class JodaNewPeriodTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaNewPeriod.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "import org.joda.time.LocalDate;",
            "import org.joda.time.Period;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: new Duration(10, 20).getStandardDays()",
            "  private static final int DAYS = new Period(10, 20).getDays();",
            "  int days(LocalDate a, LocalDate b) {",
            "    // BUG: Diagnostic contains: Days.daysBetween(a, b).getDays()",
            "    return new Period(a, b).getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Period;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Period period = new Period(10, 20);",
            "  private static final int DAYS = period.getDays();",
            "}")
        .doTest();
  }
}
