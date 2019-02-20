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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author bhagwani@google.com (Sumit Bhagwani) */
@RunWith(JUnit4.class)
public class InvalidZoneIdTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(InvalidZoneId.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import java.time.ZoneId;",
            "class A {",
            "  private static final String TIMEZONE_ID = \"unknown\";",
            "  public static void test() {",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"unknown\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(TIMEZONE_ID);",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"America/Los_Angele\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"America/Los Angeles\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"KST\");",
            "  }",
            "  public static void invalidCustomIDs() {",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"GMT+24\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"GMT1\");",
            "    // BUG: Diagnostic contains:",
            "    ZoneId.of(\"GMT/0\");",
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
            "import java.time.ZoneId;",
            "class A {",
            "  private static final String TIMEZONE_ID = \"America/New_York\";",
            "  public static void ianaIDs() {",
            "    ZoneId.of(\"America/Los_Angeles\");",
            "    ZoneId.of(TIMEZONE_ID);",
            "    ZoneId.of(\"Europe/London\");",
            "  }",
            "  public static void customIDs() {",
            "    // Custom IDs",
            "    ZoneId.of(\"GMT+0\");",
            "    ZoneId.of(\"GMT+00\");",
            "    ZoneId.of(\"GMT+00:00\");",
            "    ZoneId.of(\"GMT+00:00:00\");",
            "  }",
            "  public static void twoLetterIDs() {",
            "    ZoneId.of(\"UT\");",
            "  }",
            "  public static void threeLetterIDs() {",
            "    ZoneId.of(\"GMT\");",
            "    ZoneId.of(\"UTC\");",
            "  }",
            "}")
        .doTest();
  }
}
