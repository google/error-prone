/*
 * Copyright 2021 The Error Prone Authors.
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

/** {@link FloggerArgumentToString}Test */
@RunWith(JUnit4.class)
public class FloggerArgumentToStringTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FloggerArgumentToString.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new FloggerArgumentToString(), getClass());

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.base.Ascii;",
            "import com.google.common.flogger.FluentLogger;",
            "import java.util.Arrays;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(String world, Object[] xs, long l) {",
            "    logger.atInfo().log(\"hello '%s'\", world.toString());",
            "    logger.atInfo().log(\"hello %s %d\", world.toString(), 2);",
            "    logger.atInfo().log(\"hello %s\", world.toUpperCase());",
            "    logger.atInfo().log(\"hello %s\", Ascii.toUpperCase(world));",
            "    logger.atInfo().log(\"hello %s\", Integer.toString(42));",
            "    logger.atInfo().log(\"hello %d\", Integer.valueOf(42));",
            "    logger.atInfo().log(\"hello %s\", Integer.toHexString(42));",
            "    logger.atInfo().log(\"hello %S\", Integer.toHexString(42));",
            "    logger.atInfo().log(\"hello %s\", Arrays.asList(1, 2));",
            "    logger.atInfo().log(\"hello %s\", Arrays.asList(xs));",
            "    logger.atInfo().log(\"hello %s\", Long.toHexString(l));",
            "    logger.atInfo().log(\"%%s\", Ascii.toUpperCase(world));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Ascii;",
            "import com.google.common.flogger.FluentLogger;",
            "import java.util.Arrays;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(String world, Object[] xs, long l) {",
            "    logger.atInfo().log(\"hello '%s'\", world);",
            "    logger.atInfo().log(\"hello %s %d\", world, 2);",
            "    logger.atInfo().log(\"hello %S\", world);",
            "    logger.atInfo().log(\"hello %S\", world);",
            "    logger.atInfo().log(\"hello %d\", 42);",
            "    logger.atInfo().log(\"hello %d\", 42);",
            "    logger.atInfo().log(\"hello %x\", 42);",
            "    logger.atInfo().log(\"hello %X\", 42);",
            "    logger.atInfo().log(\"hello %s\", Arrays.asList(1, 2));",
            "    logger.atInfo().log(\"hello %s\", xs);",
            "    logger.atInfo().log(\"hello %x\", l);",
            "    logger.atInfo().log(\"%%s\", Ascii.toUpperCase(world));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void f(String world) {",
            "    logger.atInfo().log(\"hello '%s'\", world);",
            "  }",
            "}")
        .doTest();
  }
}
