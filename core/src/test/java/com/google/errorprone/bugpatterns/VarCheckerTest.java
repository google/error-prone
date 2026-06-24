/*
 * Copyright 2015 The Error Prone Authors.
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
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class VarCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(VarChecker.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(VarChecker.class, getClass());

  // fields are ignored
  @Test
  public void nonFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int x = 42;
            }
            """)
        .doTest();
  }

  @Test
  public void finalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public final int x = 42;
            }
            """)
        .doTest();
  }

  @Test
  public void positiveParam() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void x(int y) {
                y++;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              public void x(@Var int y) {
                y++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void x(int y) {}
            }
            """)
        .doTest();
  }

  @Test
  public void positiveLocal() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                int y = 0;
                y++;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              public void x() {
                @Var int y = 0;
                y++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void x() {
                int y = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalLocal7() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void x() {
                final int y = 0;
              }
            }
            """)
        .setArgs(Arrays.asList("-source", "7", "-target", "7"))
        .doTest();
  }

  @Test
  public void finalLocal8() {
    BugCheckerRefactoringTestHelper.newInstance(VarChecker.class, getClass())
        .setArgs("-source", "8", "-target", "8")
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                final int y = 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                int y = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void forLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              {
                for (int i = 0; i < 10; i++) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enhancedFor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(Iterable<String> xs) {
                for (String x : xs) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unnecessaryFinalNativeMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              native void f(final int y);
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              native void f(int y);
            }
            """)
        .doTest();
  }

  @Test
  public void nativeMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              native void f(int y);
            }
            """)
        .doTest();
  }

  @Test
  public void abstractMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void f(int y);
            }
            """)
        .doTest();
  }

  @Test
  public void interfaceMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            interface Test {
              void f(int y);
            }
            """)
        .doTest();
  }

  @Test
  public void varField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              @Var public int x = 42;
            }
            """)
        .doTest();
  }

  @Test
  public void varParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              public void x(@Var int y) {
                y++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void varLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              public void x() {
                @Var int y = 0;
                y++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void varCatch() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                try {
                } catch (Exception e) {
                  e = null;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              public void x() {
                try {
                } catch (@Var Exception e) {
                  e = null;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalCatch() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                try {
                } catch (final Exception e) {
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void x() {
                try {
                } catch (Exception e) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalTWR() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.io.InputStream;

            class Test {
              public void x() {
                try (final InputStream is = null) {
                } catch (Exception e) {
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.InputStream;

            class Test {
              public void x() {
                try (InputStream is = null) {
                } catch (Exception e) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonFinalTWR() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.io.InputStream;

            class Test {
              public void x() {
                try (InputStream is = null) {
                } catch (Exception e) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void receiverParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void f(Test this, int x) {
                this.toString();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressedByGeneratedAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.processing.Generated;

            @Generated("generator")
            class Test {
              public void x() {
                final int y = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressedBySuppressWarningsAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            @SuppressWarnings("Var")
            class Test {
              public void x() {
                final int y = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void notSuppressedByUnrelatedSuppressWarningsAnnotation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            @SuppressWarnings("Foo")
            class Test {
              public void x() {
                final int y = 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            @SuppressWarnings("Foo")
            class Test {
              public void x() {
                int y = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void effectivelyFinal() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              int f(@Var int x, @Var int y) {
                y++;
                return x + y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            class Test {
              int f(int x, @Var int y) {
                y++;
                return x + y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordCanonicalConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public record Test(String x) {
              public Test {
                x = x.replace('_', ' ');
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Var;

            public record Test(@Var String x) {
              public Test {
                x = x.replace('_', ' ');
              }
            }
            """)
        .doTest();
  }
}
