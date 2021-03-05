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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link StaticMockMember} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class StaticMockMemberTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(StaticMockMember.class, getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StaticMockMember.class, getClass());

  @Test
  public void testPositiveCases() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mock;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Mock private static String mockedPrivateString;",
            "  @Mock static String mockedString;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mock;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Mock private String mockedPrivateString;",
            "  @Mock String mockedString;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mock;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Mock private String mockedPrivateString;",
            "  @Mock String mockedString;",
            "}")
        .doTest();
  }

  @Test
  public void memberSuppression() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mock;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @SuppressWarnings(\"StaticMockMember\")",
            "  @Mock private static String mockedPrivateString;",
            "}")
        .doTest();
  }

  @Test
  public void flagIfRemovingStaticWontCompile() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mock;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: StaticMockMember",
            "  @Mock private static String mockedPrivateString;",
            "  static String someStaticMethod() {",
            "    return mockedPrivateString;",
            "  }",
            "}")
        .doTest();
  }
}
