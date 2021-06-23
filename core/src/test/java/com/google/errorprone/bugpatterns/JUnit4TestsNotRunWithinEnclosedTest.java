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

/** Unit tests for {@link JUnit4TestsNotRunWithinEnclosed}. */
@RunWith(JUnit4.class)
public final class JUnit4TestsNotRunWithinEnclosedTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JUnit4TestsNotRunWithinEnclosed.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(
          JUnit4TestsNotRunWithinEnclosed.class, getClass());

  @Test
  public void nonEnclosedRunner_testPresumedToRun() {
    helper
        .addSourceLines(
            "FooTest.java",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public final class FooTest {",
            "  @Test public void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void enclosingRunner_doesNotRunEnclosedTests() {
    helper
        .addSourceLines(
            "FooTest.java",
            "import org.junit.Test;",
            "import org.junit.experimental.runners.Enclosed;",
            "import org.junit.runner.RunWith;",
            "@RunWith(Enclosed.class)",
            "public final class FooTest {",
            "  // BUG: Diagnostic contains:",
            "  @Test public void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_changesToUseJunitRunner() {
    refactoring
        .addInputLines(
            "FooTest.java",
            "import org.junit.Test;",
            "import org.junit.experimental.runners.Enclosed;",
            "import org.junit.runner.RunWith;",
            "@RunWith(Enclosed.class)",
            "public final class FooTest {",
            "  @Test public void test() {}",
            "}")
        .addOutputLines(
            "FooTest.java",
            "import org.junit.Test;",
            "import org.junit.experimental.runners.Enclosed;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public final class FooTest {",
            "  @Test public void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void enclosingRunner_butWithClassExtended_doesRunTests() {
    helper
        .addSourceLines(
            "FooTest.java",
            "import org.junit.experimental.runners.Enclosed;",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "@RunWith(Enclosed.class)",
            "public class FooTest {",
            "  @Test public void test() {}",
            "  public class FooInnerTest extends FooTest {}",
            "}")
        .doTest();
  }

  @Test
  public void enclosingRunner_withInnerClasses_runsTests() {
    helper
        .addSourceLines(
            "FooTest.java",
            "import org.junit.experimental.runners.Enclosed;",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "@RunWith(Enclosed.class)",
            "public class FooTest {",
            "  public static class BarTest {",
            "    @Test public void test() {}",
            "  }",
            "}")
        .doTest();
  }
}
