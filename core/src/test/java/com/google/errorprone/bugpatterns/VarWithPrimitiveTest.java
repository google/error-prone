/*
 * Copyright 2026 Google Inc. All Rights Reserved.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VarWithPrimitive}. */
@RunWith(JUnit4.class)
public final class VarWithPrimitiveTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(VarWithPrimitive.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(VarWithPrimitive.class, getClass());

  // from https://openjdk.org/projects/amber/guides/lvti-style-guide#G7
  @Test
  public void lvtiExamples() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                var flags = 0;
                var mask = 0x7fff;
                var base = 17;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                int flags = 0;
                int mask = 0x7fff;
                int base = 17;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void intLiteral() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                var x = 10;
                var y = -5;
                var z = (0);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                int x = 10;
                int y = -5;
                int z = (0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void otherPrimitiveNumericLiterals() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                var a = 10L;
                var b = 1.0;
                var c = 1.0f;
                var d = -10L;
                var e = -1.0;
                var f = -1.0f;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                long a = 10L;
                double b = 1.0;
                float c = 1.0f;
                long d = -10L;
                double e = -1.0;
                float f = -1.0f;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonNumericLiterals() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                var a = true;
                var b = 'x';
                var c = "hello";
                var d = (String) null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                boolean a = true;
                char b = 'x';
                var c = "hello";
                var d = (String) null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonLiterals() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t(int arg) {
                var a = arg;
                var b = getInt();
                var c = 1 + 2;
                var d = getAgeAsLong();
              }

              int getInt() {
                return 0;
              }

              long getAgeAsLong() {
                return 0L;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public void t(int arg) {
                int a = arg;
                int b = getInt();
                int c = 1 + 2;
                long d = getAgeAsLong();
              }

              int getInt() {
                return 0;
              }

              long getAgeAsLong() {
                return 0L;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enhancedForLoop() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void t() {
                int[] arr = {1, 2, 3};
                for (var x : arr) {
                  System.out.println(x);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void t() {
                int[] arr = {1, 2, 3};
                for (int x : arr) {
                  System.out.println(x);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void forLoopInitializer() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void t() {
                for (var i = 0; i < 10; i++) {
                  System.out.println(i);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void t() {
                for (int i = 0; i < 10; i++) {
                  System.out.println(i);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void implicitLambdaParameter_noMatch() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            import java.util.stream.IntStream;
            class Test {
              void foo() {
                byte[] bar = new byte[6];
                Map<Byte, List<Byte>> indicesMap =
                    IntStream.range(0, 6)
                        .mapToObj(n -> n)
                        .collect(
                            Collectors.groupingBy(
                                n -> bar[n],
                                Collectors.mapping(
                                    n -> (byte) n.intValue(), Collectors.toList())));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void varLambdaParam() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.IntUnaryOperator;
            class Test {
              void t() {
                IntUnaryOperator op = (var n) -> n * 2;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.IntUnaryOperator;
            class Test {
              void t() {
                IntUnaryOperator op = (int n) -> n * 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotatedVarLambdaParam() {
    refactoringHelper
        .addInputLines("A.java", "@interface A { int var() default 0; }")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import java.util.function.IntUnaryOperator;
            class Test {
              void t() {
                IntUnaryOperator op = (@A(var = 0) var n) -> n * 2;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.IntUnaryOperator;
            class Test {
              void t() {
                IntUnaryOperator op = (@A(var = 0) int n) -> n * 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotatedVarLocalVariable_beforeJdk26() {
    // Before JDK 26 there is no AST node for the var type, so the token scan finds two 'var'
    // tokens (@A(var=0) attribute + type keyword) and safely returns no fix.
    assume().that(Runtime.version().feature()).isLessThan(26);
    refactoringHelper
        .addInputLines("A.java", "@interface A { int var() default 0; }")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void t() {
                @A(var = 0) var n = 5;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void annotatedVarLocalVariable_jdk26AndAbove() {
    // On JDK 26+ there is an AST node for the var type with source positions, so hasExplicitSource
    // returns true and the replacement targets the type keyword directly, preserving the
    // annotation.
    assume().that(Runtime.version().feature()).isAtLeast(26);
    refactoringHelper
        .addInputLines("A.java", "@interface A { int var() default 0; }")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void t() {
                @A(var = 0) var n = 5;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void t() {
                @A(var = 0) int n = 5;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multiParamImplicitLambda_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.IntBinaryOperator;
            class Test {
              void t() {
                IntBinaryOperator op = (a, b) -> a + b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void explicitLambdaParam_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.IntUnaryOperator;
            class Test {
              void t() {
                IntUnaryOperator op = (int n) -> n * 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void explicitType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void t() {
                int x = 10;
                long y = 10;
                byte z = 0;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
