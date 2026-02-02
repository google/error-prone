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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.fixes.Fix;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link RefactorSwitch}. */
@RunWith(JUnit4.class)
public final class RefactorSwitchTest {
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
      CompilationTestHelper.newInstance(RefactorSwitch.class, getClass())
          .addSourceLines("Suit.java", SUIT);
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(RefactorSwitch.class, getClass())
          .addInputLines("Suit.java", SUIT)
          .expectUnchanged();
  private final BugCheckerRefactoringTestHelper refactoringHelper2 =
      BugCheckerRefactoringTestHelper.newInstance(RefactorSwitch.class, getClass())
          .addInputLines("Suit.java", SUIT)
          .expectUnchanged();

  @Test
  public void refactorChain_returnSwitchExhaustive_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;
            import java.util.HashSet;
            import java.util.Set;

            class Test {
              public String foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                Set<String> set = new HashSet<>();
                switch (i) {
                  case 1 -> throw new NullPointerException("aaa");
                  case 2 -> {
                    System.out.println("hello");
                    if (true) {
                      return "bbb";
                    }
                    for (int j = 0; j < 10; j++) {
                      if (i == 0) {
                        return "for";
                      }
                      for (String setString : set) {
                        return "enhancedfor";
                      }
                      while (j > 10) {
                        return "while";
                      }
                      do {
                        if (i == 0) {
                          return "dowhile";
                        }
                      } while (false);
                    }
                    Supplier<String> supplier =
                        () -> {
                          return "lam";
                        };
                    return "aaa";
                  }
                  case 3 -> {
                    {
                      var newClass =
                          new Comparable<Integer>() {
                            @Override
                            public int compareTo(Integer other) {
                              return 0;
                            }
                          };
                      return newClass == null ? "ccc" : "ddd";
                    }
                  }
                  case 4 -> /* a */ { // b
                    /* c */ return i == 0 ? "zero" : "nonzero"; // d
                  }
                  case 5 -> {
                    throw new NullPointerException("unnecessary braces");
                  }
                  case 6 -> { // don't remove comment
                    {
                      throw new NullPointerException("multiple unnecessary braces");
                    }
                  }
                  default -> {
                    {
                      return "goodbye";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;
            import java.util.HashSet;
            import java.util.Set;

            class Test {
              public String foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                Set<String> set = new HashSet<>();
                return switch (i) {
                  case 1 -> throw new NullPointerException("aaa");
                  case 2 -> {
                    System.out.println("hello");
                    if (true) {
                      yield "bbb";
                    }
                    for (int j = 0; j < 10; j++) {
                      if (i == 0) {
                        yield "for";
                      }
                      for (String setString : set) {
                        yield "enhancedfor";
                      }
                      while (j > 10) {
                        yield "while";
                      }
                      do {
                        if (i == 0) {
                          yield "dowhile";
                        }
                      } while (false);
                    }
                    Supplier<String> supplier =
                        () -> {
                          return "lam";
                        };
                    yield "aaa";
                  }
                  case 3 -> {
                    var newClass =
                        new Comparable<Integer>() {
                          @Override
                          public int compareTo(Integer other) {
                            return 0;
                          }
                        };
                    yield newClass == null ? "ccc" : "ddd";
                  }

                  case 4 -> /* a */
                      // b
                      /* c */
                      // d
                      i == 0 ? "zero" : "nonzero";
                  case 5 -> throw new NullPointerException("unnecessary braces");
                  case 6 ->
                      // don't remove comment
                      throw new NullPointerException("multiple unnecessary braces");
                  default -> "goodbye";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_hasContinue_noError() {
    // Continuing out of a switch statement is allowed, but continuing out of a switch expression is
    // not.
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                for (int i = 0; i < 10; i++) {
                  switch (suit) {
                    case HEART, CLUB, SPADE -> {
                      return true;
                    }
                    case DIAMOND -> {
                      if (i > 99) {
                        continue;
                      }
                      return false;
                    }
                    default -> {
                      return false;
                    }
                  }
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_hasLabelledContinue_noError() {
    // Same as switchByEnum_hasContinue_noError except that an explicit label is used.
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                outer:
                for (int i = 0; i < 10; i++) {
                  switch (suit) {
                    case HEART, CLUB, SPADE -> {
                      return true;
                    }
                    case DIAMOND -> {
                      if (i > 99) {
                        continue outer;
                      }
                      return false;
                    }
                    default -> {
                      return false;
                    }
                  }
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_hasContinueWithinSwitch_error() {
    // Continue is okay in a switch statement, provide it doesn't continue out of the switch.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                for (int i = 0; i < 10; i++) {
                  switch (suit) {
                    case HEART, CLUB, SPADE -> {
                      return true;
                    }
                    case DIAMOND -> {
                      for (int q = 0; q < 10; q++) {
                        if (i > 99) {
                          continue;
                        }
                      }
                      return false;
                    }
                    default -> {
                      return false;
                    }
                  }
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                for (int i = 0; i < 10; i++) {
                  return switch (suit) {
                    case HEART, CLUB, SPADE -> true;
                    case DIAMOND -> {
                      for (int q = 0; q < 10; q++) {
                        if (i > 99) {
                          continue;
                        }
                      }
                      yield false;
                    }
                    default -> false;
                  };
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_hasLabelledContinueWithinSwitch_error() {
    // Continue is okay in a switch statement, provide it doesn't continue out of the switch.
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                outer:
                for (int i = 0; i < 10; i++) {
                  switch (suit) {
                    case HEART, CLUB, SPADE -> {
                      return true;
                    }
                    case DIAMOND -> {
                      a:
                      for (int q = 0; q < 10; q++) {
                        if (i > 99) {
                          continue a;
                        }
                      }
                      return false;
                    }
                    default -> {
                      return false;
                    }
                  }
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                outer:
                for (int i = 0; i < 10; i++) {
                  return switch (suit) {
                    case HEART, CLUB, SPADE -> true;
                    case DIAMOND -> {
                      a:
                      for (int q = 0; q < 10; q++) {
                        if (i > 99) {
                          continue a;
                        }
                      }
                      yield false;
                    }
                    default -> false;
                  };
                  // Here is not reachable
                }
                return false;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_hasLabelledContinueOutOfSwitch_noError() {
    // Labelled continue that jumps out of the switch statement
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                for (; ; ) {
                  outer:
                  while (true) {
                    for (int i = 0; i < 10; i++) {
                      switch (suit) {
                        case HEART, CLUB, SPADE -> {
                          return true;
                        }
                        case DIAMOND -> {
                          a:
                          for (int q = 0; q < 10; q++) {
                            if (i > 99) {
                              continue outer;
                            }
                          }
                          return false;
                        }
                        default -> {
                          return false;
                        }
                      }
                      // Here is not reachable
                    }
                  }
                }
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void refactorChain_returnSwitchEnumRemoveDefault_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  default -> {
                    {
                      return "should suggest to remove";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  default -> "should suggest to remove";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  default -> {
                    {
                      return "should suggest to remove";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_javaDoc_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum SideOfCoin {
                OBVERSE,
                REVERSE
              };

              private String renderName(SideOfCoin sideOfCoin) {
                switch (sideOfCoin) {
                  case OBVERSE -> {
                    return "Heads";
                  }
                  case REVERSE -> {
                    return "Tails";
                  }
                }
                // This should never happen, but removing this will cause a compile-time error
                throw new RuntimeException("Unknown side of coin");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum SideOfCoin {
                OBVERSE,
                REVERSE
              };

              private String renderName(SideOfCoin sideOfCoin) {
                return switch (sideOfCoin) {
                  case OBVERSE -> "Heads";
                  case REVERSE -> "Tails";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_javaDocPreservesComments_error() {
    // Preserves comments that should not be removed
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              enum SideOfCoin {
                OBVERSE,
                REVERSE
              };

              private String renderName(SideOfCoin sideOfCoin) {
                switch (sideOfCoin) {
                  case OBVERSE -> {
                    return "Heads";
                  }
                  case REVERSE -> {
                    return "Tails";
                  }
                }
                // This should never happen, but removing this will cause a compile-time error
                throw new RuntimeException("Unknown side of coin");
                // LINT. Don't remove this
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              enum SideOfCoin {
                OBVERSE,
                REVERSE
              };

              private String renderName(SideOfCoin sideOfCoin) {
                return switch (sideOfCoin) {
                  case OBVERSE -> "Heads";
                  case REVERSE -> "Tails";
                };
                // This should never happen, but removing this will cause a compile-time error

                // LINT. Don't remove this
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_returnSwitchNullDefaultRemoveDefault_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      return "should suggest to remove";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null, default -> "should suggest to remove";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      return "should suggest to remove";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null -> "should suggest to remove";
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_returnSwitchNullDefaultThrowRemoveDefault_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      throw new NullPointerException("should suggest to remove");
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null, default -> throw new NullPointerException("should suggest to remove");
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      throw new NullPointerException("should suggest to remove");
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null -> throw new NullPointerException("should suggest to remove");
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_returnSwitchNullDefaultGenericBlockRemoveDefault_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      System.out.println("should suggest to remove default");
                      return "should suggest to remove default";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null, default -> {
                    System.out.println("should suggest to remove default");
                    yield "should suggest to remove default";
                  }
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> {
                    {
                      return "black";
                    }
                  }
                  case null, default -> {
                    {
                      System.out.println("should suggest to remove default");
                      return "should suggest to remove default";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART, DIAMOND -> throw new NullPointerException("red");
                  case SPADE, CLUB -> "black";
                  case null -> {
                    System.out.println("should suggest to remove default");
                    yield "should suggest to remove default";
                  }
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_returnSwitchNonExhaustive_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                switch (i) {
                  case 1 -> throw new NullPointerException("one");
                  case 2 -> {
                    return "two";
                  }
                }
                return "default";
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void refactorChain_returnSwitchEmpty_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                Integer i = s == null ? 1 : 2;
                switch (i) {
                }
                return "default";
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_returnSwitch_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int invoke() {
                return 123;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return invoke();
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int invoke() {
                return 123;
              }

              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> invoke();
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_multipleStatementsAndTheLastNotReturn_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_casePatternAndGuard_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE -> {
                    System.out.println("spade");
                    throw new RuntimeException();
                  }
                  case CLUB -> {
                    throw new NullPointerException();
                  }
                  case Suit s when s == Suit.HEART -> throw new NullPointerException();
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE -> {
                    System.out.println("spade");
                    throw new RuntimeException();
                  }
                  case CLUB -> throw new NullPointerException();
                  case Suit s when s == Suit.HEART -> throw new NullPointerException();
                  default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE -> {
                    System.out.println("spade");
                    throw new RuntimeException();
                  }
                  case CLUB -> {
                    throw new NullPointerException();
                  }
                  case Suit s when s == Suit.HEART -> throw new NullPointerException();
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE -> {
                    System.out.println("spade");
                    throw new RuntimeException();
                  }
                  case CLUB -> throw new NullPointerException();
                  case Suit s when s == Suit.HEART -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_nullGroupedWithDefault_error() {
    // Null can be grouped with default
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null, default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null, default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnumReturnSwitch_nullDefaultSameProduction_error() {
    // Null can be grouped together with default in a single SwitchLabelProduction in Java 21+
    // as `case null [, default]`
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE, CLUB -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null, default -> {
                    throw new NullPointerException();
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE, CLUB -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null, default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE, CLUB -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null, default -> {
                    throw new NullPointerException();
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE, CLUB -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_middleNullCase3_error() {
    // null case is converted without being grouped with default
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return 1;
                  }
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null -> throw new RuntimeException("single null case");
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case HEART, DIAMOND -> 1;
                  case SPADE -> {
                    System.out.println("hello");
                    throw new RuntimeException();
                  }
                  case null -> throw new RuntimeException("single null case");
                  default -> throw new NullPointerException();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_exhaustiveWithDefault_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int invoke() {
                return 123;
              }

              public int foo(Suit suit) {
                String z = "dkfj";
                switch (z) {
                  case "", "DIAMOND", "SPADE" -> {
                    return invoke();
                  }
                  default -> {
                    return 2;
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int invoke() {
                return 123;
              }

              public int foo(Suit suit) {
                String z = "dkfj";
                return switch (z) {
                  case "", "DIAMOND", "SPADE" -> invoke();
                  default -> 2;
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_defaultFallThru_noError() {
    // No error because default doesn't return anything within its block
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int invoke() {
                return 123;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return invoke();
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> {}
                }
                return -2;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_returnSwitchVoid_noError() {
    // A void cannot be converted to a return switch
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void foo(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    return;
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_returnYield_noError() {
    // Does not attempt to convert "yield" expressions in colon-style switches
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
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
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnumExhaustive_qualifiedCaseLabels() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                switch (suit) {
                  case Suit.HEART -> {
                    return 1;
                  }
                  case Suit.CLUB -> {
                    return 2;
                  }
                  case Suit.DIAMOND -> {
                    return 3;
                  }
                  case Suit.SPADE -> {
                    return 4;
                  }
                }
                throw new AssertionError();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                return switch (suit) {
                  case Suit.HEART -> 1;
                  case Suit.CLUB -> 2;
                  case Suit.DIAMOND -> 3;
                  case Suit.SPADE -> 4;
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  /***********************************************************************************************
   * Assignment switch tests
   **********************************************************************************************/

  @Test
  public void switchByEnum_assignmentSwitchWithComments_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.annotation.Repeatable;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;

            class Test {
              @interface MyAnnos {
                Test.MyAnno[] value();
              }

              @Repeatable(Test.MyAnnos.class)
              @interface MyAnno {
                String v() default "";
              }

              @interface MyOtherAnno {}

              public int y = 0;

              public int foo(Suit suit) {
                @MyAnno(v = "foo")
                // alpha
                /* beta */ @MyOtherAnno
                @MyAnno
                final /* chi */ int /* gamma */ x /* delta */; // epsilon
                // zeta
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = ((y + 1) * (y * y)) << 1;
                  }
                  case SPADE -> // Spade reason
                      throw new RuntimeException();
                  default -> {
                    {
                      throw new NullPointerException();
                    }
                  }
                }
                Map<? extends String, ? super Test> map = null;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    map = new HashMap<>();
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.annotation.Repeatable;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;

            class Test {
              @interface MyAnnos {
                Test.MyAnno[] value();
              }

              @Repeatable(Test.MyAnnos.class)
              @interface MyAnno {
                String v() default "";
              }

              @interface MyOtherAnno {}

              public int y = 0;

              public int foo(Suit suit) {
                // epsilon
                // zeta
                // alpha
                /* beta */
                /* chi */
                /* gamma */
                /* delta */
                @MyAnno(v = "foo")
                @MyOtherAnno
                @MyAnno
                final int x =
                    switch (suit) {
                      case HEART, DIAMOND -> ((y + 1) * (y * y)) << 1;
                      case SPADE -> // Spade reason
                          throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };

                Map<? extends String, ? super Test> map =
                    switch (suit) {
                      case HEART, DIAMOND -> new HashMap<>();
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void variableInTransitiveEnclosingBlock_shouldNotBeMoved() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.HashMap;
            import java.util.Map;

            class Test {
              public Map<Object, Object> foo(Suit suit) {
                Map<Object, Object> map = null;
                if (toString().length() == 2)
                  switch (suit) {
                    case HEART, DIAMOND -> {
                      map = new HashMap<>();
                    }
                    case SPADE -> throw new RuntimeException();
                    default -> throw new NullPointerException();
                  }
                return map;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.HashMap;
            import java.util.Map;

            class Test {
              public Map<Object, Object> foo(Suit suit) {
                Map<Object, Object> map = null;
                if (toString().length() == 2)
                  map =
                      switch (suit) {
                        case HEART, DIAMOND -> new HashMap<>();
                        case SPADE -> throw new RuntimeException();
                        default -> throw new NullPointerException();
                      };
                return map;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToNearbyDefined_error() {
    // The switch block cannot be combined with the variable declaration for {@code x} because the
    // variable declaration is nearby, but not immediately preceding the switch block.

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int z = 3;
                int x;
                int y;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = ((z + 1) * (z * z)) << 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int z = 3;
                int x;
                int y;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> ((z + 1) * (z * z)) << 1;
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchDifferentBlockScope_error() {
    // Local variable {@code x} is defined before the switch block, but at a different (parent)
    // block scope than the switch block.  Therefore, it should not be combined with the switch
    // assignment.

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int z = 3;
                int x;
                {
                  {
                    switch (suit) {
                      case HEART, DIAMOND -> {
                        x = ((z + 1) * (z * z)) << 1;
                      }
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    }
                  }
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int z = 3;
                int x;
                {
                  {
                    x =
                        switch (suit) {
                          case HEART, DIAMOND -> ((z + 1) * (z * z)) << 1;
                          case SPADE -> throw new RuntimeException();
                          default -> throw new NullPointerException();
                        };
                  }
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchToLocalHasDefaultVolatile_error() {
    // Local variable {@code x} is initialized by reading a {@code volatile} field, which includes
    // inter-thread action effects (refer to e.g. JLS 21 § 17.4.2); therefore, should not be
    // combined with the switch assignment because those effects could be different from the
    // original source code.  See also e.g. Shuyang Liu, John Bender, and Jens Palsberg. Compiling
    // Volatile Correctly in Java. In 36th European Conference on Object-Oriented Programming (ECOOP
    // 2022). Leibniz International Proceedings in Informatics (LIPIcs), Volume 222, pp. 6:1-6:26,
    // Schloss Dagstuhl – Leibniz-Zentrum für Informatik (2022)

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              volatile int v = 0;

              public int foo(Suit suit) {
                int z = 3;
                int x = v;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = ((z + 1) * (z * z)) << 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              volatile int v = 0;

              public int foo(Suit suit) {
                int z = 3;
                int x = v;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> ((z + 1) * (z * z)) << 1;
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchVarEnum_error() {
    // var type + enum value merging with definition
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                var suit2 = Suit.SPADE;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    suit2 = Suit.SPADE;
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return suit2 == null ? 0 : 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {

                var suit2 =
                    switch (suit) {
                      case HEART, DIAMOND -> Suit.SPADE;
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return suit2 == null ? 0 : 1;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchVarConstant_error() {
    // var type + compile time constant merging with definition
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                var rv = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    rv = 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return rv;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {

                var rv =
                    switch (suit) {
                      case HEART, DIAMOND -> 1;
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return rv;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedReferences_error() {
    // Must deduce that "x" and "this.x" refer to same thing
    // Note that suggested fix uses the style of the first case (in source order).
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  /* Comment before first case */
                  case /* LHS comment */ HEART -> {
                    this.x <<= 2;
                  }
                  case DIAMOND -> {
                    x <<= (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Suit suit) {
                this.x <<=
                    switch (suit) {
                      /* Comment before first case */
                      case /* LHS comment */ HEART -> 2;
                      case DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedReferences_noError() {
    // Must deduce that "x" and "this.y" refer to different things
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int x, y;

              public Test(int foo) {
                x = -1;
                y = -1;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    x = 2;
                  }
                  case DIAMOND -> {
                    this.y = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_sameVariableDeclaratorBlock_error() {
    // x and y are declared in the same VariableDeclaratorList, therefore cannot be combined,
    // however assignment switch conversion can still be applied
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int y = 0, x;
                switch (suit) {
                  case HEART -> {
                    x = 2;
                  }
                  case DIAMOND -> {
                    x = (((y + 1) * (y * y)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int y = 0, x;
                x =
                    switch (suit) {
                      case HEART -> 2;
                      case DIAMOND -> (((y + 1) * (y * y)) << 1);
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return 0;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_declaredTwoLinesAbove_error() {
    // Variable `x` is declared two lines above the switch statement, therefore cannot be combined,
    // but the assignment switch conversion can still be applied
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x;
                int y;
                switch (suit) {
                  case HEART -> {
                    x = 2;
                  }
                  case DIAMOND -> {
                    x = "hello".length();
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x;
                int y;
                x =
                    switch (suit) {
                      case HEART -> 2;
                      case DIAMOND -> "hello".length();
                      case SPADE -> throw new RuntimeException();
                      default -> throw new NullPointerException();
                    };
                return 0;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchTwoAssignments_noError() {
    // Can't convert multiple assignments, even if redundant
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    x = 2;
                    x = 3;
                  }
                  case DIAMOND -> {
                    this.x = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToSingleArray_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Suit suit) {
                switch (suit) {
                  case HEART -> throw new RuntimeException();
                  case DIAMOND -> {
                    x[6] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Suit suit) {
                x[6] <<=
                    switch (suit) {
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
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToMultipleArray_noError() {
    // Multiple array dereferences or other non-variable left-hand-suit expressions may (in
    // principle) be convertible to assignment switches, but this feature is not supported at this
    // time
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    // Inline comment
                    x[6] <<= 2;
                  }
                  case DIAMOND -> {
                    x[6] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchToMultipleDistinct_noError() {
    // x[5] and x[6] are distinct assignment targets
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int[] x;

              public Test(int foo) {
                x = null;
              }

              public int[] foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    // Inline comment
                    x[6] <<= 2;
                  }
                  case DIAMOND -> {
                    x[5] <<= (((x[6] + 1) * (x[6] * x[5]) << 1));
                  }
                  case SPADE -> {
                    throw new RuntimeException();
                  }
                  default -> {
                    throw new NullPointerException();
                  }
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentSwitchMixedKinds_noError() {
    // Different assignment types ("=" versus "+=").  The check does not attempt to alter the
    // assignments to make the assignment types match (e.g. does not change to "x = x + 2")
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    x += 2;
                  }
                  case DIAMOND -> {
                    x = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  default -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_assignmentLabelledContinue_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int x;

              public Test(int foo) {
                x = -1;
              }

              public int foo(Suit suit) {
                before:
                for (; ; ) {
                  switch (suit) {
                    case HEART -> {
                      x = 2;
                    }
                    case DIAMOND -> {
                      x = (((x + 1) * (x * x)) << 1);
                    }
                    case SPADE -> {
                      continue before;
                    }
                    default -> throw new NullPointerException();
                  }
                  break;
                }
                after:
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_exhaustiveAssignmentSwitch_error() {
    // Transformation can change error handling.  Here, if the enum is not exhaustive at runtime
    // (say there is a new JOKER suit), then nothing would happen.  But the transformed source,
    // would throw.

    // Note also that the initial value of {@code x} is used in the computation inside the switch,
    // thus its definition is not eligible to be combined with the switch (e.g. {@code int x =
    // switch (...)}).
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = (((x + 1) * (x * x)) << 2);
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> (((x + 1) * (x * x)) << 2);
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        // There should be no second fix that attempts to remove the default case because there is
        // no default case.
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnumAssignment_nullDefaultSameProduction_error() {
    // Null can be grouped together with default in a single SwitchLabel production in Java 21+
    // as `case null [, default]`
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = x + 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                  case null, default -> throw new IllegalArgumentException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> x + 1;
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                      case null, default -> throw new IllegalArgumentException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = x + 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                  case null, default -> throw new IllegalArgumentException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> x + 1;
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                      case null -> throw new IllegalArgumentException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnumAssignment_fullyRemovesDefaultCase_error() {
    // Removes default case and its code entirely
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = x + 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                  default -> throw new IllegalArgumentException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> x + 1;
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                      default -> throw new IllegalArgumentException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = x + 1;
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                  default -> throw new IllegalArgumentException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> x + 1;
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_exhaustiveCompoundAssignmentSwitch_error() {
    // Verify compound assignments (here, +=)
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x += (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x +=
                    switch (suit) {
                      case HEART, DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE -> throw new RuntimeException();
                      case CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_compoundAssignmentExampleInDocumentation_error() {
    // This code appears as an example in the documentation (added surrounding class)
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              int score = 0;

              private void updateScore(Suit suit) {
                switch (suit) {
                  case HEART, DIAMOND -> {
                    score += -1;
                  }
                  case SPADE -> {
                    score += 2;
                  }
                  case CLUB -> {
                    score += 3;
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int score = 0;

              private void updateScore(Suit suit) {
                score +=
                    switch (suit) {
                      case HEART, DIAMOND -> -1;
                      case SPADE -> 2;
                      case CLUB -> 3;
                    };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_exhaustiveAssignmentSwitchCaseList_error() {
    // Statement switch has cases with multiple values
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case HEART, DIAMOND -> {
                    x = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE, CLUB -> {
                    throw new NullPointerException();
                  }
                }
                return x;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                x =
                    switch (suit) {
                      case HEART, DIAMOND -> (((x + 1) * (x * x)) << 1);
                      case SPADE, CLUB -> throw new NullPointerException();
                    };
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest();
  }

  @Test
  public void switchByEnum_nonExhaustiveAssignmentSwitch_noError() {
    // No HEART case
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case DIAMOND -> {
                    x = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_switchInSwitch_noError() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                Integer i = suit == null ? 0 : 1;
                switch (i) {
                  case 0:
                    break;
                  default:
                    switch (suit) {
                      case HEART -> {
                        return true;
                      }
                      default -> {
                        return true;
                      }
                    }
                }
                return false;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=true",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  /***********************************************************************************************
   * Simplify switch tests
   **********************************************************************************************/

  @Test
  public void switchByEnum_simplify_noError() {
    // No HEART case
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case DIAMOND -> {
                    x = (((x + 1) * (x * x)) << 1);
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .doTest();
  }

  @Test
  public void switchByEnum_switchStatementWithinSwitchStatement_noError() {
    // Arrow-style switch within an arrow-style switch, both statement switches
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int foo(Suit suit) {
                int x = 0;
                switch (suit) {
                  case DIAMOND -> {
                    switch (x) {
                      case 1 -> {
                        x = 1;
                      }
                      case 2 -> {
                        x = 2;
                      }
                      case 3 -> {
                        x = 3;
                      }
                    }
                  }
                  case SPADE -> throw new RuntimeException();
                  case CLUB -> throw new NullPointerException();
                }
                return x;
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .doTest();
  }

  @Test
  public void refactorChain_simplify_error() {

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                    yield "diamond";
                  }
                  case SPADE -> {
                    {
                      Supplier<String> supplier =
                          () -> {
                            {
                              return "black";
                            }
                          };
                      yield "black";
                    }
                  }
                  case CLUB -> {
                    {
                      yield "also black";
                    }
                  }
                  case null, default -> {
                    {
                      System.out.println("should suggest to remove default");
                      yield "should suggest to remove default";
                    }
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                    yield "diamond";
                  }
                  case SPADE -> {
                    Supplier<String> supplier =
                        () -> {
                          {
                            return "black";
                          }
                        };
                    yield "black";
                  }

                  case CLUB -> "also black";
                  case null, default -> {
                    System.out.println("should suggest to remove default");
                    yield "should suggest to remove default";
                  }
                };
              }
            }

            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                    yield "diamond";
                  }
                  case SPADE -> {
                    {
                      Supplier<String> supplier =
                          () -> {
                            {
                              return "black";
                            }
                          };
                      yield "black";
                    }
                  }
                  case CLUB -> {
                    {
                      yield "also black";
                    }
                  }
                  case null, default -> {
                    {
                      System.out.println("should suggest to remove default");
                      yield "should suggest to remove default";
                    }
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                    yield "diamond";
                  }
                  case SPADE -> {
                    Supplier<String> supplier =
                        () -> {
                          {
                            return "black";
                          }
                        };
                    yield "black";
                  }

                  case CLUB -> "also black";
                  case null -> {
                    System.out.println("should suggest to remove default");
                    yield "should suggest to remove default";
                  }
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_cannotSimplifyIf_error() {
    // The "if" block cannot be simplified into an expression

    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {

              public String makeString() {
                return "foo";
              }

              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> makeString();
                  case DIAMOND, SPADE -> {
                    yield makeString();
                  }
                  case CLUB -> {
                    if (true) {
                      yield makeString();
                    } else {
                      System.out.println("Desktop MinuteMaid URLs not supported for flow: " + s);
                      yield makeString();
                    }
                  }
                  default -> {
                    yield makeString();
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {

              public String makeString() {
                return "foo";
              }

              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> makeString();
                  case DIAMOND, SPADE -> makeString();
                  case CLUB -> {
                    if (true) {
                      yield makeString();
                    } else {
                      System.out.println("Desktop MinuteMaid URLs not supported for flow: " + s);
                      yield makeString();
                    }
                  }
                  default -> makeString();
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {

              public String makeString() {
                return "foo";
              }

              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> makeString();
                  case DIAMOND, SPADE -> {
                    yield makeString();
                  }
                  case CLUB -> {
                    if (true) {
                      yield makeString();
                    } else {
                      System.out.println("Desktop MinuteMaid URLs not supported for flow: " + s);
                      yield makeString();
                    }
                  }
                  default -> {
                    yield makeString();
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {

              public String makeString() {
                return "foo";
              }

              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> makeString();
                  case DIAMOND, SPADE -> makeString();
                  case CLUB -> {
                    if (true) {
                      yield makeString();
                    } else {
                      System.out.println("Desktop MinuteMaid URLs not supported for flow: " + s);
                      yield makeString();
                    }
                  }
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_simplifyBraces_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> {
                    System.out.println("diamond");
                  }
                  case null, default -> {
                    {
                      System.out.println("others");
                    }
                  }
                }
                return "";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> throw new NullPointerException("heart");
                  case DIAMOND -> System.out.println("diamond");

                  case null, default -> System.out.println("others");
                }
                return "";
              }
            }

            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_simplifyBracesPartial_error() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> {
                    {
                      throw new NullPointerException("heart");
                    }
                  }
                  case DIAMOND -> {
                    {
                      yield "diamond";
                    }
                  }
                  case SPADE, CLUB -> "black";
                  case null, default -> {
                    {
                      yield "others";
                    }
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");

                  case DIAMOND -> "diamond";
                  case SPADE, CLUB -> "black";
                  case null, default -> "others";
                };
              }
            }

            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> {
                    {
                      throw new NullPointerException("heart");
                    }
                  }
                  case DIAMOND -> {
                    {
                      yield "diamond";
                    }
                  }
                  case SPADE, CLUB -> "black";
                  case null, default -> {
                    {
                      yield "others";
                    }
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case HEART -> throw new NullPointerException("heart");

                  case DIAMOND -> "diamond";
                  case SPADE, CLUB -> "black";
                  case null -> "others";
                };
              }
            }

            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_simplifyBracesPartialSwitchStatement_error() {
    // We are not testing the return switch conversion here, just simplifying a switch statement
    // (includes removing the default case)
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> {
                    {
                      throw new NullPointerException("heart");
                    }
                  }
                  case DIAMOND -> {
                    {
                      return "diamond";
                    }
                  }
                  case SPADE, CLUB -> {
                    return "black";
                  }
                  case null, default -> {
                    {
                      return "others";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> throw new NullPointerException("heart");

                  case DIAMOND -> {
                    return "diamond";
                  }

                  case SPADE, CLUB -> {
                    return "black";
                  }
                  case null, default -> {
                    return "others";
                  }
                }
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(FixChoosers.FIRST)
        .doTest(TEXT_MATCH);

    refactoringHelper2
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> {
                    {
                      throw new NullPointerException("heart");
                    }
                  }
                  case DIAMOND -> {
                    {
                      return "diamond";
                    }
                  }
                  case SPADE, CLUB -> {
                    return "black";
                  }
                  case null, default -> {
                    {
                      return "others";
                    }
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                switch (s) {
                  case HEART -> throw new NullPointerException("heart");

                  case DIAMOND -> {
                    return "diamond";
                  }

                  case SPADE, CLUB -> {
                    return "black";
                  }
                  case null -> {
                    return "others";
                  }
                }
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(RefactorSwitchTest::assertSecondAndLastFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactorChain_simplifyBracesCannotRemoveDefault_error() {
    // HEART case is missing, so default cannot be removed in secondary fix
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case DIAMOND -> {
                    {
                      yield "diamond";
                    }
                  }
                  case SPADE, CLUB -> "black";
                  case null, default -> {
                    {
                      yield "others";
                    }
                  }
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Supplier;

            class Test {
              public String foo(Suit s) {
                return switch (s) {
                  case DIAMOND -> "diamond";
                  case SPADE, CLUB -> "black";
                  case null, default -> "others";
                };
              }
            }

            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .setFixChooser(RefactorSwitchTest::assertOneFixAndChoose)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void switchByEnum_cannotSimplify_noError() {
    // HEART case has a size-one block, but cannot be simplified because it has an if statement (we
    // don't attempt to convert simple if statements into ternaries)
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                return switch (suit) {
                  case HEART -> {
                    if (true) {
                      yield true;
                    } else {
                      yield false;
                    }
                  }
                  case DIAMOND -> true;
                  case SPADE -> true;
                  default -> true;
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=true")
        .doTest();
  }

  /***********************************************************************************************
   * Disabled tests
   **********************************************************************************************/

  @Test
  public void switchByEnum_allFlagsDisabledSwitch_noError() {
    // The switch could be refactored if not disabled by flags
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                switch (suit) {
                  case HEART -> {
                    return true;
                  }
                  case DIAMOND -> {
                    return true;
                  }
                  case SPADE -> {
                    return true;
                  }
                  default -> {
                    return true;
                  }
                }
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
  }

  @Test
  public void switchByEnum_allFlagsDisabledSwitchExpr_noError() {
    // The switch could be refactored if not disabled by flags
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean foo(Suit suit) {
                return switch (suit) {
                  case HEART -> {
                    yield true;
                  }
                  case DIAMOND -> true;
                  case SPADE -> true;
                  default -> true;
                };
              }
            }
            """)
        .setArgs(
            "-XepOpt:RefactorSwitch:EnableAssignmentSwitch=false",
            "-XepOpt:RefactorSwitch:EnableReturnSwitch=false",
            "-XepOpt:RefactorSwitch:EnableSimplifySwitch=false")
        .doTest();
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

  /** Asserts that there are exactly two suggested fixes and returns the second one. */
  public static Fix assertSecondAndLastFixAndChoose(List<Fix> fixes) {
    assertThat(fixes).hasSize(2);
    return fixes.get(1);
  }
}
