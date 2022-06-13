/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FloggerLogString}Test */
@RunWith(JUnit4.class)
public class FloggerLogStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerLogString.class, getClass());

  private static final String ERROR_MESSAGE =
      "Arguments to log(String) must be compile-time constants";

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(String s) {",
            "    // BUG: Diagnostic contains: " + ERROR_MESSAGE,
            "    logger.atInfo().log(s);",
            "    // BUG: Diagnostic contains: " + ERROR_MESSAGE,
            "    logger.atInfo().log(\"foo \" + s);",
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
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String CONSTANT = \"CONSTANT\";",
            "  public void f(@CompileTimeConstant String s) {",
            "    final String localFinal = \"localFinal\";",
            "    logger.atInfo().log(\"hello\");",
            "    logger.atInfo().log(s);",
            "    logger.atInfo().log(CONSTANT);",
            "    logger.atInfo().log(localFinal);",
            "    logger.atInfo().log(\"hello \" + s);",
            "    logger.atInfo().log(\"hello \" + CONSTANT);",
            "    logger.atInfo().log(\"hello \" + localFinal);",
            "    logger.atInfo().log(\"hello \"+ s + CONSTANT);",
            "    logger.atInfo().log(\"hello \"+ s + localFinal);",
            "    logger.atInfo().log(\"hello \" + localFinal + CONSTANT);",
            "    logger.atInfo().log(\"hello \"+ s + localFinal + CONSTANT);",
            "    logger.atInfo().log(s + localFinal);",
            "    logger.atInfo().log(s + CONSTANT);",
            "    logger.atInfo().log(CONSTANT + localFinal);",
            "    logger.atInfo().log(s + localFinal + CONSTANT);",
            "    logger.atInfo().log((String) null);",
            "  }",
            "}")
        .doTest();
  }
}
