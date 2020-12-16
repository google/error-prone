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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JodaDateTimeConstants}. */
@RunWith(JUnit4.class)
public class JodaDateTimeConstantsTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaDateTimeConstants.class, getClass());

  @Test
  public void assignment() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTimeConstants;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: JodaDateTimeConstants",
            "  private final long oneMinsInMillis = DateTimeConstants.MILLIS_PER_MINUTE;",
            // this usage is OK - we only flag the _PER_ constants
            "  private final int january = DateTimeConstants.JANUARY;",
            "}")
        .doTest();
  }

  @Test
  public void usedInMultiplication() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTimeConstants;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: JodaDateTimeConstants",
            "  private final long sixMinsInMillis = 6 * DateTimeConstants.MILLIS_PER_MINUTE;",
            "}")
        .doTest();
  }

  @Test
  public void staticImported() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static org.joda.time.DateTimeConstants.MILLIS_PER_MINUTE;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: JodaDateTimeConstants",
            "  private final long sixMinsInMillis = 6 * MILLIS_PER_MINUTE;",
            "}")
        .doTest();
  }
}
