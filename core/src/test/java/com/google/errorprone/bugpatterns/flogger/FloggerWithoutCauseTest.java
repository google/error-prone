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

/** {@link FloggerWithoutCause}Test */
@RunWith(JUnit4.class)
public class FloggerWithoutCauseTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerWithoutCause.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(Exception e, Throwable t) {",
            "    // BUG: Diagnostic contains: logger.atInfo().withCause(e).log(\"hello %s\", e);",
            "    logger.atInfo().log(\"hello %s\", e);",
            "    // BUG: Diagnostic contains: .atInfo().withCause(t).log(\"hello %s\", e, t);",
            "    logger.atInfo().log(\"hello %s\", e, t);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSubtype() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(ReflectiveOperationException e) {",
            "    // BUG: Diagnostic contains: logger.atInfo().withCause(e).log(\"hello %s\", e);",
            "    logger.atInfo().log(\"hello %s\", e);",
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
            "    logger.atInfo().log(null);",
            "    logger.atInfo().log(\"hello\");",
            "    logger.atInfo().log(\"hello %s\", 1);",
            "    logger.atInfo().withCause(e).log(\"hello %s\", e);",
            "    logger.atInfo().withCause(e).log(\"hello %s\", e, t);",
            "  }",
            "}")
        .doTest();
  }
}
