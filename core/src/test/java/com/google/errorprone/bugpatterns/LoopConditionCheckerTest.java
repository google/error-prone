/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LoopConditionChecker}Test */
@RunWith(JUnit4.class)
public class LoopConditionCheckerTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(LoopConditionChecker.class, getClass());

  @Test
  public void positive() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int h9() {
                int sum = 0;
                int i, j;
                for (i = 0; i < 10; ++i) {
                  // BUG: Diagnostic contains:
                  for (j = 0; j < 10; ++i) {
                    sum += j;
                  }
                }
                return sum;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_noUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                // BUG: Diagnostic contains:
                for (int i = 0; i < 10; ) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int h9() {
                int sum = 0;
                int i, j;
                for (i = 0; i < 10; ++i) {
                  for (j = 0; j < 10; ++i) {
                    sum += j;
                    j++;
                  }
                }
                return sum;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_forExpression() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (int i = 0; i < 10; i++) {
                  System.err.println(i);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_noVariable() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Iterator;

            class Test {
              void f(Iterable<String> xs) {
                Iterator<String> it = xs.iterator();
                while (it.hasNext()) {
                  System.err.println(it.next());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_noCondition() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (; ; ) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_noUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (int i = 0; i < 10; ) {
                  i++;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_conditionUpdate() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                int i = 0;
                while (i++ < 10) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compileTimeConstantBounds_areEquivalentToLiterals() {
    compilationTestHelper
        .expectErrorMessage(
            "ONLY_I",
            message ->
                message.contains("condition variable(s) never modified in loop body: i")
                    && !message.contains("i,"))
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static final int ZERO = 0;
              static final int ONE = 1;
              static final boolean TRUE = true;
              static final boolean FALSE = false;

              void sink() {}

              void f() {
                final int localOne = 1;
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; i < 1; sink()) {}
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; i < localOne; sink()) {}
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; i < ONE; sink()) {}
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; i < ZERO; sink()) {}
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; TRUE && i < ONE; sink()) {}
                // BUG: Diagnostic matches: ONLY_I
                for (int i = 0; FALSE || i < ONE; sink()) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compileTimeConstantExpressions() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Bounds {
              static final int ZERO = 0;
              static final int ONE = 1;
              static final int TWO = ONE + 1;
            }

            class Test {
              static final boolean TRUE = true;

              void sink() {}

              void f() {
                // BUG: Diagnostic contains:
                for (int i = 0; i < Bounds.ONE; sink()) {}
                // BUG: Diagnostic contains:
                for (int i = 0; i < Bounds.TWO; sink()) {}
                // BUG: Diagnostic contains:
                for (int i = 0; i < Bounds.ONE + 1; sink()) {}
                // BUG: Diagnostic contains:
                for (int i = 0; i < (int) Bounds.ONE; sink()) {}
                // BUG: Diagnostic contains:
                for (int i = 0; i < (TRUE ? Bounds.ONE : Bounds.ZERO); sink()) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonConstantFieldsRemainOutOfScope() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static final Integer BOXED_ONE = 1;
              static final int RUNTIME_ONE = Integer.parseInt("1");
              static final Boolean BOXED_TRUE = true;
              static final boolean RUNTIME_TRUE = Boolean.parseBoolean("true");
              static final boolean RUNTIME_FALSE = Boolean.parseBoolean("false");
              static int mutableOne = 1;
              static boolean mutableTrue = true;
              static boolean mutableFalse = false;

              void sink() {}

              void f() {
                for (int i = 0; i < BOXED_ONE; sink()) {}
                for (int i = 0; i < RUNTIME_ONE; sink()) {}
                for (int i = 0; i < mutableOne; sink()) {}
                for (int i = 0; BOXED_TRUE && i < 1; sink()) {}
                for (int i = 0; RUNTIME_TRUE && i < 1; sink()) {}
                for (int i = 0; RUNTIME_FALSE || i < 1; sink()) {}
                for (int i = 0; mutableTrue && i < 1; sink()) {}
                for (int i = 0; mutableFalse || i < 1; sink()) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_field() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int i = 0;

              void f() {
                while (i < 10) {
                  g();
                }
              }

              void g() {
                i++;
              }
            }
            """)
        .doTest();
  }
}
