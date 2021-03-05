/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link XorPower}Test */
@RunWith(JUnit4.class)
public class XorPowerTest {
  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(XorPower.class, getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final int X = 2 ^ 16;",
            "  static final int Y = 2 ^ 32;",
            "  static final int Z = 2 ^ 31;",
            "  static final int P = 10 ^ 6;",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  static final int X = 1 << 16;",
            "  static final int Y = 2 ^ 32;",
            "  static final int Z = 1 << 31;",
            "  static final int P = 1000000;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(XorPower.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final int X = 2 ^ 0x16;",
            "  // BUG: Diagnostic contains:",
            "  static final int Y = 2 ^ 32;",
            "}")
        .doTest();
  }
}
