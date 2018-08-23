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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.CompilationTestHelper;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author awturner@google.com (Andy Turner) */
@RunWith(JUnit4.class)
public class ThreeLetterTimeZoneIDTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ThreeLetterTimeZoneID.class, getClass());
  }

  @Test
  public void testAllThreeLetterIdsAreCoveredByZoneIdShortIds() {
    // The check's logic is predicated on there being an entry in SHORT_IDS for all three-letter
    // IDs in TimeZone.getAvailableIDs() that aren't in ZoneId.getAvailableZoneIds(). Sanity check.
    Set<String> availableZoneIds = new HashSet<>(ZoneId.getAvailableZoneIds());
    Set<String> expectedIds =
        Arrays.asList(TimeZone.getAvailableIDs()).stream()
            .filter(s -> s.length() == 3)
            .filter(s -> !availableZoneIds.contains(s))
            .collect(Collectors.toSet());

    assertThat(ZoneId.SHORT_IDS.keySet()).containsExactlyElementsIn(expectedIds);
  }

  @Test
  @Ignore
  public void printMappings() {
    // This is here for debugging, to allow printing of the suggested replacements.
    for (String id : TimeZone.getAvailableIDs()) {
      if (id.length() == 3) {
        System.out.printf(
            "%s %s %s%n",
            id,
            ThreeLetterTimeZoneID.getReplacement(id, false).replacements,
            ThreeLetterTimeZoneID.getReplacement(id, true).replacements);
      }
    }
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import java.util.TimeZone;",
            "class A {",
            "  public static void test_PST() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"America/Los_Angeles\")",
            "    TimeZone.getTimeZone(\"PST\");",
            "  }",
            "  public static void test_EST() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"Etc/GMT+5\")",
            "    TimeZone.getTimeZone(\"EST\");",
            "  }",
            "  public static void test_noPreferredReplacements() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"Asia/Dhaka\")",
            "    TimeZone.getTimeZone(\"BST\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCaseJodaTime() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import java.util.TimeZone;",
            "import org.joda.time.DateTimeZone;",
            "class A {",
            "  public static void test_EST() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"America/New_York\")",
            "    DateTimeZone.forTimeZone(TimeZone.getTimeZone(\"EST\"));",
            "  }",
            "  public static void test_HST() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"Pacific/Honolulu\")",
            "    DateTimeZone.forTimeZone(TimeZone.getTimeZone(\"HST\"));",
            "  }",
            "  public static void test_MST() {",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"America/Denver\")",
            "    DateTimeZone.forTimeZone(TimeZone.getTimeZone(\"MST\"));",
            "  }",
            "  public static void test_PST() {",
            "    // Not a special case, but should still work.",
            "    // BUG: Diagnostic contains: TimeZone.getTimeZone(\"America/Los_Angeles\")",
            "    DateTimeZone.forTimeZone(TimeZone.getTimeZone(\"PST\"));",
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
            "  public static void notThreeLetter() {",
            "    TimeZone.getTimeZone(\"\");",
            "    TimeZone.getTimeZone(\"America/Los_Angeles\");",
            "  }",
            "  public static void threeLetterButAllowed() {",
            "    TimeZone.getTimeZone(\"GMT\");",
            "    TimeZone.getTimeZone(\"UTC\");",
            "    TimeZone.getTimeZone(\"CET\");",
            "    TimeZone.getTimeZone(\"PRC\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReplacements_PST() {
    List<String> replacements = ThreeLetterTimeZoneID.getReplacement("PST", false).replacements;
    // Suggests IANA ID first, then the fixed offset.
    assertThat(replacements).containsExactly("America/Los_Angeles", "Etc/GMT+8").inOrder();
  }

  @Test
  public void testReplacements_EST() {
    List<String> replacements = ThreeLetterTimeZoneID.getReplacement("EST", false).replacements;
    // Suggests fixed offset first, then the IANA ID, because this the former has the same rules as
    // TimeZone.getTimeZone("EST").
    assertThat(replacements).containsExactly("Etc/GMT+5", "America/New_York").inOrder();
  }

  @Test
  public void testReplacements_IST() {
    List<String> replacements = ThreeLetterTimeZoneID.getReplacement("IST", false).replacements;
    assertThat(replacements).containsExactly("Asia/Kolkata").inOrder();
  }

  @Test
  public void testReplacements_CST() {
    // Only rule-equivalent suggestions are made (unless we have explicitly provided suggestions) -
    // we don't suggest "China Standard Time" for CST, because the existing code is semantically
    // equivalent to US "Central Standard Time".
    List<String> replacements = ThreeLetterTimeZoneID.getReplacement("CST", false).replacements;
    assertThat(replacements).contains("America/Chicago");
    assertThat(replacements).doesNotContain("Asia/Shanghai");
  }
}
