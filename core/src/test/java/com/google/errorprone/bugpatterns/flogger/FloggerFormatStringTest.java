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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FloggerFormatString}Test */
@RunWith(JUnit4.class)
public class FloggerFormatStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerFormatString.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(Exception e, Throwable t) {",
            "    // BUG: Diagnostic contains: 'java.lang.String' cannot be formatted using '%d'",
            "    logger.atInfo().log(\"hello %d\", \"world\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithCause() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(Exception e, Throwable t) {",
            "    // BUG: Diagnostic contains: logger.atInfo().withCause(e).log(\"hello\");",
            "    logger.atInfo().log(\"hello\", e);",
            "    // BUG: Diagnostic contains: logger.atInfo().withCause(t).log(\"hello %s\", e);",
            "    logger.atInfo().log(\"hello %s\", e, t);",
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
            "  public void f(Exception e, Throwable t) {",
            "    logger.atInfo().withCause(e).log(\"hello %s\", e);",
            "    logger.atInfo().log();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lazyArg() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.flogger.LazyArgs.lazy;",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(String s, Integer i) {",
            "    // BUG: Diagnostic contains: 'java.lang.String'",
            "    logger.atInfo().log(\"hello %d\", lazy(() -> s));",
            "    logger.atInfo().log(\"hello %d\", lazy(() -> i));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(Object... xs) {",
            "    // BUG: Diagnostic contains:",
            "    logger.atInfo().log(\"hello %s %s\", xs);",
            "  }",
            "}")
        .doTest();
  }
}
