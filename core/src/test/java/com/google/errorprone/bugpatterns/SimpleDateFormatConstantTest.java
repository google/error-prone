/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link java.text.SimpleDateFormat}Test */
@RunWith(JUnit4.class)
public class SimpleDateFormatConstantTest {
  @Test
  public void negative() throws IOException {
    CompilationTestHelper.newInstance(SimpleDateFormatConstant.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  private static final SimpleDateFormat NO_INITIALIZER;",
            "  static {",
            "    NO_INITIALIZER = new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  }",
            "  private final SimpleDateFormat NON_STATIC =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  private static SimpleDateFormat NON_FINAL =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  private static final SimpleDateFormat lowerCamelCase =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  static void f() {",
            "    final SimpleDateFormat NOT_A_FIELD =",
            "      new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  }",
            "  private static final String NOT_A_SIMPLE_DATE_FORMAT = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void threadLocalFix() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new SimpleDateFormatConstant(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.text.SimpleDateFormat;",
            "import java.util.Date;",
            "class Test {",
            "  private static final SimpleDateFormat DATE_FORMAT =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  static String f(Date d) {",
            "    return DATE_FORMAT.format(d);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.text.SimpleDateFormat;",
            "import java.util.Date;",
            "class Test {",
            "  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ",
            "    ThreadLocal.withInitial(() -> new SimpleDateFormat(\"yyyy-MM-dd HH:mm\"));",
            "  static String f(Date d) {",
            "    return DATE_FORMAT.get().format(d);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lowerCamelCaseFix() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new SimpleDateFormatConstant(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.text.SimpleDateFormat;",
            "import java.util.Date;",
            "class Test {",
            "  private static final SimpleDateFormat DATE_FORMAT =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  static String f(Date d) {",
            "    return DATE_FORMAT.format(d);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.text.SimpleDateFormat;",
            "import java.util.Date;",
            "class Test {",
            "  private static final SimpleDateFormat dateFormat =",
            "    new SimpleDateFormat(\"yyyy-MM-dd HH:mm\");",
            "  static String f(Date d) {",
            "    return dateFormat.format(d);",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }
}
