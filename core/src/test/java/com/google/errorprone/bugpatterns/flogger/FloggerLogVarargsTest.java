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

/** Tests for {@link FloggerLogVarargs}. */
@RunWith(JUnit4.class)
public final class FloggerLogVarargsTest {
  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new FloggerLogVarargs(), getClass())
        .addInputLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  void log(String s, Object... a) {",
            "    logger.atInfo().log(s, a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  void log(String s, Object... a) {",
            "    logger.atInfo().logVarargs(s, a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveAnonymousClass() {
    BugCheckerRefactoringTestHelper.newInstance(new FloggerLogVarargs(), getClass())
        .addInputLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  Predicate<Void> log(String s, Object... a) {",
            "    return new Predicate<Void>() {",
            "      @Override public boolean test(Void unused) {",
            "        logger.atInfo().log(s, a);",
            "        return true;",
            "      }",
            "    };",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  Predicate<Void> log(String s, Object... a) {",
            "    return new Predicate<Void>() {",
            "      @Override public boolean test(Void unused) {",
            "        logger.atInfo().logVarargs(s, a);",
            "        return true;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(FloggerLogVarargs.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  void log(String s, Object a) {",
            "    logger.atInfo().log(s, a);",
            "  }",
            "  void bar() {",
            "    logger.atInfo().log(\"foo\", new Object[] {1});",
            "  }",
            "}")
        .doTest();
  }
}
