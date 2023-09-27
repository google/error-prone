/*
 * Copyright 2023 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class DuplicateDateFormatFieldTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DuplicateDateFormatField.class, getClass());

  @Test
  public void singleDuplicateField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the field 'm' more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(\"mm/dd/yyyy hh:mm:ss\");",
            "}")
        .doTest();
  }

  @Test
  public void doubleDuplicateFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the fields ['m', 's'] more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(\"mm/dd/yyyy" + " hh:mm:ss.sss\");",
            "}")
        .doTest();
  }

  @Test
  public void constantWithDuplicateField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  static final String PATTERN = \"mm/dd/yyyy hh:mm:ss\";",
            "  // BUG: Diagnostic contains: uses the field 'm' more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(PATTERN);",
            "}")
        .doTest();
  }

  @Test
  public void recognizedDateTimeFormat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatter;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the field 'm' more than once",
            "  DateTimeFormatter formatter = DateTimeFormatter.ofPattern(\"mm/dd/yyyy hh:mm:ss\");",
            "}")
        .doTest();
  }

  @Test
  public void simpleDateFormat_applyPattern() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  public void foo() {",
            "    SimpleDateFormat format = new SimpleDateFormat();",
            "    // BUG: Diagnostic contains: uses the field 'm' more than once",
            "    format.applyPattern(\"mm/dd/yyyy hh:mm:ss\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void simpleDateFormat_applyLocalizedPattern() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  public void foo() {",
            "    SimpleDateFormat format = new SimpleDateFormat();",
            "    // BUG: Diagnostic contains: uses the field 'm' more than once",
            "    format.applyLocalizedPattern(\"mm/dd/yyyy hh:mm:ss\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void forgotToEscapteSpecialCharacters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the field 'W' more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(\"Week W ' of ' L\");",
            "}")
        .doTest();
  }

  @Test
  public void withOptionalGroup() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the field 'm' more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(\"hh:mm[:ss] yyyy/mm/dd\");",
            "}")
        .doTest();
  }

  @Test
  public void withNeestedOptionalGroup() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  // BUG: Diagnostic contains: uses the field 'm' more than once",
            "  SimpleDateFormat format = new SimpleDateFormat(\"hh:mm[:ss[.SSS]] yyyy/mm/dd\");",
            "}")
        .doTest();
  }

  @Test
  public void noDupliatedFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  SimpleDateFormat format = new SimpleDateFormat(\"yyyy-MM-dd\");",
            "}")
        .doTest();
  }

  @Test
  public void ignoresEscapedPatternCharacters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  SimpleDateFormat format = new SimpleDateFormat(\"'Week' W ' of ' L\");",
            "}")
        .doTest();
  }

  @Test
  public void ignoredOptionalGroups() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  SimpleDateFormat format = ",
            "    new SimpleDateFormat(\"yyyy'-'MM'-'dd'T'HH':'mm[':'ss][XXX][X]\");",
            "}")
        .doTest();
  }
}
