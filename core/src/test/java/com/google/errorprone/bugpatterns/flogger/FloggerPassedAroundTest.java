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

/** Tests for {@link FloggerPassedAround}. */
@RunWith(JUnit4.class)
public final class FloggerPassedAroundTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(FloggerPassedAround.class, getClass());

  @Test
  public void loggerAcceptedAsParameter() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  public void test(FluentLogger logger) {}",
            "}")
        .doTest();
  }

  @Test
  public void loggerAcceptedAsParameterToConstructor() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  public Test(FluentLogger logger) {}",
            "}")
        .doTest();
  }

  @Test
  public void nonLoggerParameter() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.common.flogger.FluentLogger;",
            "class Test {",
            "  public Test(int a) {}",
            "}")
        .doTest();
  }
}
