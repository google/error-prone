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

package com.google.errorprone.bugpatterns.flogger;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FloggerMessageFormat}. */
@RunWith(JUnit4.class)
public class FloggerMessageFormatTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerMessageFormat.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String FORMAT_STRING = \"hello {0}\";",
            "  public void method(Exception e) {",
            "    // BUG: Diagnostic contains:",
            "    logger.atInfo().log(\"hello {0}\");",
            "    // BUG: Diagnostic contains:",
            "    logger.atInfo().log(\"hello {0}\", \"world\");",
            "    // BUG: Diagnostic contains:",
            "    logger.atInfo().log(\"\" + \"hello {0}\", \"world\");",
            "    // BUG: Diagnostic contains:",
            "    logger.atInfo().log(FORMAT_STRING, \"world\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String FORMAT_STRING = \"hello {0}\";",
            "  public void method(Exception e) {",
            "    logger.atInfo().log(\"hello %s\", \"world\");",
            "    logger.atInfo().log();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fix() {
    BugCheckerRefactoringTestHelper.newInstance(new FloggerMessageFormat(), getClass())
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String FORMAT_STRING = \"hello {0}\";",
            "  public void method(Exception e) {",
            "    logger.atInfo().log(\"hello {0}\", \"world\");",
            "    logger.atInfo().log(\"\" + \"hello {0}\", \"world\");",
            "    logger.atInfo().log(FORMAT_STRING, \"world\");",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String FORMAT_STRING = \"hello {0}\";",
            "  public void method(Exception e) {",
            "    logger.atInfo().log(\"hello %s\", \"world\");",
            "    logger.atInfo().log(\"\" + \"hello %s\", \"world\");",
            "    logger.atInfo().log(FORMAT_STRING, \"world\");",
            "  }",
            "}")
        .doTest();
  }
}
