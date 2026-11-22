/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ThreadSafeRefactoringTest {
  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(ThreadSafeRefactoring.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "}")
        .doTest();
  }

  @Test
  public void someImmutableSomeNot() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  int a = 42;",
            "  @ThreadSafe static class Inner {",
            "    final int a = 43;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "// This class was annotated with javax.annotation.concurrent.ThreadSafe, but didn't"
                + " seem to be provably thread safe.",
            "class Test {",
            "  int a = 42;",
            "  @ThreadSafe ",
            "  static class Inner {",
            "    final int a = 43;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  int a = 42;",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "// This class was annotated with javax.annotation.concurrent.ThreadSafe, but didn't"
                + " seem to be provably thread safe.",
            "class Test {",
            "  int a = 42;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative_multipleClasses() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  int a = 42;",
            "  @ThreadSafe static class Inner {",
            "    int a = 43;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "// This class was annotated with javax.annotation.concurrent.ThreadSafe, but didn't"
                + " seem to be provably thread safe.",
            "class Test {",
            "  int a = 42;",
            "  // This class was annotated with javax.annotation.concurrent.ThreadSafe, but didn't"
                + " seem to be provably thread safe.",
            "  static class Inner {",
            "    int a = 43;",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }
}
