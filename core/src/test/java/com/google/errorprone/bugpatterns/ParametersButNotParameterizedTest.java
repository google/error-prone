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

/** Tests for {@link ParametersButNotParameterized}. */
@RunWith(JUnit4.class)
public final class ParametersButNotParameterizedTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ParametersButNotParameterized.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ParametersButNotParameterized(), getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.runners.Parameterized.Parameter;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Parameter public int foo;",
            "}")
        .addOutputLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.runners.Parameterized;",
            "import org.junit.runners.Parameterized.Parameter;",
            "@RunWith(Parameterized.class)",
            "public class Test {",
            "  @Parameter public int foo;",
            "}")
        .doTest();
  }

  @Test
  public void alreadyParameterized_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.Parameterized;",
            "import org.junit.runners.Parameterized.Parameter;",
            "@RunWith(Parameterized.class)",
            "public class Test {",
            "  @Parameter public int foo;",
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
