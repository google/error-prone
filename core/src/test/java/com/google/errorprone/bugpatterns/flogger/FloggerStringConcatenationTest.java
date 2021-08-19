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

/** {@link FloggerStringConcatenation}Test */
@RunWith(JUnit4.class)
public class FloggerStringConcatenationTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(FloggerStringConcatenation.class, getClass());

  @Test
  public void fix() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String CONSTANT = \"constant\";",
            "  public void method(String world, int i, long l, float f, double d, boolean b) {",
            "    logger.atInfo().log(\"hello \" + world + i + l + f + (d + \"\" + b) + CONSTANT);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String CONSTANT = \"constant\";",
            "  public void method(String world, int i, long l, float f, double d, boolean b) {",
            "    logger.atInfo().log(\"hello %s%d%d%g%g%s%s\", world, i, l, f, d, b, CONSTANT);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constant() {
    CompilationTestHelper.newInstance(FloggerStringConcatenation.class, getClass())
        .addSourceLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  private static final String CONSTANT = \"constant\";",
            "  public void method() {",
            "    logger.atInfo().log(CONSTANT + \"hello\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void minus() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(String world, int i) {",
            "    logger.atInfo().log(\"hello \" + world + \" \" + (i - 1));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(String world, int i) {",
            "    logger.atInfo().log(\"hello %s %d\", world, i - 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void numericOps() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(int x, int y) {",
            "    logger.atInfo().log(x + y + \" sum; mean \" + (x + y) / 2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  private static final FluentLogger logger = FluentLogger.forEnclosingClass();",
            "  public void method(int x, int y) {",
            "    logger.atInfo().log(\"%d sum; mean %d\", x + y, (x + y) / 2);",
            "  }",
            "}")
        .doTest();
  }
}
