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
            import java.util.List;
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  int unused = 1;
                  int unused1 = 2;
                  String unused2 = "hello";
                }
                Object bar = new Object();
                bar = null;
                Function<String, Integer> f = unused -> 1;
                f = (String unused) -> 1;
                f = (unused) -> 1;
              }

              public void foo(List<String> list) {
                list.forEach(unused -> System.out.println());
                list.forEach((String unused1) -> System.out.println());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  var _ = 1;
                  var _ = 2;
                  var _ = "hello";
                }
                Object bar = new Object();
                bar = null;
                Function<String, Integer> f = _ -> 1;
                f = (_) -> 1;
                f = (_) -> 1;
              }

              public void foo(List<String> list) {
                list.forEach(_ -> System.out.println());
                list.forEach((String _) -> System.out.println());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    refactoringHelper
        .addInputLines(
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
        .expectUnchanged()
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
            import java.io.IOException;

            class Test {
              void f() {
                try {
                  throw new IOException();
                } catch (IOException ioe) {
                } catch (Exception e) {
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.IOException;

            class Test {
              void f() {
                try {
                  throw new IOException();
                } catch (IOException _) {
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
              void f1(AutoCloseable a) throws Exception {
                try (AutoCloseable c = a) {}
              }

              void f2(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f1(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }

              void f2(AutoCloseable a) throws Exception {
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
                for (Integer x : xs) {}
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

  @Test
  public void declarationWithoutInitializer_noVar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String unused;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void localVariable_namedUnusedButActuallyUsed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public void foo() {
                String unused = "Hello world!";
                System.out.println(unused);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void traditionalForLoop_unused() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (int i = 0; ; ) {
                  break;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (var _ = 0; ; ) {
                  break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariable_initializedToNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String unused = null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String _ = null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void alreadyVar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                var unused = 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                var _ = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleDeclarations() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int unused = 1, unused2 = 2;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int _ = 1, _ = 2;
              }
            }
            """)
        .doTest();
  }
}
