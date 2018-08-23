/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author awturner@google.com (Andy Turner) */
@RunWith(JUnit4.class)
public class InvalidTimeZoneIDTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(InvalidTimeZoneID.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import java.util.TimeZone;",
            "class A {",
            "  private static final String TIMEZONE_ID = \"unknown\";",
            "  public static void test() {",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"unknown\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(TIMEZONE_ID);",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"America/Los_Angele\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"KST\");",
            "  }",
            "  public static void invalidCustomIDs() {",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"UTC+0\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"GMT+24\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"GMT1\");",
            "    // BUG: Diagnostic contains:",
            "    TimeZone.getTimeZone(\"GMT/0\");",
            "  }",
            "  public static void underscoreSuggestion() {",
            "    // BUG: Diagnostic contains: America/Los_Angeles",
            "    TimeZone.getTimeZone(\"America/Los Angeles\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import java.util.TimeZone;",
            "class A {",
            "  private static final String TIMEZONE_ID = \"America/New_York\";",
            "  public static void ianaIDs() {",
            "    TimeZone.getTimeZone(\"America/Los_Angeles\");",
            "    TimeZone.getTimeZone(TIMEZONE_ID);",
            "    TimeZone.getTimeZone(\"Europe/London\");",
            "  }",
            "  public static void customIDs() {",
            "    // Custom IDs",
            "    TimeZone.getTimeZone(\"GMT+0\");",
            "    TimeZone.getTimeZone(\"GMT+00\");",
            "    TimeZone.getTimeZone(\"GMT+00:00\");",
            "  }",
            "  public static void threeLetterIDs() {",
            "    TimeZone.getTimeZone(\"GMT\");",
            "    TimeZone.getTimeZone(\"UTC\");",
            "    // Some 3-letter IDs are deprecated, but are still recognized.",
            "    TimeZone.getTimeZone(\"PST\");",
            "  }",
            "}")
        .doTest();
  }
}
