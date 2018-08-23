/*
 * Copyright 2018 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link FloatingPointAssertionWithinEpsilon} bug pattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class FloatingPointAssertionWithinEpsilonTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloatingPointAssertionWithinEpsilon.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceFile("FloatingPointAssertionWithinEpsilonPositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceFile("FloatingPointAssertionWithinEpsilonNegativeCases.java")
        .doTest();
  }

  @Test
  public void testFixes() {
    BugCheckerRefactoringTestHelper.newInstance(
            new FloatingPointAssertionWithinEpsilon(), getClass())
        .addInput("FloatingPointAssertionWithinEpsilonPositiveCases.java")
        .addOutput("FloatingPointAssertionWithinEpsilonPositiveCases_expected.java")
        .doTest(TestMode.AST_MATCH);
  }
}
