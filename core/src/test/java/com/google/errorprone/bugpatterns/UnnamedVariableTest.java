/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnnamedVariableTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(UnnamedVariable.class, getClass())
          .setArgs("-XepOpt:UnnamedVariable:OnlyRenameVariablesNamedUnused=false");
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnamedVariable.class, getClass())
          .setArgs("-XepOpt:UnnamedVariable:OnlyRenameVariablesNamedUnused=false");

  @Before
  public void setUp() {
    assume().that(Runtime.version().feature()).isAtLeast(22);
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  var unused = new Object();
                }
                var bar = new Object();
                bar = null;
                Function<String, Integer> f = unused -> 1;
                f = (String unused) -> 1;
                f = (unused) -> 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  var _ = new Object();
                }
                var bar = new Object();
                bar = null;
                Function<String, Integer> f = _ -> 1;
                f = (String _) -> 1;
                f = (_) -> 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                var _ = 42;
                int x = 1;
                x++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveAllNames() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                // BUG: Diagnostic contains:
                int x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeAllNames() {
    CompilationTestHelper.newInstance(UnnamedVariable.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                int x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void catchBlock() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                try {
                  throw new Exception();
                } catch (Exception e) {
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                try {
                  throw new Exception();
                } catch (Exception _) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void tryWithResources() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(AutoCloseable a) throws Exception {
                try (var c = a) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void forLoop() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> xs) {
                for (var x : xs) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> xs) {
                for (var _ : xs) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void patternVariable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean f(Object o) {
                return switch (o) {
                  case String unused -> true;
                  default -> false;
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              boolean f(Object o) {
                return switch (o) {
                  case String _ -> true;
                  default -> false;
                };
              }
            }
            """)
        .doTest();
  }
}
