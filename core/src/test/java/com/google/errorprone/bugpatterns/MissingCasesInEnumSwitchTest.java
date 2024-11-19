/*
 * Copyright 2014 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MissingCasesInEnumSwitch}Test */
@RunWith(JUnit4.class)
public class MissingCasesInEnumSwitchTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingCasesInEnumSwitch.class, getClass());

  @Test
  public void exhaustive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                switch (c) {
                  case ONE:
                  case TWO:
                  case THREE:
                    System.err.println("found it!");
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void exhaustive_allowsQualifying() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                switch (c) {
                  case Case.ONE:
                  case Case.TWO:
                  case Case.THREE:
                    System.err.println("found it!");
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void exhaustive_multipleCaseExpressions() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO
              }

              void m(Case c) {
                switch (c) {
                  case ONE, TWO -> {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_withDefault() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                switch (c) {
                  case ONE:
                  case TWO:
                    System.err.println("found it!");
                    break;
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                // BUG: Diagnostic contains: THREE
                switch (c) {
                  case ONE:
                  case TWO:
                    System.err.println("found it!");
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_manyCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE,
                FOUR,
                FIVE,
                SIX,
                SEVEN,
                EIGHT
              }

              void m(Case c) {
                // BUG: Diagnostic contains: TWO, THREE, FOUR, and 4 others
                switch (c) {
                  case ONE:
                    System.err.println("found it!");
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_nonEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                switch (i) {
                  case 1:
                  case 2:
                    System.err.println("found it!");
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void empty() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO
              }

              void m(Case e) {
                // BUG: Diagnostic contains: ONE, TWO
                switch (e) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_arrowStatement() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO
              }

              void m(Case c) {
                // BUG: Diagnostic contains: TWO
                switch (c) {
                  case ONE -> {
                    System.err.println("found it!");
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_multi() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                // BUG: Diagnostic contains: THREE
                switch (c) {
                  case ONE, TWO:
                    System.err.println("found it!");
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonExhaustive_multiArrow() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Case {
                ONE,
                TWO,
                THREE
              }

              void m(Case c) {
                // BUG: Diagnostic contains: THREE
                switch (c) {
                  case ONE, TWO -> {
                    System.err.println("found it!");
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void i4684() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    compilationHelper
        .addSourceLines(
            "ErrorProneBug.java",
            """
            public class ErrorProneBug {
              enum A {
                A1,
                A2,
                A3
              }

              public static void main(String[] args) {
                A a = null;

                switch (a) {
                  case null -> {
                    System.out.println("null");
                  }
                  case A1 -> {
                    System.out.println("A1");
                  }
                  case A2 -> {
                    System.out.println("A2");
                  }
                  case A3 -> {
                    System.out.println("A3");
                  }
                }
              }
            }
            """)
        .doTest();
  }
}
