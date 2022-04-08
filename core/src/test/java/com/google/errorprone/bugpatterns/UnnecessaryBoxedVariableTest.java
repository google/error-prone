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

/**
 * @author awturner@google.com (Andy Turner)
 */
@RunWith(JUnit4.class)
public class UnnecessaryBoxedVariableTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryBoxedVariable.class, getClass());
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(UnnecessaryBoxedVariable.class, getClass());

  @Test
  public void testCases() {
    helper
        .addInput("testdata/UnnecessaryBoxedVariableCases.java")
        .addOutput("testdata/UnnecessaryBoxedVariableCases_expected.java")
        .doTest();
  }

  @Test
  public void suppression() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"UnnecessaryBoxedVariable\")",
            "  private int a(Integer o) {",
            "    return o;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void lambdas() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface Boxed<O> { void a(O b); }",
            "  void boxed(Boxed<?> b) {}",
            "  private void test() {",
            "    boxed((Double a) -> { double b = a + 1; });",
            "  }",
            "}")
        .doTest();
  }
}
