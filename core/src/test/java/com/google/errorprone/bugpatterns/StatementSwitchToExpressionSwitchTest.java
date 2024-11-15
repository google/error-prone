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

package com.google.errorprone.bugpatterns;

import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StatementSwitchToExpressionSwitch}. */
@RunWith(JUnit4.class)
public final class StatementSwitchToExpressionSwitchTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(StatementSwitchToExpressionSwitch.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          StatementSwitchToExpressionSwitch.class, getClass());

  @Test
  public void switchByEnum_removesRedundantBreak_error() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case OBVERSE:
                    // Explanatory comment
                    System.out.println("the front is called the");
                    // Middle comment
                    System.out.println("obverse");
                    // Break comment
                    break;
                  // End comment
                  case REVERSE:
                    System.out.println("reverse");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE /* left comment */ /* and there is more: */ // to end of line
                  :
                    // Explanatory comment
                    System.out.println("the front is called the");
                    // Middle comment
                    System.out.println("obverse");
                    // Break comment
                    break;
                  // End comment
                  case REVERSE:
                    System.out.println("reverse");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE -> {
                    /* left comment */
                    /* and there is more: */
                    // to end of line
                    // Explanatory comment
                    System.out.println("the front is called the");
                    // Middle comment
                    System.out.println("obverse");
                    // Break comment
                    // End comment
                  }
                  case REVERSE -> System.out.println("reverse");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnumWithCompletionAnalsis_removesRedundantBreak_error() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  // Comment before first case
                  case OBVERSE:
                    // Explanatory comment
                    System.out.println("this block cannot complete normally");
                    {
                      throw new NullPointerException();
                    }
                  case REVERSE:
                    System.out.println("reverse");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  // Comment before first case
                  case OBVERSE:
                    // Explanatory comment
                    System.out.println("this block cannot complete normally");
                    {
                      throw new NullPointerException();
                    }
                  case REVERSE:
                    System.out.println("reverse");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE -> {
                    // Comment before first case
                    // Explanatory comment
                    System.out.println("this block cannot complete normally");
                    {
                      throw new NullPointerException();
                    }
                  }
                  case REVERSE -> System.out.println("reverse");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnumCard_combinesCaseComments_error() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart2");
                    break;
                  case /* sparkly */ DIAMOND /* Sparkly */:
                  // Empty block comment 1
                  // Fall through
                  case SPADE:
                  // Empty block comment 2
                  case CLUB:
                    // Start of block comment 1
                    System.out.println("what's not a heart is ");
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart2");
                    break;
                  case /* sparkly */ DIAMOND /* Sparkly */:
                  // Empty block comment 1
                  // Fall through
                  case SPADE:
                  // Empty block comment 2
                  case CLUB:
                    // Start of block comment 1
                    System.out.println("what's not a heart is ");
                    System.out.println("everything else");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART -> System.out.println("heart2");
                  case DIAMOND, SPADE, CLUB -> {
                    /* sparkly */
                    /* Sparkly */
                    // Empty block comment 1
                    // Empty block comment 2
                    // Start of block comment 1
                    System.out.println("what's not a heart is ");
                    System.out.println("everything else");
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnumCard2_removesRedundantBreaks_error() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    // Pre break comment
                    break;
                  // Post break comment
                  case DIAMOND:
                    // Diamond break comment
                    break;
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    // Pre break comment
                    break;
                  // Post break comment
                  case DIAMOND:
                    // Diamond break comment
                    break;
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART -> System.out.println("heart");
                  // Pre break comment
                  // Post break comment
                  case DIAMOND -> {
                    // Diamond break comment
                  }
                  case SPADE, CLUB -> System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnumCard_onlyExpressionsAndThrowAreBraceless_error() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                for (; ; ) {
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART:
                      System.out.println("heart");
                      break;
                    case DIAMOND:
                      continue;
                    case SPADE:
                      return;
                    case CLUB:
                      throw new AssertionError();
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      System.out.println("heart");
                      break;
                    case DIAMOND:
                      continue;
                    case SPADE:
                      return;
                    case CLUB:
                      throw new AssertionError();
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                for (; ; ) {
                  switch (side) {
                    case HEART -> System.out.println("heart");
                    case DIAMOND -> {
                      continue;
                    }
                    case SPADE -> {
                      return;
                    }
                    case CLUB -> throw new AssertionError();
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchFallsThruToDefault_noError() {

    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    break;
                  case SPADE:
                  default:
                    System.out.println("spade or club");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchFallsThruFromDefault_noError() {

    // Placing default in the middle of the switch is not recommended, but is valid Java
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    System.out.println("diamond");
                  default:
                    System.out.println("club");
                    break;
                  case SPADE:
                    System.out.println("spade");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchWithDefaultInMiddle_error() {

    // Placing default in the middle of the switch is not recommended, but is valid Java
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    System.out.println("diamond");
                    return;
                  default:
                    System.out.println("club");
                    break;
                  case SPADE:
                    System.out.println("spade");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // This check does not attempt to re-order cases, for example to move the default to the end, as
    // this scope is delegated to other tests e.g. SwitchDefault
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    System.out.println("diamond");
                    return;
                  default /* comment: */:
                    System.out.println("club");
                    break;
                  case SPADE:
                    System.out.println("spade");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART -> System.out.println("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                    return;
                  }
                  default ->
                      /* comment: */
                      System.out.println("club");
                  case SPADE -> System.out.println("spade");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchWithLabelledBreak_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                outer:
                for (; ; ) {
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART:
                      System.out.println("will return");
                      return;
                    case DIAMOND:
                      break outer;
                    case SPADE:
                    case CLUB:
                      System.out.println("everything else");
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                outer:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      System.out.println("will return");
                      return;
                    case DIAMOND:
                      break outer;
                    case SPADE:
                    case CLUB:
                      System.out.println("everything else");
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                outer:
                for (; ; ) {
                  switch (side) {
                    case HEART -> {
                      System.out.println("will return");
                      return;
                    }
                    case DIAMOND -> {
                      break outer;
                    }
                    case SPADE, CLUB -> System.out.println("everything else");
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_statementSwitchWithMultipleExpressions_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("will return");
                    return;
                  case DIAMOND:
                  case SPADE, CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("will return");
                    return;
                  case DIAMOND:
                  case SPADE, CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART -> {
                    System.out.println("will return");
                    return;
                  }
                  case DIAMOND, SPADE, CLUB -> System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithThrow_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("will return");
                    return;
                  case DIAMOND:
                    break;
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchInSwitch_error() {
    // Only the outer "switch" should generate a finding
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    switch (side) {
                      case HEART:
                      case SPADE:
                        System.out.println("non-default");
                        break;
                      default:
                        System.out.println("do nothing");
                    }
                    break;
                  case DIAMOND:
                    break;
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithReturnNested1_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    {
                      System.out.println("nested1");
                      break;
                    }
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    {
                      System.out.println("nested1");
                      break;
                    }
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART -> System.out.println("heart");
                  case DIAMOND -> System.out.println("nested1");
                  case SPADE, CLUB -> System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithReturnNested2_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                  case DIAMOND:
                    {
                      System.out.println("nested2a");
                      {
                        System.out.println("nested2b");
                        break;
                      }
                    }
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumWithConditionalControl_noError() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    if (true) {
                      break;
                    }
                  case DIAMOND:
                    break;
                  case SPADE:
                  case CLUB:
                    System.out.println("everything else");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumWithLambda_noError() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            " import java.util.function.Function;",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            // "Last" statement in the HEART block is a return, but we don't want to conclude that
            // the block has definite control flow based on that
            "          Function<Integer,Integer> x = (i) -> {while(true) {break;} return i;};",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void singleCaseConvertible_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    System.out.println("heart");
                    break;
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void emptyExpressionSwitchCases_noMatch() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo(int value) {
                switch (value) {
                  case 0 -> {}
                  default -> {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonEmptyExpressionSwitchCases_noMatch() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo(int value) {
                switch (value) {
                  case 0 -> System.out.println("zero");
                  default -> {
                    System.out.println("non-zero");
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void dynamicWithThrowableDuringInitializationFromMethod_noMatch() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              Throwable foo = bar();

              public Test(int foo) {}

              private static Throwable bar() {
                return new NullPointerException("initialized with return value");
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_exampleInDocumentation_error() {
    // This code appears as an example in the documentation (added surrounding class)
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEARTS:
                    System.out.println("Red hearts");
                    break;
                  case DIAMONDS:
                    System.out.println("Red diamonds");
                    break;
                  case SPADES:
                  // Fall through
                  case CLUBS:
                    bar();
                    System.out.println("Black suit");
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                switch (suit) {
                  case HEARTS:
                    System.out.println("Red hearts");
                    break;
                  case DIAMONDS:
                    System.out.println("Red diamonds");
                    break;
                  case SPADES:
                  // Fall through
                  case CLUBS:
                    bar();
                    System.out.println("Black suit");
                }
              }

              private void bar() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                switch (suit) {
                  case HEARTS -> System.out.println("Red hearts");
                  case DIAMONDS -> System.out.println("Red diamonds");
                  case SPADES, CLUBS -> {
                    bar();
                    System.out.println("Black suit");
                  }
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_caseHasOnlyComments_error() {
    // When a case is solely comments, we should still try to convert the switch using braceless
    // syntax
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEARTS:
                  // A comment here
                  // more comments.
                  case DIAMONDS:
                    // Diamond comment
                    System.out.println("Red diamonds");
                    break;
                  case SPADES:
                  // Fall through
                  case CLUBS:
                    bar();
                    System.out.println("Black suit");
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                switch (suit) {
                  case HEARTS:
                  // A comment here
                  // more comments.
                  case DIAMONDS:
                    // Diamond comment
                    System.out.println("Heart or diamond");
                    break;
                  case SPADES:
                  // Fall through
                  case CLUBS:
                    bar();
                    System.out.println("Black suit");
                }
              }

              private void bar() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                switch (suit) {
                  case HEARTS, DIAMONDS ->
                      // A comment here
                      // more comments.
                      // Diamond comment
                      System.out.println("Heart or diamond");
                  case SPADES, CLUBS -> {
                    bar();
                    System.out.println("Black suit");
                  }
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_accumulatedComments_error() {
    // Comments should be aggregated across multiple cases
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case /* red */ HEARTS:
                  // A comment here
                  // more comments.
                  case /* red */ DIAMONDS:
                  // Diamonds comment
                  case /* black */ SPADES:
                  // Spades comment
                  case /* black */ CLUBS:
                    bar();
                    System.out.println("Any suit");
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case /* red */ HEARTS:
                  // A comment here
                  // more comments.
                  case /* red */ DIAMONDS:
                  // Diamonds comment
                  case /* black */ SPADES:
                  // Spades comment
                  case /* black */ CLUBS:
                    bar();
                    System.out.println("Any suit");
                }
              }

              private void bar() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              public Test() {}

              private void foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEARTS, DIAMONDS, SPADES, CLUBS -> {
                    /* red */
                    // A comment here
                    // more comments.
                    /* red */
                    // Diamonds comment
                    /* black */
                    // Spades comment
                    /* black */
                    bar();
                    System.out.println("Any suit");
                  }
                }
              }

              private void bar() {}
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_surroundingBracesCannotRemove_error() {
    // Can't remove braces around OBVERSE because break statements are not a member of
    // KINDS_CONVERTIBLE_WITHOUT_BRACES
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case OBVERSE:
                    {
                      // The quick brown fox, jumps over the lazy dog, etc.
                      break;
                    }

                  default:
                    {
                      throw new RuntimeException("Invalid type.");
                    }
                }
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE:
                    {
                      // The quick brown fox, jumps over the lazy dog, etc.
                      break;
                    }

                  default:
                    {
                      throw new RuntimeException("Invalid type.");
                    }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE -> {
                    // The quick brown fox, jumps over the lazy dog, etc.
                  }
                  default -> throw new RuntimeException("Invalid type.");
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_surroundingBracesEmpty_error() {
    // Test handling of cases with surrounding braces that are empty.  The braces around OBVERSE
    // can be removed because throw is a member of KINDS_CONVERTIBLE_WITHOUT_BRACES.
    assume().that(Runtime.version().feature()).isAtLeast(14);

    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case OBVERSE:
                    {
                      // The quick brown fox, jumps over the lazy dog, etc.
                      throw new RuntimeException("Invalid.");
                    }

                  default:
                    {
                    }
                }
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE:
                    {
                      // The quick brown fox, jumps over the lazy dog, etc.
                      throw new RuntimeException("Invalid.");
                    }

                  default:
                    {
                    }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                OBVERSE,
                REVERSE
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case OBVERSE ->
                      // The quick brown fox, jumps over the lazy dog, etc.
                      throw new RuntimeException("Invalid.");
                  default -> {}
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_afterReturnComments_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEART:
                    // before return comment
                    return 123;
                  // after return comment
                  /* more comments */
                  default:
                }
                return 0;
              }
            }
            """)
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEART:
                    // before return comment
                    return 123;
                  // after return comment
                  /* more comments */
                  default:
                    // default comment
                }
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEART -> {
                    // before return comment
                    return 123;
                    // after return comment
                    /* more comments */
                  }
                  default -> {
                    // default comment
                  }
                }
                return 0;
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  /**********************************
   *
   * Return switch test cases
   *
   **********************************/

  @Test
  public void switchByEnum_returnSwitch_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    return invoke();
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    return invoke();
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                return switch (side) {
                  case HEART, DIAMOND -> invoke();
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnSwitchWithShouldNeverHappen_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    return invoke();
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                // This should never happen
                int z = invoke();
                z++;
                throw new RuntimeException("Switch was not exhaustive at runtime " + z);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                return switch (side) {
                  case HEART, DIAMOND -> invoke();
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                };
                // This should never happen

              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_returnSwitchCommentsBeforeFirstCase_errorAndRetained() {
    assume().that(Runtime.version().feature()).isAtLeast(14);

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  // Abracadabra
                  /* foo */ case HEART:
                  // Card trick
                  case DIAMOND:
                    return invoke();
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                // This should never happen
                int z = invoke();
                z++;
                throw new RuntimeException("Switch was not exhaustive at runtime " + z);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                return switch (side) {
                  case HEART, DIAMOND ->
                      // Abracadabra
                      /* foo */
                      // Card trick
                      invoke();
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                };
                // This should never happen

              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_switchInReturnSwitchWithShouldNeverHappen_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    // No error because the inner switch is the only fixable one
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                    System.out.println("hi");
                    switch (side) {
                      case HEART:
                      case DIAMOND:
                        return invoke();
                      case SPADE:
                        throw new RuntimeException();
                      case CLUB:
                        throw new NullPointerException();
                    }
                    // This should never happen
                    int z = invoke();
                    z++;
                    throw new RuntimeException("Switch was not exhaustive at runtime " + z);

                  default:
                    System.out.println("default");
                    return 0;
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_exhaustiveWithDefault_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                String z = "dkfj";
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (z) {
                  case "":
                  case "DIAMOND":
                  // Custom comment
                  case "SPADE":
                    return invoke();
                  default:
                    return 2;
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                String z = "dkfj";
                switch (z) {
                  case "":
                  case "DIAMOND":
                  // Custom comment
                  case "SPADE":
                    return invoke();
                  default:
                    return 2;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                String z = "dkfj";
                return switch (z) {
                  case "", "DIAMOND", "SPADE" ->
                      // Custom comment
                      invoke();
                  default -> 2;
                };
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_defaultFallThru_noError() {
    // No error because default doesn't return anything within its block
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    return invoke();
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    // Fall through
                }
                return -2;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_alwaysThrows_noError() {
    // Every case throws, thus no type for return switch
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    throw new NullPointerException();
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnSwitchWithShouldNeverHappen_errorAndRemoveShouldNeverHappen() {
    // The switch has a case for each enum and "should never happen" error handling
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART:
                    // Fall through
                    case DIAMOND:
                      return invoke();
                    case SPADE:
                      throw new RuntimeException();
                    case CLUB:
                      throw new NullPointerException();
                  }
                  // Custom comment - should never happen
                  int z = invoke(/* block comment 0 */ );
                  // Custom comment 2
                  {
                    z++;
                  }
                  throw new RuntimeException("Switch was not exhaustive at runtime " + z);
                }
                System.out.println("don't delete 2");
                return 0;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  switch (side) {
                    case HEART /* lhs comment */: // rhs comment
                    // Fall through
                    case DIAMOND:
                      return invoke();
                    case SPADE:
                      throw new RuntimeException();
                    case CLUB:
                      throw new NullPointerException();
                  }
                  // Custom comment - should never happen
                  int z = invoke(/* block comment 0 */ );
                  // Custom comment 2
                  {
                    z++;
                  }
                  throw new RuntimeException("Switch was not exhaustive at runtime " + z);
                }
                System.out.println("don't delete 2");
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  return switch (side) {
                    case HEART, DIAMOND ->
                        /* lhs comment */
                        // rhs comment
                        invoke();
                    case SPADE -> throw new RuntimeException();
                    case CLUB -> throw new NullPointerException();
                  };
                  // Custom comment - should never happen

                  // Custom comment 2

                }
                System.out.println("don't delete 2");
                return 0;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_returnSwitchNoFollowingStatementsInBlock_errorAndNoRemoval() {
    // The switch is exhaustive but doesn't have any statements immediately following it in the
    // lowest ancestor statement block
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART /* lhs comment */: // rhs comment
                    // Fall through
                    case DIAMOND:
                      return invoke();
                    case SPADE:
                      throw new RuntimeException();
                    case CLUB:
                      throw new NullPointerException();
                  }
                }
                // Custom comment - should never happen because invoke returns 123
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  switch (side) {
                    case HEART /* lhs comment */: // rhs comment
                    // Fall through
                    case DIAMOND:
                      return invoke();
                    case SPADE:
                      throw new RuntimeException();
                    case CLUB:
                      throw new NullPointerException();
                  }
                }
                // Custom comment - should never happen because invoke returns 123
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  return switch (side) {
                    case HEART, DIAMOND ->
                        /* lhs comment */
                        // rhs comment
                        invoke();
                    case SPADE -> throw new RuntimeException();
                    case CLUB -> throw new NullPointerException();
                  };
                }
                // Custom comment - should never happen because invoke returns 123
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_groupedComments_errorAndNoRemoval() {
    // The switch is exhaustive but doesn't have any statements immediately following it in the
    // lowest ancestor statement block
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART /* lhs comment */: // rhs comment
                    // Another comment
                    case /* sparkly */ DIAMOND /* Sparkly */:
                    // Diamond
                    case SPADE:
                      // Before invoke
                      return invoke();
                    // After invoke
                    case CLUB:
                      throw new NullPointerException();
                      // After last case
                  }
                }
                // Custom comment - should never happen because invoke returns 123 or throws
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  switch (side) {
                    case HEART /* lhs comment */: // rhs comment
                    // Another comment
                    case /* sparkly */ DIAMOND /* Sparkly */:
                    // Diamond
                    case SPADE:
                      // Before invoke
                      return invoke();
                    // After invoke
                    /* More after invoke */
                    case CLUB:
                      throw new NullPointerException();
                      // After last case
                  }
                }
                // Custom comment - should never happen because invoke returns 123 or throws
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                System.out.println("don't delete 0");
                if (invoke() > 0) {
                  System.out.println("don't delete 1");
                  // Preceding comment
                  // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                  return switch (side) {
                    case HEART, DIAMOND, SPADE ->
                        /* lhs comment */
                        // rhs comment
                        // Another comment
                        /* sparkly */
                        /* Sparkly */
                        // Diamond
                        // Before invoke
                        invoke();
                    // After invoke
                    /* More after invoke */
                    case CLUB -> throw new NullPointerException();
                      // After last case
                  };
                }
                // Custom comment - should never happen because invoke returns 123 or throws
                int z = invoke(/* block comment 0 */ );
                {
                  z++;
                }
                throw new RuntimeException("Invoke <= 0 at runtime ");
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void
      switchByEnum_returnSwitchWithShouldNeverHappenInLambda_errorAndRemoveShouldNeverHappen() {
    // Conversion to return switch within a lambda
    assume().that(Runtime.version().feature()).isAtLeast(14);

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                Supplier<Integer> lambda =
                    () -> {
                      // Preceding comment
                      switch (side) {
                        case HEART:
                        // Fall through
                        case DIAMOND:
                          return invoke();
                        case SPADE:
                          throw new RuntimeException();
                        case CLUB:
                          throw new NullPointerException();
                      }
                      // Custom comment - should never happen
                      int z = invoke(/* block comment 0 */ );
                      z++;
                      throw new RuntimeException("Switch was not exhaustive at runtime " + z);
                    };
                System.out.println("don't delete 2");
                return lambda.get();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int invoke() {
                return 123;
              }

              public int foo(Side side) {
                Supplier<Integer> lambda =
                    () -> {
                      // Preceding comment
                      return switch (side) {
                        case HEART, DIAMOND -> invoke();
                        case SPADE -> throw new RuntimeException();
                        case CLUB -> throw new NullPointerException();
                      };
                      // Custom comment - should never happen

                    };
                System.out.println("don't delete 2");
                return lambda.get();
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_returnSwitchVoid_noError() {
    // A void cannot be converted to a return switch
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public void foo(Side side) {
                switch (side) {
                  case HEART:
                  // Fall through
                  case DIAMOND:
                    return;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnLabelledContinue_noError() {
    // Control jumps outside the switch for HEART
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      continue before;
                    case DIAMOND:
                      return 3;
                    case SPADE:
                      throw new RuntimeException();
                    default:
                      throw new NullPointerException();
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnUnlabelledContinue_noError() {
    // Control jumps outside the switch for HEART
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      continue;
                    case DIAMOND:
                      return 3;
                    case SPADE:
                      throw new RuntimeException();
                    default:
                      throw new NullPointerException();
                  }
                }
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnLabelledBreak_noError() {
    // Control jumps outside the switch for HEART
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      break before;
                    case DIAMOND:
                      return 3;
                    case SPADE:
                      throw new RuntimeException();
                    default:
                      throw new NullPointerException();
                  }
                }
                return 0;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_returnYield_noError() {
    // Does not attempt to convert "yield" expressions
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                return switch (side) {
                  case HEART:
                    yield 2;
                  case DIAMOND:
                    yield 3;
                  case SPADE:
                  // Fall through
                  default:
                    throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion"))
        .doTest();
  }

  /**********************************
   *
   * Assignment switch test cases
   *
   **********************************/

  @Test
  public void switchByEnum_assignmentSwitchToLocalHasDefault_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    x = ((x + 1) * (x * x)) << 1;
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                x =
                    switch (side) {
                      case HEART, DIAMOND -> ((x + 1) * (x * x)) << 1;
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedReferences_error() {
    // Must deduce that "x" and "this.x" refer to same thing
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  /* Comment before first case */
                  case /* LHS comment */ HEART:
                    // Inline comment
                    x <<= 2;
                    break;
                  case DIAMOND:
                    this.x <<= (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    // Check correct generated code.
    // Note that suggested fix uses the style of the first case (in source order).
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                switch (side) {
                  /* Comment before first case */
                  case /* LHS comment */ HEART:
                    // Inline comment
                    this.x <<= 2;
                    break;
                  case DIAMOND:
                    x <<= (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                this.x <<=
                    switch (side) {
                      case HEART ->
                          /* Comment before first case */
                          /* LHS comment */
                          // Inline comment
                          2;
                      case DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedReferences_noError() {
    // Must deduce that "x" and "this.y" refer to different things
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x, y;

              public Test(int foo) {
                x = -1;
                y = -1;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                    x = 2;
                    break;
                  case DIAMOND:
                    this.y = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchTwoAssignments_noError() {
    // Can't convert multiple assignments, even if redundant
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                    x = 2;
                    x = 3;
                    break;
                  case DIAMOND:
                    this.x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToSingleArray_error() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    throw new RuntimeException();
                  case DIAMOND:
                    x[6] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Side side) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                    throw new RuntimeException();
                  case DIAMOND:
                    x[6] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Side side) {
                x[6] <<=
                    switch (side) {
                      case HEART -> throw new RuntimeException();
                      case DIAMOND -> (((x[6] + 1) * (x[6] * x[5]) << 1));
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToMultipleArray_noError() {
    // Multiple array dereferences or other non-variable left-hand-side expressions may (in
    // principle) be convertible to assignment switches, but this feature is not supported at this
    // time
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Side side) {
                switch (side) {
                  case HEART:
                    // Inline comment
                    x[6] <<= 2;
                    break;
                  case DIAMOND:
                    x[6] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToMultipleDistinct_noError() {
    // x[5] and x[6] are distinct assignment targets
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Side side) {
                switch (side) {
                  case HEART:
                    // Inline comment
                    x[6] <<= 2;
                    break;
                  case DIAMOND:
                    x[5] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedKinds_noError() {
    // Different assignment types ("=" versus "+=").  The check does not attempt to alter the
    // assignments to make the assignment types match (e.g. does not change to "x = x + 2")
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                switch (side) {
                  case HEART:
                    x += 2;
                    break;
                  case DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  default:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentLabelledContinue_noError() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      x = 2;
                      break;
                    case DIAMOND:
                      x = (((x + 1) * (x * x)) << 1);
                      break;
                    case SPADE:
                      continue before;
                    default:
                      throw new NullPointerException();
                  }
                  break;
                }
                after:
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentLabelledBreak_noError() {
    // Can't convert because of "break before"
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      x = 2;
                      break;
                    case DIAMOND:
                      x = (((x + 1) * (x * x)) << 1);
                      break;
                    case SPADE:
                      break before;
                    default:
                      throw new NullPointerException();
                  }
                  break;
                }
                after:
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentLabelledBreak2_noError() {
    // Can't convert because of "break before" as the second statement in its block
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      x = 2;
                      break;
                    case DIAMOND:
                      x = (((x + 1) * (x * x)) << 1);
                      break;
                    case SPADE:
                      x = 3;
                      break before;
                    default:
                      throw new NullPointerException();
                  }
                  break;
                }
                after:
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentUnlabelledContinue_noError() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Side side) {
                before:
                for (; ; ) {
                  switch (side) {
                    case HEART:
                      x = 2;
                      break;
                    case DIAMOND:
                      x = (((x + 1) * (x * x)) << 1);
                      break;
                    case SPADE:
                      continue;
                    default:
                      throw new NullPointerException();
                  }
                  break;
                }
                after:
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentYield_noError() {
    // Does not attempt to convert "yield" expressions
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x =
                    switch (side) {
                      case HEART:
                        yield 2;
                      case DIAMOND:
                        yield 3;
                      case SPADE:
                      // Fall through
                      default:
                        throw new NullPointerException();
                    };
                x <<= 1;
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_exhaustiveAssignmentSwitch_error() {
    // Transformation can change error handling.  Here, if the enum is not exhaustive at runtime
    // (say there is a new JOKER suit), then nothing would happen.  But the transformed source,
    // would throw.
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                  // Heart comment
                  // Fall through
                  case DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                switch (side) {
                  case HEART:
                  // Heart comment
                  // Fall through
                  case DIAMOND:
                    x = (((x + 1) * (x * x)) << 2);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                x =
                    switch (side) {
                      case HEART, DIAMOND ->
                          // Heart comment
                          (((x + 1) * (x * x)) << 2);
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_exhaustiveCompoundAssignmentSwitch_error() {
    // Verify compound assignments (here, +=)
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    x += (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                switch (side) {
                  case HEART:
                  case DIAMOND:
                    x += (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                x +=
                    switch (side) {
                      case HEART, DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_groupedComments_error() {
    // Verify compound assignments (here, *=) with grouped comments
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case /* red suit */ HEART:
                  // Heart comment
                  case /* red suit */ DIAMOND: // sparkles
                  // Diamond comment
                  // Fall through
                  case /* black suit */ SPADE:
                    x *= 2;
                    // Before break comment
                    break;
                  // After break comment
                  case /* black suit */ CLUB:
                    // Club comment
                    throw new NullPointerException();
                    // Club after throw comment
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case /* red suit */ HEART:
                  // Heart comment
                  case /* red suit */ DIAMOND: // sparkles
                  // Diamond comment
                  // Fall through
                  case /* black suit */ SPADE:
                    x *= 2;
                    // Before break comment
                    break;
                  // After break comment
                  case /* black suit */ CLUB:
                    // Club comment
                    throw new NullPointerException();
                    // Club after throw comment
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                x *=
                    switch (side) {
                      case HEART, DIAMOND, SPADE ->
                          /* red suit */
                          // Heart comment
                          /* red suit */
                          // sparkles
                          // Diamond comment
                          /* black suit */
                          2;
                      // Before break comment
                      // After break comment
                      case CLUB ->
                          /* black suit */
                          // Club comment
                          throw new NullPointerException();
                        // Club after throw comment
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_compoundAssignmentExampleInDocumentation_error() {
    // This code appears as an example in the documentation (added surrounding class)
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              int score = 0;

              public Test() {}

              private void updateScore(Suit suit) {
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (suit) {
                  case HEARTS:
                  // Fall through
                  case DIAMONDS:
                    score += -1;
                    break;
                  case SPADES:
                    score += 2;
                    break;
                  case CLUBS:
                    score += 3;
                    break;
                }
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion")
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              int score = 0;

              public Test() {}

              private void updateScore(Suit suit) {
                switch (suit) {
                  case HEARTS:
                  // Fall through
                  case DIAMONDS:
                    score += -1;
                    break;
                  case SPADES:
                    score += 2;
                    break;
                  case CLUBS:
                    score += 3;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Suit {
                HEARTS,
                CLUBS,
                SPADES,
                DIAMONDS
              };

              int score = 0;

              public Test() {}

              private void updateScore(Suit suit) {
                score +=
                    switch (suit) {
                      case HEARTS, DIAMONDS -> -1;
                      case SPADES -> 2;
                      case CLUBS -> 3;
                    };
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchByEnum_exhaustiveAssignmentSwitchCaseList_error() {
    // Statement switch has cases with multiple values
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]
                switch (side) {
                  case HEART, DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE, CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                switch (side) {
                  case HEART, DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE, CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                x =
                    switch (side) {
                      case HEART, DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE, CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_nonExhaustiveAssignmentSwitch_noError() {
    // No HEART case
    assume().that(Runtime.version().feature()).isAtLeast(14);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum Side {
                HEART,
                SPADE,
                DIAMOND,
                CLUB
              };

              public Test(int foo) {}

              public int foo(Side side) {
                int x = 0;
                switch (side) {
                  case DIAMOND:
                    x = (((x + 1) * (x * x)) << 1);
                    break;
                  case SPADE:
                    throw new RuntimeException();
                  case CLUB:
                    throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            ImmutableList.of(
                "-XepOpt:StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion"))
        .doTest();
  }

  @Test
  public void i4222() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public static void main(String[] args) {
                switch (args.length) {
                  case 0:
                    {
                      System.out.println(0);
                      break;
                    }
                  case 1:
                    System.out.println(1);
                    break;
                  case 2:
                    System.out.println(2);
                    System.out.println(2);
                    break;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public static void main(String[] args) {
                switch (args.length) {
                  case 0 -> System.out.println(0);
                  case 1 -> System.out.println(1);
                  case 2 -> {
                    System.out.println(2);
                    System.out.println(2);
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion=true")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void unnecessaryBreaks() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public static void main(String[] args) {
                switch (args.length) {
                  case 0:
                    System.out.println(0);
                    break;
                  default:
                    // hello
                    // world
                    break;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public static void main(String[] args) {
                switch (args.length) {
                  case 0 -> System.out.println(0);
                  default -> {
                    // hello
                    // world
                  }
                }
              }
            }
            """)
        .setArgs("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion=true")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void mixedExpressionsAndYields() {
    assume().that(Runtime.version().feature()).isAtLeast(14);
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              String f(int x) {
                switch (x) {
                  case 0:
                    return "ZERO";
                  case 1:
                    return "ONE";
                  case 2: // hello
                    // world
                    System.err.println();
                    System.err.println();
                    return "TWO";
                  // hello
                  // world
                  default:
                    return "";
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              String f(int x) {
                return switch (x) {
                  case 0 -> "ZERO";
                  case 1 -> "ONE";
                  case 2 -> {
                    // hello
                    // world
                    System.err.println();
                    System.err.println();
                    yield "TWO";
                  }
                  // hello
                  // world
                  default -> "";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion=true",
            "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion=true")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void fallOutComment() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              String f(int x) {
                switch (x) {
                  case 0:
                    return "ZERO";
                  default: // fall out
                }
                return "";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              String f(int x) {
                switch (x) {
                  case 0 -> {
                    return "ZERO";
                  }
                  default -> {}
                }
                return "";
              }
            }
            """)
        .setArgs(
            "-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion=true",
            "-XepOpt:StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion=true")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }
}
