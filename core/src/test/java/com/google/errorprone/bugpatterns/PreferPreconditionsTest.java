/*
 * Copyright 2026 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PreferPreconditions}. */
@RunWith(JUnit4.class)
public final class PreferPreconditionsTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(PreferPreconditions.class, getClass());

  @Test
  public void simpleIae() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(String s) {
                checkArgument(!s.isEmpty());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iaeWithNotNullSuggestion() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s == null) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(String s) {
                checkArgument(s != null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void npeWithNotNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s == null) {
                  throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkNotNull;

            class Test {
              void m(String s) {
                checkNotNull(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iaeWithMessage() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i <= 0) {
                  throw new IllegalArgumentException("must be positive");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(int i) {
                checkArgument(i > 0, "must be positive");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iaeWithFormat() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i <= 0) {
                  throw new IllegalArgumentException(String.format("expected > 0, got %s", i));
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(int i) {
                checkArgument(i > 0, "expected > 0, got %s", i);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iseOnParam() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i != 42) {
                  throw new IllegalStateException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkState;

            class Test {
              void m(int i) {
                checkState(i == 42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void notAParameter() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private int field;

              void m() {
                if (field == 0) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void hasElse() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i == 0) {
                  throw new IllegalArgumentException();
                } else {
                  System.out.println(i);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negatedCondition() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int a, int b) {
                if (!(a == b)) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(int a, int b) {
                checkArgument(a == b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iseWithNotNullSuggestion() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s == null) {
                  throw new IllegalStateException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkState;

            class Test {
              void m(String s) {
                checkState(s != null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void npeWithNonNotNullCheck() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new NullPointerException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void floatingPoint() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(double d) {
                if (d < 0.0) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(double d) {
                checkArgument(!(d < 0.0));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void throwableCause() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(Throwable t) {
                if (t == null) {
                  throw new IllegalArgumentException("throwable", t);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void partOfElseIf() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i, int j) {
                if (i == 0) {
                  System.out.println(i);
                } else if (j == 0) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void withComments() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i <= 0) {
                  // check if i is positive
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void withCommentsInside() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int i) {
                if (i <= 0) {
                  throw new IllegalArgumentException(); // check if i is positive
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void messageWithMethodCall() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new IllegalArgumentException("got " + s.length());
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void formatWithConstantMethodCall() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new IllegalArgumentException(String.format("got %s", s.length()));
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(String s) {
                checkArgument(!s.isEmpty(), "got %s", s.length());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void formatWithStaticMethodCall() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new IllegalArgumentException("got " + Math.random());
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void formatWithUnknownMethodCall() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s.isEmpty()) {
                  throw new IllegalArgumentException(String.format("got %s", random()));
                }
              }

              double random() {
                return Math.random();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void withMemberSelect() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              static final String INVALID_ID = "invalid id";

              void m(int i) {
                if (i <= 0) {
                  throw new IllegalArgumentException(Test.INVALID_ID);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              static final String INVALID_ID = "invalid id";

              void m(int i) {
                checkArgument(i > 0, Test.INVALID_ID);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void simpleIf() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(boolean ok) {
                if (!ok) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkArgument;

            class Test {
              void m(boolean ok) {
                checkArgument(ok);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionThatLooksBadNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int c, int first, int last) {
                if (c < first || c > last) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void conditionWithAndNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int a, int b) {
                if (a > 0 && b > 0) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void conditionWithXorNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(boolean a, boolean b) {
                if (a ^ b) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void conditionWithTernaryNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(boolean a, boolean b, boolean c) {
                if (a ? b : c) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void conditionWithSwitchExpressionNegated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(int x) {
                if (switch (x) {
                  case 1 -> true;
                  default -> false;
                }) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void insideCheckNotNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public static <T> T checkNotNull(T reference) {
                if (reference == null) {
                  throw new NullPointerException();
                }
                return reference;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void insideCheckArgument() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public static void checkArgument(boolean expression) {
                if (!expression) {
                  throw new IllegalArgumentException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void insideCheckState() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public static void checkState(boolean expression) {
                if (!expression) {
                  throw new IllegalStateException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multipleStatementsInThen() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s == null) {
                  System.out.println("null");
                  throw new NullPointerException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void exceptionNotNew() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                if (s == null) {
                  throw createNpe();
                }
              }
              NullPointerException createNpe() {
                return new NullPointerException();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void localVariableInCondition() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void m(String s) {
                String local = s;
                if (local == null) {
                  throw new NullPointerException();
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
