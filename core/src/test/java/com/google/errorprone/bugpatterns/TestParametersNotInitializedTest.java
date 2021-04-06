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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TestParametersNotInitialized}. */
@RunWith(JUnit4.class)
public final class TestParametersNotInitializedTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TestParametersNotInitialized.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(TestParametersNotInitialized.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @TestParameter public boolean foo;",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "import com.google.testing.junit.testparameterinjector.TestParameterInjector;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(TestParameterInjector.class)",
            "public class Test {",
            "  @TestParameter public boolean foo;",
            "}")
        .doTest();
  }

  @Test
  public void onlyFlagsJunit4Runner() {
    refactoringHelper
        .addInputLines(
            "MyRunner.java",
            "import org.junit.runners.BlockJUnit4ClassRunner;",
            "import org.junit.runners.model.InitializationError;",
            "public final class MyRunner extends BlockJUnit4ClassRunner {",
            "  public MyRunner(Class<?> testClass) throws InitializationError {",
            "    super(testClass);",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(MyRunner.class)",
            "public class Test {",
            "  @TestParameter public boolean foo;",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void alreadyParameterized_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "import com.google.testing.junit.testparameterinjector.TestParameterInjector;",
            "import org.junit.runner.RunWith;",
            "@RunWith(TestParameterInjector.class)",
            "public class Test {",
            "  @TestParameter public boolean foo;",
            "}")
        .doTest();
  }

  @Test
  public void noParameters_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "}")
        .doTest();
  }
}
