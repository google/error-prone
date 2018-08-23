/*
 * Copyright 2017 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author @mariasam (Maria Sam) on 7/6/17. */
@RunWith(JUnit4.class)
public class MultipleParallelOrSequentialCallsTest {

  CompilationTestHelper compilationTestHelper;

  @Before
  public void setup() {
    compilationTestHelper =
        CompilationTestHelper.newInstance(MultipleParallelOrSequentialCalls.class, getClass());
  }

  @Test
  public void positiveCases() {
    compilationTestHelper
        .addSourceFile("MultipleParallelOrSequentialCallsPositiveCases.java")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationTestHelper
        .addSourceFile("MultipleParallelOrSequentialCallsNegativeCases.java")
        .doTest();
  }

  @Test
  public void testFixes() {
    BugCheckerRefactoringTestHelper.newInstance(new MultipleParallelOrSequentialCalls(), getClass())
        .addInput("MultipleParallelOrSequentialCallsPositiveCases.java")
        .addOutput("MultipleParallelOrSequentialCallsPositiveCases_expected.java")
        .doTest(TestMode.AST_MATCH);
  }
}
