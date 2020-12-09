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

/** Tests for {@link FloggerLogWithCause}. */
@RunWith(JUnit4.class)
public final class FloggerLogWithCauseTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerLogWithCause.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void test() {",
            "    try {}",
            "    catch (Exception e) {",
            "      // BUG: Diagnostic contains: logger.atWarning().withCause(e).log",
            "      logger.atWarning().log(\"failed\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void loggedAtInfo_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void test() {",
            "    try {}",
            "    catch (Exception e) {",
            "      logger.atInfo().log(\"failed\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void alreadyLoggedWithCause() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void test() {",
            "    try {}",
            "    catch (Exception e) {",
            "      logger.atWarning().withCause(e).log(\"failed\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variableUsedInOtherWay() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void test() {",
            "    try {}",
            "    catch (Exception e) {",
            "      logger.atWarning().log(e.getMessage());",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void test() {",
            "    try {}",
            "    catch (@SuppressWarnings(\"FloggerLogWithCause\") Exception e) {",
            "      logger.atWarning().log(e.getMessage());",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
