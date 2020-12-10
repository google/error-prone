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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author mariasam@google.com (Maria Sam) */
@RunWith(JUnit4.class)
public class FloggerRedundantIsEnabledTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(FloggerRedundantIsEnabled.class, getClass());

  @Test
  public void doPositiveCases() {
    compilationTestHelper.addSourceFile("FloggerRedundantIsEnabledPositiveCases.java").doTest();
  }

  @Test
  public void doNegativeCases() {
    compilationTestHelper.addSourceFile("FloggerRedundantIsEnabledNegativeCases.java").doTest();
  }

  @Test
  public void testFixes() {
    BugCheckerRefactoringTestHelper.newInstance(new FloggerRedundantIsEnabled(), getClass())
        .addInput("FloggerRedundantIsEnabledPositiveCases.java")
        .addOutput("FloggerRedundantIsEnabledPositiveCases_expected.java")
        .doTest(TestMode.AST_MATCH);
  }
}
