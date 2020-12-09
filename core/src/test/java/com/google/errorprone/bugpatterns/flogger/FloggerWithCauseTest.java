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

/** Tests for {@link FloggerWithCause} */
@RunWith(JUnit4.class)
public class FloggerWithCauseTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerWithCause.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "import java.io.IOException;",
            "class Test {",
            "  abstract static class MyException extends IOException {",
            "    abstract public String foo();",
            "  }",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(Exception e, MyException e2) {",
            "    // BUG: Diagnostic contains:"
                + " logger.atSevere().withStackTrace(MEDIUM).log(\"msg\");",
            "    logger.atSevere().withCause(new Error()).log(\"msg\");",
            "    logger.atSevere().withCause(new Error(e2.foo())).log(\"msg\");",
            "    FluentLogger.Api severeLogger = logger.atSevere();",
            "    // BUG: Diagnostic contains:"
                + " severeLogger.withStackTrace(MEDIUM).log(\"message\");",
            "    severeLogger.withCause(new IllegalArgumentException()).log(\"message\");",
            "    // BUG: Diagnostic contains:",
            "    // logger.atSevere().withCause(e).log(\"message\");",
            "    // logger.atSevere().withStackTrace(MEDIUM).withCause(e).log(\"message\");",
            "    logger.atSevere().withCause(new Throwable(e)).log(\"message\");",
            "    // BUG: Diagnostic contains:",
            "    // logger.atSevere().withCause(e).log(\"message\");",
            "    // logger.atSevere().withStackTrace(MEDIUM).withCause(e).log(\"message\");",
            "    logger.atSevere().withCause(new SecurityException(e)).log(\"message\");",
            "    // BUG: Diagnostic contains:",
            "    // logger.atSevere().withCause(e).log(\"msg\");",
            "    // logger.atSevere().withStackTrace(MEDIUM).withCause(e).log(\"msg\");",
            "    logger.atSevere().withCause(new"
                + " NumberFormatException(e.getMessage())).log(\"msg\");",
            "    // BUG: Diagnostic contains:",
            "    // logger.atSevere().withCause(e).log(\"message\");",
            "    // logger.atSevere().withStackTrace(MEDIUM).withCause(e).log(\"message\");",
            "    logger.atSevere().withCause(new Exception(e.toString())).log(\"message\");",
            "    // BUG: Diagnostic contains:",
            "    // logger.atSevere().withCause(e.getCause()).log(\"message\");",
            "    //"
                + " logger.atSevere().withStackTrace(MEDIUM).withCause(e.getCause()).log(\"message\");",
            "    logger.atSevere().withCause(new RuntimeException(e.getCause())).log(\"message\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.flogger.StackSize.FULL;",
            "import static com.google.common.flogger.StackSize.MEDIUM;",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(Exception e) {",
            "    logger.atSevere().log(null);",
            "    logger.atSevere().log(\"hello\");",
            "    logger.atSevere().log(\"hello %d\", 1);",
            "    logger.atSevere().withCause(e).log(\"some log message\");",
            "    logger.atSevere().withStackTrace(FULL).log(\"some log message\");",
            "    logger.atSevere().withStackTrace(MEDIUM).withCause(e).log(\"some log message\");",
            "    logger.atSevere().withCause(new NumberFormatException()).log(\"message\");",
            "  }",
            "}")
        .doTest();
  }

  // regression test for http://b/29131466
  @Test
  public void breakBeforeWithCause() {
    BugCheckerRefactoringTestHelper.newInstance(new FloggerWithCause(), getClass())
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(Exception e) {",
            "    logger.atSevere()",
            "        .withCause(new IllegalArgumentException())",
            "        .log(\"message\");",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.flogger.StackSize.MEDIUM;",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(Exception e) {",
            "    logger.atSevere().withStackTrace(MEDIUM).log(\"message\");",
            "  }",
            "}")
        .doTest();
  }
}
