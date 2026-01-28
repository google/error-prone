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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.fixes.Fix;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link IfChainToSwitch}. */
@RunWith(JUnit4.class)
public final class IfChainToSwitchTest {
  private static final String SUIT =
      """
      enum Suit {
        HEART,
        SPADE,
        DIAMOND,
        CLUB
      };
      """;
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(IfChainToSwitch.class, getClass())
          .addSourceLines("Suit.java", SUIT);
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(IfChainToSwitch.class, getClass())
          .addInputLines("Suit.java", SUIT)
          .expectUnchanged();

  @Test
  public void ifChain_nameInvariance_error() {
    // Different ways of referencing the same symbol should not matter
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this./* hi */ suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else if (this./* hello */ suit == null) {
                  System.out.println("It's null!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused ->
                      /* hi */
                      System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null ->
                      /* hello */
                      System.out.println("It's null!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_removesTrailing_error() {
    // Removal of unreachable code after the final branch
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  throw new RuntimeException("It's a string!");
                } else if (suit instanceof Number) {
                  return;
                } else if (suit instanceof Object) {
                  throw new RuntimeException("It's an object!");
                } else if (suit == null) {
                  throw new RuntimeException("It's null!");
                }
                // Farewell to this comment
                System.out.println("Delete me");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> throw new RuntimeException("It's a string!");
                  case Number unused -> {
                    return;
                  }
                  case Object unused -> throw new RuntimeException("It's an object!");
                  case null -> throw new RuntimeException("It's null!");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_removesTrailingButKeepsInterestingComments_error() {
    // Interesting comments should be preserved
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  throw new RuntimeException("It's a string!");
                } else if (suit instanceof Number) {
                  return;
                } else if (suit instanceof Object) {
                  throw new RuntimeException("It's an object!");
                } else if (suit == null) {
                  throw new RuntimeException("It's null!");
                }
                // LINT.Something(...)

                System.out.println("Delete me");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> throw new RuntimeException("It's a string!");
                  case Number unused -> {
                    return;
                  }
                  case Object unused -> throw new RuntimeException("It's an object!");
                  case null -> throw new RuntimeException("It's null!");
                }
                // LINT.Something(...)

              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_booleanSwitch_noError() {
    // Boolean switch is not supported by this tool
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Boolean b = s == null;
                if (b == true) {
                  System.out.println("It's true");
                } else if (b == false) {
                  System.out.println("It's false");
                } else if (b instanceof Boolean boo) {
                  throw new RuntimeException("It's a boolean that's neither true nor false");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_longSwitch_noError() {
    // Switch on long is not supported by this tool
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                long l = s == null ? 1 : 2;
                if (l == 23l) {
                  System.out.println("It's 23");
                } else if (l == 45l) {
                  System.out.println("It's 45");
                } else if (l == 67l) {
                  System.out.println("It's neither 23 nor 45");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_intSwitch_error() {
    // Switch on int is supported
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                int i = s == null ? 1 : 2;
                if (i == 23) {
                  System.out.println("It's 23");
                } else if (i == 45) {
                  System.out.println("It's 45");
                } else if (i == 67) {
                  System.out.println("It's 67");
                } else {
                  System.out.println("None of the above");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                int i = s == null ? 1 : 2;
                switch (i) {
                  case 23 -> System.out.println("It's 23");
                  case 45 -> System.out.println("It's 45");
                  case 67 -> System.out.println("It's 67");
                  default -> System.out.println("None of the above");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_dontAlwaysPullUp_error() {
    // Pull up should not occur if it would result in a conflict
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = null;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else if (this.suit instanceof Object o) {
                  System.out.println("It's an object!");
                }
                throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = null;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case Object o -> System.out.println("It's an object!");
                }
                throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_dontAlwaysPullUpSafe_error() {
    // Pull up should not occur if it would result in a conflict
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = null;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else if (this.suit instanceof Object o) {
                  System.out.println("It's an object!");
                }
                throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = null;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case Object o -> System.out.println("It's an object!");
                  case null -> {}
                }
                throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_nonexhausivePattern_noError() {
    // Pulling up the throw would change semantics
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = null;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                }
                throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_twoLinesAfterDefault_error() {
    // Convert but don't delete following lines if they remain reachable
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Object) {
                  System.out.println("It's an object!");
                } else if (suit == null) {
                  System.out.println("It's null!");
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Object unused -> System.out.println("It's an object!");
                  case null -> System.out.println("It's null!");
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_bindingPatternTree_error() {
    // Note `instanceof Number n`, otherwise same as ifChain_twoLinesAfterDefault_error
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number n) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Object) {
                  System.out.println("It's an object!");
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number n -> System.out.println("It's a number!");
                  case Object unused -> System.out.println("It's an object!");
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_bindingPatternTreeSafe_error() {
    // Note `instanceof Number n`, otherwise same as ifChain_twoLinesAfterDefault_error
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                if (this.suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Number n) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Object) {
                  System.out.println("It's an object!");
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              private Object suit;

              public void foo(Suit s) {
                this.suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number n -> System.out.println("It's a number!");
                  case Object unused -> System.out.println("It's an object!");
                  case null -> {}
                }
                System.out.println("Don't delete me!");
                System.out.println("Don't delete me either!");
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_removesParens_error() {
    // Removes redundant parens around if condition
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_removesParensSafe_error() {
    // Removes redundant parens around if condition
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null, default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_equality_error() {
    // Enum equality
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit.DIAMOND -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_equalitySafe_error() {
    // Enum equality
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit.DIAMOND -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null, default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_exhaustiveEnum_error() {
    // Enum equality
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void foo(Suit suit) {
                if (suit == Suit.CLUB) {
                  System.out.println("It's a club!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit == Suit.HEART)) {
                  System.out.println("It's a heart!");
                } else { // c1
                  {
                    System.out.println("It's a diamond!");
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
class Test {
  public void foo(Suit suit) {
    switch (suit) {
      case Suit.CLUB -> System.out.println("It's a club!");
      case Suit.DIAMOND -> System.out.println("It's a diamond!");
      case Suit.HEART -> System.out.println("It's a heart!");
      default ->
          // c1
          System.out.println("It's a diamond!");
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_exhaustiveEnumSafe_error() {
    // Enum equality
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void foo(Suit suit) {
                if (suit == Suit.CLUB) {
                  System.out.println("It's a club!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit == Suit.HEART)) {
                  System.out.println("It's a heart!");
                } else { // c1
                  {
                    System.out.println("It's a diamond!");
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
class Test {
  public void foo(Suit suit) {
    switch (suit) {
      case Suit.CLUB -> System.out.println("It's a club!");
      case Suit.DIAMOND -> System.out.println("It's a diamond!");
      case Suit.HEART -> System.out.println("It's a heart!");
      case null, default ->
          // c1
          System.out.println("It's a diamond!");
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_equalityNull_error() {
    // Equality with null
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit == null) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null -> System.out.println("It's a diamond!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_equalityNullAndNoExplicitDefault_error() {
    // Equality with null, and default must be synthesized
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                if (i == 1) {
                  System.out.println("s is null");
                } else if (i == null) {
                  System.out.println("a mystery");
                } else if (i == 2) {
                  System.out.println("s is non-null");
                } else if (i == 3) {
                  System.out.println("another mystery");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                switch (i) {
                  case 1 -> System.out.println("s is null");
                  case null -> System.out.println("a mystery");
                  case 2 -> System.out.println("s is non-null");
                  case 3 -> System.out.println("another mystery");
                  default -> {}
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_hashCode_noError() {
    // Switch on the result of a method; here `hashCode`.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                Object suit2 = s;
                System.out.println("yo");
                if (suit.hashCode() == 213) {
                  System.out.println("It's a string!");
                } else if (suit.hashCode() == 456) {
                  System.out.println("It's a diamond!");
                } else if (suit.hashCode() == 789) {
                  System.out.println("It's a diamond!");
                }
                System.out.println("Don't delete me!");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                Object suit2 = s;
                System.out.println("yo");
                switch (suit.hashCode()) {
                  case 213 -> System.out.println("It's a string!");
                  case 456 -> System.out.println("It's a diamond!");
                  case 789 -> System.out.println("It's a diamond!");
                  default -> {}
                }
                System.out.println("Don't delete me!");
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_returnPullUp_error() {
    // The last case can complete normally, so the return cannot be pulled up.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit s) {
                int i = s == null ? 1 : 2;
                if (i == 1) {
                  return 1;
                } else if (i == 2) {
                  return 2;
                } else if (i == 3) {
                  if (i > 33) {
                    return 33;
                  }
                }
                return -1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit s) {
                int i = s == null ? 1 : 2;
                switch (i) {
                  case 1 -> {
                    return 1;
                  }
                  case 2 -> {
                    return 2;
                  }
                  case 3 -> {
                    if (i > 33) {
                      return 33;
                    }
                  }
                  default -> {}
                }
                return -1;
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_proto_error() {
    // Switch on the result of a method; here a proto getter.
    helper
        .addSourceLines(
            "com/google/protobuf/GeneratedMessage.java",
            """
            package com.google.protobuf;

            public class GeneratedMessage {}
            """)
        .addSourceLines(
            "Proto.java",
            """
            public abstract class Proto extends com.google.protobuf.GeneratedMessage {
              public abstract Long getMessage();
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean g(Proto proto, Suit s) {
                Object suit = s;
                Object suit2 = s;
                System.out.println("yo");
                // BUG: Diagnostic contains:
                if (proto.getMessage() == 1l) {
                  System.out.println("It's red");
                } else if (proto.getMessage() == 2l) {
                  System.out.println("It's yellow");
                } else if (proto.getMessage() == 3l) {
                  System.out.println("It's red");
                } else throw new AssertionError();
                return true;
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_string_noError() {
    // Comparing strings with == is probably not what is intended
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit suit) {
                String s = suit == null ? "null" : "nonnull";
                if (s == "null") {
                  System.out.println("iffy way to compare strings");
                } else if (s == "nonnull") {
                  System.out.println("also this");
                } else if (s == "something else") {
                  System.out.println("unlikely");
                } else {
                  System.out.println("Probably landing here");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_legalDuplicate_error() {
    // Although the guard effectively duplicates the diamond constant case, this construction is
    // legal
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                if (s == Suit.DIAMOND) {
                  System.out.println("Diamond");
                } else if (s instanceof Suit r) {
                  System.out.println("It's some black suit");
                } else if (s == Suit.HEART) {
                  System.out.println("Heart");
                } else if (s instanceof Suit ss && ss == Suit.DIAMOND) {
                  System.out.println("Technically allowed");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                switch (s) {
                  case Suit.DIAMOND -> System.out.println("Diamond");
                  case Suit.HEART -> System.out.println("Heart");
                  case Suit ss when ss == Suit.DIAMOND -> System.out.println("Technically allowed");
                  case Suit r -> System.out.println("It's some black suit");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_legalDuplicateSafe_error() {
    // Although the guard effectively duplicates the diamond constant case, this construction is
    // legal
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                if (s == Suit.DIAMOND) {
                  System.out.println("Diamond");
                } else if (s instanceof Suit r) {
                  System.out.println("It's some black suit");
                } else if (s == Suit.HEART) {
                  System.out.println("Heart");
                } else if (s instanceof Suit ss && ss == Suit.DIAMOND) {
                  System.out.println("Technically allowed");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                switch (s) {
                  case Suit.DIAMOND -> System.out.println("Diamond");
                  case Suit.HEART -> System.out.println("Heart");
                  case Suit ss when ss == Suit.DIAMOND -> System.out.println("Technically allowed");
                  case Suit r -> System.out.println("It's some black suit");
                  case null -> {}
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_dupeEnum_noError() {
    // Duplicate enum
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                if (s == Suit.DIAMOND) {
                  System.out.println("Diamond");
                } else if (s instanceof Suit r) {
                  System.out.println("It's some black suit");
                } else if (Suit.HEART == s) {
                  System.out.println("Hearts");
                } else if (Suit.DIAMOND == s) {
                  System.out.println("Uh oh, diamond again");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_loopContext_noError() {
    // Wrapping in a switch will change the meaning of the break
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                while (true) {
                  if (s == Suit.DIAMOND) {
                    System.out.println("Diamond");
                  } else if (s instanceof Suit r) {
                    System.out.println("It's some black suit");
                    break;
                  } else if (Suit.HEART == s) {
                    System.out.println("Hearts");
                  } else if (Suit.SPADE == s) {
                    System.out.println("Spade");
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_loopContextYield_noError() {
    // Putting a switch in another switch would change the meaning of the `yield`
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                int q =
                    switch (s) {
                      default -> {
                        if (s == Suit.DIAMOND) {
                          throw new AssertionError("Diamond");
                        } else if (s instanceof Suit r) {
                          throw new AssertionError("Club");
                        } else if (Suit.HEART == s) {
                          throw new AssertionError("Heart");
                        } else if (Suit.SPADE == s) {
                          System.out.println("Spade");
                          yield 4;
                        }
                        throw new AssertionError();
                      }
                    };
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_loopContextPullUp_noError() {
    // Can't pull up break into switch context
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                while (true) {
                  if (s == Suit.DIAMOND) {
                    System.out.println("Diamond");
                  } else if (s instanceof Suit r) {
                    System.out.println("It's some black suit");
                    break;
                  } else if (Suit.HEART == s) {
                    System.out.println("Hearts");
                  } else if (Suit.SPADE == s) {
                    System.out.println("Spade");
                  }
                  break;
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_dupeConstant_noError() {
    // Duplicate int constant
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                if (i == 1) {
                  System.out.println("1");
                } else if (i == 2) {
                  System.out.println("2");
                } else if (i instanceof Integer) {
                  System.out.println("Integer");
                } else if (1 == i) {
                  System.out.println("Uh oh, 1 again");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_record_noError() {
    // Don't attempt to convert record types (unsupported)
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {

              record Person(String name, int age) {}

              public void foo(Object o) {
                if (o instanceof Person(String name, int age)) {
                  System.out.println("generic person");
                } else if (o instanceof Person(String name, int age) && name.equals("alice") && age == 20) {
                  System.out.println("alice20");
                } else if (o instanceof Person(String name, int age) && name.equals("bob") && age == 21) {
                  System.out.println("bob21");
                } else if (o instanceof Object obj) {
                  System.out.println("make exhaustive");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_nonVarSubject_noError() {
    // Should not merely compare method name (e.g. `hashCode`)
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                Object suit2 = s;
                System.out.println("yo");
                if (suit.hashCode() == 13242) {
                  System.out.println("It's a string!");
                } else if (suit2.hashCode() == 23) {
                  System.out.println("Weird");
                } else throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_equalityAlone_noError() {
    // Can't use guard with equality
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit.hashCode() == 13242 && suit.hashCode() > 0) {
                  System.out.println("It's a string!");
                } else if (suit.hashCode() == 23) {
                  System.out.println("Weird");
                } else throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_nested_error() {
    // Only report on the outermost if-chain
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.Optional;
            import java.lang.Integer;
            import java.lang.Long;
            import java.math.BigDecimal;
            import java.time.Duration;
            import java.time.Instant;
            import java.util.Date;

            class Test {
              public void foo(Suit s) {
                Object[] parts = {s, s};
                for (Object o : parts) {
                  Optional<Integer> c = Optional.empty();
                  if (o instanceof Number) {
                    System.out.println("It's a number!");
                  }
                  if (o instanceof Number) {
                    if (o instanceof Integer) {
                      System.out.println("It's an integer!");
                    } else if (o instanceof Long) {
                      System.out.println("It's a long!");
                    } else if (o instanceof Double) {
                      System.out.println("It's a double!");
                    } else if (o instanceof Float) {
                      System.out.println("It's a float!");
                    } else if (o instanceof BigDecimal) {
                      System.out.println("It's a BigDecimal!");
                    } else {
                      throw new IllegalArgumentException("Weird number type");
                    }
                  } else if (o instanceof String) {
                    System.out.println("It's a string!");
                  } else if (o instanceof byte[]) {
                    System.out.println("It's a byte array!");
                  } else if (o instanceof int[][] ii) {
                    System.out.println("It's an int array array");
                  } else if (o instanceof Boolean) {
                    System.out.println("It's a Boolean!");
                  } else if (o == null) {
                    System.out.println("It's null!");
                  } else if (o instanceof Instant[][]) {
                    System.out.println("It's an Instant array array");
                  } else if (o instanceof Duration[] d) {
                    System.out.println("It's a Duration array");
                  } else if (o instanceof Date) {
                    System.out.println("It's a Date!");
                  } else {
                    throw new IllegalArgumentException("Some unexpected type");
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.Optional;
            import java.lang.Integer;
            import java.lang.Long;
            import java.math.BigDecimal;
            import java.time.Duration;
            import java.time.Instant;
            import java.util.Date;

            class Test {
              public void foo(Suit s) {
                Object[] parts = {s, s};
                for (Object o : parts) {
                  Optional<Integer> c = Optional.empty();
                  if (o instanceof Number) {
                    System.out.println("It's a number!");
                  }
                  switch (o) {
                    case Number unused -> {
                      if (o instanceof Integer) {
                        System.out.println("It's an integer!");
                      } else if (o instanceof Long) {
                        System.out.println("It's a long!");
                      } else if (o instanceof Double) {
                        System.out.println("It's a double!");
                      } else if (o instanceof Float) {
                        System.out.println("It's a float!");
                      } else if (o instanceof BigDecimal) {
                        System.out.println("It's a BigDecimal!");
                      } else {
                        throw new IllegalArgumentException("Weird number type");
                      }
                    }
                    case String unused -> System.out.println("It's a string!");
                    case byte[] unused -> System.out.println("It's a byte array!");
                    case int[][] ii -> System.out.println("It's an int array array");
                    case Boolean unused -> System.out.println("It's a Boolean!");
                    case Instant[][] unused -> System.out.println("It's an Instant array array");
                    case Duration[] d -> System.out.println("It's a Duration array");
                    case Date unused -> System.out.println("It's a Date!");
                    case null -> System.out.println("It's null!");
                    default -> throw new IllegalArgumentException("Some unexpected type");
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_elseBecomesDefault_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else {
                  throw new AssertionError();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit.DIAMOND -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_elseBecomesDefaultSafe_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit == Suit.DIAMOND) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else {
                  throw new AssertionError();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit.DIAMOND -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null, default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_commentHandling_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                // alpha
                /* beta */ if /* gamma */ (suit /* delta */ instanceof String /* epsilon */) {
                  // zeta
                  return;
                  /* eta */
                } /* nu */ else /* theta */ if (suit == /* iota */ Suit.DIAMOND) {
                  /* kappa */ {
                    return; // lambda
                  }
                } else if (((suit instanceof Number) /* tao */)) {
                  // Square
                  throw new NullPointerException(/* chi */ );
                } else if (suit /* omicron */ instanceof Suit /* pi */) {
                  /* mu */
                  return;
                  /* nu */
                }
                return;
                /* xi */
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                // alpha
                /* beta */ switch (suit) {
                  case String unused ->
                  /* gamma */
                  /* delta */
                  /* epsilon */
                  /* nu */
                  /* theta */
                  {
                    // zeta
                    return;
                    /* eta */
                  }
                  case Suit.DIAMOND ->
                  /* iota */
                  /* kappa */
                  {
                    return; // lambda
                  }
                  case Number unused ->
                      /* tao */
                      // Square
                      throw new NullPointerException(/* chi */ );
                  case Suit unused ->
                  /* omicron */
                  /* pi */
                  {
                    /* mu */
                    return;
                    /* nu */
                  }
                  default -> {
                    return;
                  }
                }

                /* xi */
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_commentHandlingSafe_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                // alpha
                /* beta */ if /* gamma */ (suit /* delta */ instanceof String /* epsilon */) {
                  // zeta
                  return;
                  /* eta */
                } /* nu */ else /* theta */ if (suit == /* iota */ Suit.DIAMOND) {
                  /* kappa */ {
                    return; // lambda
                  }
                } else if (((suit instanceof Number) /* tao */)) {
                  // Square
                  throw new NullPointerException(/* chi */ );
                } else if (suit /* omicron */ instanceof Suit /* pi */) {
                  /* mu */
                  return;
                  /* nu */
                }
                return;
                /* xi */
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                // alpha
                /* beta */ switch (suit) {
                  case String unused ->
                  /* gamma */
                  /* delta */
                  /* epsilon */
                  /* nu */
                  /* theta */
                  {
                    // zeta
                    return;
                    /* eta */
                  }
                  case Suit.DIAMOND ->
                  /* iota */
                  /* kappa */
                  {
                    return; // lambda
                  }
                  case Number unused ->
                      /* tao */
                      // Square
                      throw new NullPointerException(/* chi */ );
                  case Suit unused ->
                  /* omicron */
                  /* pi */
                  {
                    /* mu */
                    return;
                    /* nu */
                  }
                  case null, default -> {
                    return;
                  }
                }

                /* xi */
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUp_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  return;
                } else if (suit == Suit.DIAMOND) {
                  return;
                } else if ((suit instanceof Number)) {
                  return;
                } else if (suit instanceof Suit) {
                  return;
                }
                return;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> {
                    return;
                  }
                  case Suit.DIAMOND -> {
                    return;
                  }
                  case Number unused -> {
                    return;
                  }
                  case Suit unused -> {
                    return;
                  }
                  default -> {
                    return;
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUpHasExplicitNullCheck_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit == Suit.SPADE) {
                  return;
                } else if (suit == Suit.DIAMOND) {
                  return;
                } else if (suit == Suit.HEART) {
                  return;
                } else if (suit == null) {
                  throw new NullPointerException();
                }
                return;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case Suit.SPADE -> {
                    return;
                  }
                  case Suit.DIAMOND -> {
                    return;
                  }
                  case Suit.HEART -> {
                    return;
                  }
                  case null -> throw new NullPointerException();
                  default -> {
                    return;
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe=false")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUpHasExplicitNullCheckSafe_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit == Suit.SPADE) {
                  return;
                } else if (suit == Suit.DIAMOND) {
                  return;
                } else if (suit == Suit.HEART) {
                  return;
                } else if (suit == null) {
                  throw new NullPointerException();
                }
                return;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case Suit.SPADE -> {
                    return;
                  }
                  case Suit.DIAMOND -> {
                    return;
                  }
                  case Suit.HEART -> {
                    return;
                  }
                  case null -> throw new NullPointerException();
                  default -> {
                    return;
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUpSafe2_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  return;
                } else if (suit == Suit.DIAMOND) {
                  return;
                } else if ((suit instanceof Number)) {
                  return;
                } else if (suit instanceof Suit) {
                  return;
                }
                return;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> {
                    return;
                  }
                  case Suit.DIAMOND -> {
                    return;
                  }
                  case Number unused -> {
                    return;
                  }
                  case Suit unused -> {
                    return;
                  }
                  case null, default -> {
                    return;
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUpReachabilityUnchanged_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                {
                  Suit suit = s;
                  if (Suit.SPADE == suit) {
                    return;
                  } else if (suit == Suit.DIAMOND) {
                    return;
                  } else if (suit == Suit.HEART) {
                    return;
                  } else if (suit == Suit.CLUB) {
                    return;
                  }
                  System.out.println("this will become unreachable");
                  System.out.println("this will too");
                }
                System.out.println("this will too");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                {
                  Suit suit = s;
                  switch (suit) {
                    case Suit.SPADE -> {
                      return;
                    }
                    case Suit.DIAMOND -> {
                      return;
                    }
                    case Suit.HEART -> {
                      return;
                    }
                    case Suit.CLUB -> {
                      return;
                    }
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_pullUpReachabilityChanges_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                {
                  Suit suit = s;
                  if (Suit.SPADE == suit) {
                    return;
                  } else if (suit == Suit.DIAMOND) {
                    return;
                  } else if (suit == Suit.HEART) {
                    return;
                  } else if (suit == Suit.CLUB) {
                    return;
                  } else if (suit == null) {
                    return;
                  }
                  System.out.println("this will become unreachable");
                  System.out.println("this will too");
                }
                System.out.println("this will vanish");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                {
                  Suit suit = s;
                  switch (suit) {
                    case Suit.SPADE -> {
                      return;
                    }
                    case Suit.DIAMOND -> {
                      return;
                    }
                    case Suit.HEART -> {
                      return;
                    }
                    case Suit.CLUB -> {
                      return;
                    }
                    case null -> {
                      return;
                    }
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_instanceOfWithGuardSafe_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Suit && suit == Suit.SPADE) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  {
                    {
                      /* stay silent about numbers */
                    }
                  }
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit unused when suit == Suit.SPADE -> System.out.println("It's a diamond!");
                  case Number unused -> {
                    /* stay silent about numbers */
                  }
                  case Suit unused -> System.out.println("It's a Suit!");
                  case null, default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_instanceOfWithGuard_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Suit && suit == Suit.SPADE) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  {
                    {
                      /* stay silent about numbers */
                    }
                  }
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit unused when suit == Suit.SPADE -> System.out.println("It's a diamond!");
                  case Number unused -> {
                    /* stay silent about numbers */
                  }
                  case Suit unused -> System.out.println("It's a Suit!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_tooShallow_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_mismatchSubject_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                Object suit2 = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit2 instanceof Number) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a Suit!");
                } else throw new AssertionError();
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_parameterizedTypeSafe_error() {
    // Raw types are converted to the wildcard type.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              public void foo(Suit s) {
                List<Integer> list = new ArrayList<>();
                if (list instanceof ArrayList<Integer> && list.size() == 0) {
                  System.out.println("int empty array list");
                } else if (list instanceof ArrayList && list.size() == 0) {
                  System.out.println("raw empty array list");
                } else if (list instanceof ArrayList<?> && list.size() == 1) {
                  System.out.println("wildcard element array list");
                } else if (list instanceof ArrayList<? extends Number> && list.size() == 1) {
                  System.out.println("number element array list");
                } else if (list instanceof List<Integer> l && l.hashCode() == 17) {
                  System.out.println("hash 17 list");
                } else if (list instanceof List l) {
                  System.out.println("list");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import java.lang.Number;
import java.util.List;
import java.util.ArrayList;

class Test {
  public void foo(Suit s) {
    List<Integer> list = new ArrayList<>();
    switch (list) {
      case ArrayList<Integer> unused when list.size() == 0 ->
          System.out.println("int empty array list");
      case ArrayList<?> unused when list.size() == 0 -> System.out.println("raw empty array list");
      case ArrayList<?> unused when list.size() == 1 ->
          System.out.println("wildcard element array list");
      case ArrayList<? extends Number> unused when list.size() == 1 ->
          System.out.println("number element array list");
      case List<Integer> l when l.hashCode() == 17 -> System.out.println("hash 17 list");
      case List<?> l -> System.out.println("list");
      case null -> {}
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_parameterizedType_error() {
    // Raw types are converted to the wildcard type.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              public void foo(Suit s) {
                List<Integer> list = new ArrayList<>();
                if (list instanceof ArrayList<Integer> && list.size() == 0) {
                  System.out.println("int empty array list");
                } else if (list instanceof ArrayList && list.size() == 0) {
                  System.out.println("raw empty array list");
                } else if (list instanceof ArrayList<?> && list.size() == 1) {
                  System.out.println("wildcard element array list");
                } else if (list instanceof ArrayList<? extends Number> && list.size() == 1) {
                  System.out.println("number element array list");
                } else if (list instanceof List<Integer> l && l.hashCode() == 17) {
                  System.out.println("hash 17 list");
                } else if (list instanceof List l) {
                  System.out.println("list");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import java.lang.Number;
import java.util.List;
import java.util.ArrayList;

class Test {
  public void foo(Suit s) {
    List<Integer> list = new ArrayList<>();
    switch (list) {
      case ArrayList<Integer> unused when list.size() == 0 ->
          System.out.println("int empty array list");
      case ArrayList<?> unused when list.size() == 0 -> System.out.println("raw empty array list");
      case ArrayList<?> unused when list.size() == 1 ->
          System.out.println("wildcard element array list");
      case ArrayList<? extends Number> unused when list.size() == 1 ->
          System.out.println("number element array list");
      case List<Integer> l when l.hashCode() == 17 -> System.out.println("hash 17 list");
      case List<?> l -> System.out.println("list");
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_multiParameterizedTypeSafe_error() {
    // Raw types are converted to the wildcard type.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.Map;
            import java.util.HashMap;

            class Test {
              public void foo(Suit s) {
                Map<Integer, String> map = new HashMap<>();
                if (map instanceof HashMap && map.size() == 0) {
                  System.out.println("empty hash map");
                } else if (map instanceof HashMap<Integer, String> && map.size() == 0) {
                  System.out.println("empty hash map with type parameters");
                } else if (map instanceof HashMap<?, String> && map.size() == 0) {
                  System.out.println("empty hash map with wildcard");
                } else if (map instanceof HashMap && map.size() == 1) {
                  System.out.println("one element hash map");
                } else if (map instanceof Map<Integer, ? extends Object> m && m.hashCode() == 17) {
                  System.out.println("hash 17");
                } else if (map instanceof Map m) {
                  System.out.println("map");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import java.lang.Number;
import java.util.Map;
import java.util.HashMap;

class Test {
  public void foo(Suit s) {
    Map<Integer, String> map = new HashMap<>();
    switch (map) {
      case HashMap<?, ?> unused when map.size() == 0 -> System.out.println("empty hash map");
      case HashMap<Integer, String> unused when map.size() == 0 ->
          System.out.println("empty hash map with type parameters");
      case HashMap<?, String> unused when map.size() == 0 ->
          System.out.println("empty hash map with wildcard");
      case HashMap<?, ?> unused when map.size() == 1 -> System.out.println("one element hash map");
      case Map<Integer, ? extends Object> m when m.hashCode() == 17 ->
          System.out.println("hash 17");
      case Map<?, ?> m -> System.out.println("map");
      case null -> {}
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_multiParameterizedType_error() {
    // Raw types are converted to the wildcard type.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;
            import java.util.Map;
            import java.util.HashMap;

            class Test {
              public void foo(Suit s) {
                Map<Integer, String> map = new HashMap<>();
                if (map instanceof HashMap && map.size() == 0) {
                  System.out.println("empty hash map");
                } else if (map instanceof HashMap<Integer, String> && map.size() == 0) {
                  System.out.println("empty hash map with type parameters");
                } else if (map instanceof HashMap<?, String> && map.size() == 0) {
                  System.out.println("empty hash map with wildcard");
                } else if (map instanceof HashMap && map.size() == 1) {
                  System.out.println("one element hash map");
                } else if (map instanceof Map<Integer, ? extends Object> m && m.hashCode() == 17) {
                  System.out.println("hash 17");
                } else if (map instanceof Map m) {
                  System.out.println("map");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import java.lang.Number;
import java.util.Map;
import java.util.HashMap;

class Test {
  public void foo(Suit s) {
    Map<Integer, String> map = new HashMap<>();
    switch (map) {
      case HashMap<?, ?> unused when map.size() == 0 -> System.out.println("empty hash map");
      case HashMap<Integer, String> unused when map.size() == 0 ->
          System.out.println("empty hash map with type parameters");
      case HashMap<?, String> unused when map.size() == 0 ->
          System.out.println("empty hash map with wildcard");
      case HashMap<?, ?> unused when map.size() == 1 -> System.out.println("one element hash map");
      case Map<Integer, ? extends Object> m when m.hashCode() == 17 ->
          System.out.println("hash 17");
      case Map<?, ?> m -> System.out.println("map");
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_duplicateConstant_noError() {
    // Duplicate constant
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 0 : 1;
                if (i == 88) {
                  System.out.println("i is 88");
                } else if (i instanceof Integer) {
                  System.out.println("It's a integer!");
                } else if (i == 88) {
                  System.out.println("i is once again 88");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_domination1Safe_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit && suit == Suit.DIAMOND) {
                  System.out.println("It's a Suit!");
                } else if (suit instanceof Suit suity && suit == Suit.SPADE) {
                  System.out.println("It's a Suity!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit unused when suit == Suit.DIAMOND -> System.out.println("It's a Suit!");
                  case Suit suity when suit == Suit.SPADE -> System.out.println("It's a Suity!");
                  case Suit unused -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  case null, default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain", "-XepOpt:IfChainToSwitch:EnableSafe")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_domination1_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                if (suit instanceof String) {
                  System.out.println("It's a string!");
                } else if (suit instanceof Suit) {
                  System.out.println("It's a diamond!");
                } else if ((suit instanceof Number)) {
                  System.out.println("It's a number!");
                } else if (suit instanceof Suit && suit == Suit.DIAMOND) {
                  System.out.println("It's a Suit!");
                } else if (suit instanceof Suit suity && suit == Suit.SPADE) {
                  System.out.println("It's a Suity!");
                } else throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Object suit = s;
                System.out.println("yo");
                switch (suit) {
                  case String unused -> System.out.println("It's a string!");
                  case Suit unused when suit == Suit.DIAMOND -> System.out.println("It's a Suit!");
                  case Suit suity when suit == Suit.SPADE -> System.out.println("It's a Suity!");
                  case Suit unused -> System.out.println("It's a diamond!");
                  case Number unused -> System.out.println("It's a number!");
                  default -> throw new AssertionError();
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_domination2_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 0 : 1;
                if (i instanceof Integer) {
                  System.out.println("It's a integer!");
                } else if (i == 0) {
                  System.out.println("It's 0!");
                } else if (i instanceof Integer o && o.hashCode() == 17) {
                  System.out.println("Its hashcode is 17");
                } else if (i == 23) {
                  System.out.println("It's a 23");
                } else if (i instanceof Integer in && i > 348) {
                  System.out.println("It's a big integer!");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
"""
import java.lang.Number;

class Test {
  public void foo(Suit s) {
    Integer i = s == null ? 0 : 1;
    switch (i) {
      case 0 -> System.out.println("It's 0!");
      case Integer o when o.hashCode() == 17 -> System.out.println("Its hashcode is 17");
      case 23 -> System.out.println("It's a 23");
      case Integer in when i > 348 -> System.out.println("It's a big integer!");
      case Integer unused -> System.out.println("It's a integer!");
    }
  }
}
""")
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void ifChain_domination3_error() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 0 : 1;
                if (i instanceof Number n) {
                  System.out.println("It's a number!");
                } else if (i == null) {
                  System.out.println("It's 0!");
                } else if (i instanceof Object o && o.hashCode() == 17) {
                  System.out.println("Its hashcode is 17");
                } else if (i == 23) {
                  System.out.println("It's a 23");
                } else if (i instanceof Integer) {
                  System.out.println("It's a integer!");
                } else if (i instanceof Integer in && i > 348) {
                  System.out.println("It's a big integer!");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_domination4_noError() {
    // Number and Integer are conflicting (both unconditional for Integer)
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 0 : 1;
                if (i instanceof Number n) {
                  System.out.println("It's a number!");
                } else if (i == 88) {
                  System.out.println("i is 88");
                } else if (i instanceof Integer) {
                  System.out.println("It's a integer!");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_domination5_noError() {
    // Both a default and unconditional conflict
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Suit s) {
                Integer i = s == null ? 0 : 1;
                if (i == 123) {
                  System.out.println("i is 123");
                } else if (i == 456) {
                  System.out.println("i is 456");
                } else if (i instanceof Integer) {
                  System.out.println("Some other int!");
                } else {
                  System.out.println("not possible to have default and unconditional");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .doTest();
  }

  @Test
  public void ifChain_javadocOrdering_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Object obj) {
                if (obj instanceof Object) {
                  System.out.println("It's an object!");
                } else if (obj instanceof Number n) {
                  System.out.println("It's a number!");
                } else if (obj instanceof String) {
                  System.out.println("It's a string!");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.Number;

            class Test {
              public void foo(Object obj) {
                switch (obj) {
                  case Number n -> System.out.println("It's a number!");
                  case String unused -> System.out.println("It's a string!");
                  case Object unused -> System.out.println("It's an object!");
                }
              }
            }
            """)
        .setArgs("-XepOpt:IfChainToSwitch:EnableMain")
        .setFixChooser(IfChainToSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  /**
   * Asserts that there is exactly one suggested fix and returns it.
   *
   * <p>Similar to {@code FixChoosers.FIRST}, but also asserts that there is exactly one fix.
   */
  public static Fix assertOneFixAndChoose(List<Fix> fixes) {
    assertThat(fixes).hasSize(1);
    return fixes.get(0);
  }
}
