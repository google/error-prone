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

package com.google.errorprone.util;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability.CanCompleteNormallyPatch;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** {@link Reachability}Test. */
@RunWith(TestParameterInjector.class)
public class ReachabilityTest {

  /** Reports an error if the first case in a switch falls through to the second. */
  @BugPattern(summary = "", severity = ERROR)
  public static class FirstCaseFallsThrough extends BugChecker implements SwitchTreeMatcher {

    @Override
    public Description matchSwitch(SwitchTree tree, VisitorState state) {
      if (tree.getCases().size() != 2 || tree.getCases().get(0).getStatements().isEmpty()) {
        return NO_MATCH;
      }
      return Reachability.canCompleteNormally(
              Iterables.getLast(tree.getCases().get(0).getStatements()))
          ? describeMatch(tree.getCases().get(1))
          : NO_MATCH;
    }
  }

  /**
   * Returns a test that {@link Reachability} and javac agree on whether {@code statement} can
   * complete normally. {@code expected} is the ground truth of whether the {@code statement} can
   * complete normally.
   *
   * <p>The {@code statement} may use (but doesn't need to use) the parameters {@code x}, {@code o},
   * {@code shape}, {@code paint} and {@code color}, of the types declared in the generated source.
   */
  private CompilationTestHelper canCompleteNormallyTest(boolean expected, String statement) {
    return CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              sealed interface Shape {}

              record Circle() implements Shape {}

              record Square() implements Shape {}

              record Triangle() implements Shape {}

              sealed interface Paint {}

              enum Color implements Paint {
                RED,
                GREEN,
                BLUE
              }

              void f(int x, Object o, Shape shape, Paint paint, Color color) {
                switch (x) {
                  case 1:
            %s
                  %s
                  default:
                    break;
                }
              }

              %s javacAgrees(int x, Object o, Shape shape, Paint paint, Color color) {
            %s
                %s
              }
            }
            """
                .formatted(
                    statement,
                    expected ? "// BUG: Diagnostic contains:" : "",
                    expected ? "void" : "int",
                    statement,
                    expected ? "System.out.println(\"reached\");" : ""));
  }

  @Test
  public void test(
      @TestParameter({
            """
            int a = 1;
            int b = 2;
            break;
            """,
            """
            System.err.println();
            // BUG: Diagnostic contains:
            """,
            """
            int a = 1;
            int b = 2;
            class L {}
            ;;
            assert false;
            label: System.err.println();
            // BUG: Diagnostic contains:
            """,
            """
            if (true) {
            } else {
              break;
            }
            // BUG: Diagnostic contains:
            """,
            """
            if (true) {
              break;
            }
            // BUG: Diagnostic contains:
            """,
            """
            if (true) {
              break;
            } else {
            }
            // BUG: Diagnostic contains:
            """,
            """
            if (true) {
              break;
            } else {
              break;
            }
            """,
            """
            switch (42) {
              case 0:
              case 1:
              case 2:
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              case 0:
                break;
              case 1:
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              default:
                break;
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              default:
                return;
            }
            """,
            """
            while (true) {
            }
            """,
            """
            while (true) {
              break;
            }
            // BUG: Diagnostic contains:
            """,
            """
            while (true) {
              return;
            }
            """,
            """
            while (x == 0) {}
            // BUG: Diagnostic contains:
            """,
            """
            loop: do {
              continue loop;
            } while (x == 0);
            // BUG: Diagnostic contains:
            """,
            """
            loop: do {
              continue loop;
            } while (true);
            """,
            """
            int i = 1;
            loop: do {
              i++;
              continue loop;
            } while (i == 0);
            // BUG: Diagnostic contains:
            """,
            """
            try {
              Files.readAllBytes(Paths.get("file"));
              return;
            } catch (IOException e) {
              return;
            }
            """,
            """
            try {
              try {
                Files.readAllBytes(Paths.get("file"));
                return;
              } catch (NullPointerException e) {
                return;
              }
            } catch (IOException e) {
              return;
            }
            """,
            """
            try {
              Files.readAllBytes(Paths.get("file"));
              return;
            } catch (IOException e) {
              return;
            } finally {
            }
            """,
            """
            try {
              //
            } catch (Throwable t) {
              return;
            } finally {
              return;
            }
            """,
            """
            try {
              return;
            } catch (Throwable t) {
              //
            } finally {
              return;
            }
            """,
            """
            int y = 0;
            while (true) {
              if (y++ > 10) {
                return;
              }
              if (y-- < 10) {
                return;
              }
            }
            """,
            """
            int y = 0;
            while (true) {
              do {
                switch (y) {
                  case 0:
                    continue; // continue target is do/while, not switch or outer while
                }
              } while (y > 0);
              break;
            }
            // BUG: Diagnostic contains:
            """,
            """
            try {
              if (Files.readAllBytes(Paths.get("file")) != null) {}
              return;
            } catch (IOException e) {
            }
            // BUG: Diagnostic contains:
            """,
            """
            try {
              Files.readAllBytes(Paths.get("file"));
            } catch (IOException e) {
              throw new IOError(e);
            }
            // BUG: Diagnostic contains:
            """,
            """
            try {
              Files.readAllBytes(Paths.get("file"));
              return;
            } catch (IOException e) {
              throw new IOError(e);
            }
            """,
            """
            throw new AssertionError();
            """,
            """
            for (;;) {}
            """,
            """
            System.exit(1);
            """,
            """
            {
              System.exit(1);
              throw new AssertionError();
            }
            """,
            """
            l:
            do {
              break l;
            } while (true);
            System.err.println();
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              case 1 -> {}
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              case 1 -> throw new AssertionError();
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              case 1 -> throw new AssertionError();
              case 2 -> throw new AssertionError();
              default -> {}
            }
            // BUG: Diagnostic contains:
            """,
            """
            switch (42) {
              case 1 -> throw new AssertionError();
              case 2 -> throw new AssertionError();
              default -> throw new AssertionError();
            }
            """,
            """
            switch (42) {
              case 1 -> { break; }
              case 2 -> throw new AssertionError();
              default -> throw new AssertionError();
            }
            // BUG: Diagnostic contains:
            """
          })
          String lines) {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            "import java.io.*;",
            "import java.nio.file.*;",
            "class Test {",
            "  void f(int x) {",
            "    switch (x) {",
            "      case 1:",
            lines.trim(),
            "      default:",
            "        break;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  /** A labeled block exited by a {@code break} can complete normally (JLS 21 §14.22). */
  @Test
  public void breakExitsLabeledStatement_block() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            l:
            {
              if (x == 1) {
                break l;
              }
              return;
            }
            """)
        .doTest();
  }

  /**
   * A labeled {@code if} statement exited by a {@code break} can complete normally (JLS 21 §14.22).
   */
  @Test
  public void breakExitsLabeledStatement_if() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            l:
            if (x == 1) {
              break l;
            } else {
              return;
            }
            """)
        .doTest();
  }

  /**
   * A labeled {@code try} statement exited by a {@code break} can complete normally (JLS 21
   * §14.22).
   */
  @Test
  public void breakExitsLabeledStatement_try() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            l:
            try {
              break l;
            } catch (RuntimeException e) {
              return;
            }
            """)
        .doTest();
  }

  /**
   * A labeled {@code synchronized} statement exited by a {@code break} can complete normally (JLS
   * 21 §14.22).
   */
  @Test
  public void breakExitsLabeledStatement_synchronized() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            l:
            synchronized (this) {
              break l;
            }
            """)
        .doTest();
  }

  /**
   * With two labels on a loop, a {@code break} of the outer label exits the outer labeled
   * statement, so that statement can complete normally (JLS 21 §14.22) even though the loop cannot.
   */
  @Test
  public void breakExitsLabeledStatement_nestedLabelsOnLoop() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            outer:
            inner:
            while (true) {
              break outer;
            }
            """)
        .doTest();
  }

  /**
   * With two labels on a {@code switch}, a {@code break} of the outer label exits the outer labeled
   * statement, so that statement can complete normally (JLS 21 §14.22) even though the {@code
   * switch} cannot.
   */
  @Test
  public void breakExitsLabeledStatement_nestedLabelsOnSwitch() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            outer:
            inner:
            switch (x) {
              default:
                break outer;
            }
            """)
        .doTest();
  }

  /**
   * An unlabeled {@code break} exits only the loop it is in, not the labeled block around it, which
   * here cannot complete normally.
   */
  @Test
  public void jumpDoesNotExitLabeledStatement_breakOutOfNestedLoop() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            l:
            {
              while (true) {
                if (x == 1) {
                  break;
                }
              }
              throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * {@code break inner} exits only the inner labeled block, not the outer one, which here cannot
   * complete normally.
   */
  @Test
  public void jumpDoesNotExitLabeledStatement_breakOutOfInnerLabel() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            outer:
            {
              inner:
              {
                if (x == 1) {
                  break inner;
                }
              }
              throw new AssertionError();
            }
            """)
        .doTest();
  }

  /** {@code continue l} does not exit the labeled loop, which here cannot complete normally. */
  @Test
  public void jumpDoesNotExitLabeledStatement_continueOfLabeledLoop() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            l:
            while (true) {
              if (x == 1) {
                continue l;
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} in a {@code try} block does not exit the loop if the {@code finally} block
   * cannot complete normally, because the {@code finally} block discards it (JLS 21 §14.20.2,
   * §14.22).
   */
  @Test
  public void jumpDiscardedByFinally_breakInTryBlock() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            while (true) {
              try {
                break;
              } finally {
                throw new AssertionError();
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} in a {@code catch} block does not exit the loop if the {@code finally} block
   * cannot complete normally, because the {@code finally} block discards it (JLS 21 §14.20.2).
   */
  @Test
  public void jumpDiscardedByFinally_breakInCatchBlock() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            while (true) {
              try {
                x++;
              } catch (RuntimeException e) {
                break;
              } finally {
                throw new AssertionError();
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code continue} in a {@code try} block does not continue the {@code do} statement if the
   * {@code finally} block cannot complete normally, because the {@code finally} block discards it
   * (JLS 21 §14.20.2, §14.22).
   */
  @Test
  public void jumpDiscardedByFinally_continueInTryBlock() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            do {
              try {
                continue;
              } finally {
                throw new AssertionError();
              }
            } while (x == 0);
            """)
        .doTest();
  }

  /**
   * Similar to {@link #continueInTryBlock}, but the {@code continue} is camouflaged as a larger try
   * statement.
   */
  @Test
  public void jumpDiscardedByFinally_camouflagedContinueInTryBlock() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            do {
              try {
                try {
                  break;
                } finally {
                  continue;
                }
              } finally {
                throw new AssertionError();
              }
            } while (x == 0);
            """)
        .doTest();
  }

  /**
   * A {@code break} in a {@code try} block does not exit the {@code switch} if the {@code finally}
   * block cannot complete normally, because the {@code finally} block discards it (JLS 21 §14.20.2,
   * §14.22).
   */
  @Test
  public void jumpDiscardedByFinally_breakOutOfSwitch() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (x) {
              default:
                try {
                  break;
                } finally {
                  throw new AssertionError();
                }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break l} in a {@code try} block does not exit the labeled statement if the {@code
   * finally} block cannot complete normally, because the {@code finally} block discards it (JLS 21
   * §14.20.2, §14.22).
   */
  @Test
  public void jumpDiscardedByFinally_breakOutOfLabeledStatement() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            l:
            try {
              break l;
            } finally {
              throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} does not exit the loop if the {@code finally} block of any enclosing {@code
   * try} statement within the loop cannot complete normally, even if an inner one can (JLS 21
   * §14.22).
   */
  @Test
  public void jumpDiscardedByFinally_outerFinally() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            while (true) {
              try {
                try {
                  break;
                } finally {
                  x++;
                }
              } finally {
                throw new AssertionError();
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} in a {@code try} block exits the loop if the {@code finally} block can complete
   * normally.
   */
  @Test
  public void jumpNotDiscardedByFinally_finallyCompletesNormally() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            while (true) {
              try {
                break;
              } finally {
                x++;
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} in the {@code finally} block itself exits the loop, even though it means that
   * the {@code finally} block cannot complete normally.
   */
  @Test
  public void jumpNotDiscardedByFinally_breakInFinallyBlock() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            while (true) {
              try {
                x++;
              } finally {
                break;
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code break} before the {@code try} statement still exits the loop, even though a {@code
   * break} to the same loop inside the {@code try} block is discarded.
   */
  @Test
  public void jumpNotDiscardedByFinally_breakBeforeTry() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            while (true) {
              if (x == 1) {
                break;
              }
              try {
                break;
              } finally {
                throw new AssertionError();
              }
            }
            """)
        .doTest();
  }

  /**
   * A {@code switch} over a sealed interface, with a pattern for each permitted subclass and no
   * {@code default}, is enhanced, so it cannot complete normally when every case throws.
   */
  @Test
  public void enhancedSwitchWithoutDefault_sealedPatterns() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (shape) {
              case Circle c -> throw new AssertionError();
              case Square s -> throw new AssertionError();
              case Triangle t -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /** Like {@link #enhancedSwitchWithoutDefault_sealedPatterns}, but with old-style colon switch. */
  @Test
  public void enhancedSwitchWithoutDefault_sealedPatternsInStatementGroups() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (shape) {
              case Circle c:
                throw new AssertionError();
              case Square s:
                throw new AssertionError();
              case Triangle t:
                throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * A {@code switch} over an enum with a {@code case null} is enhanced, so without a {@code
   * default} it cannot complete normally when every case throws (§14.22).
   */
  @Test
  public void enhancedSwitchWithoutDefault_nullLabel() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (color) {
              case RED -> throw new AssertionError();
              case GREEN, BLUE -> throw new AssertionError();
              case null -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * An unconditional pattern is similar to but not the same as a {@code default}: a {@code switch}
   * with patterns is enhanced, so it cannot complete normally when every case throws.
   */
  @Test
  public void enhancedSwitchWithoutDefault_unconditionalPattern() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (o) {
              case String s -> throw new AssertionError();
              case Integer i -> throw new AssertionError();
              case Object other -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * A {@code switch} over a sealed interface is enhanced (JLS 21 §14.11.2) even if its only labels
   * are constants of an enum that implements the interface, so it cannot complete normally when
   * every case throws (§14.22).
   */
  @Test
  public void enhancedSwitchWithoutDefault_enumConstantsOfSealedInterface() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            switch (paint) {
              case Color.RED -> throw new AssertionError();
              case Color.GREEN -> throw new AssertionError();
              case Color.BLUE -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * A {@code switch} over an enum with only constant labels is not enhanced (JLS 21 §14.11.2), so
   * without a {@code default} it can complete normally, even though it names every constant and
   * every case throws (§14.22).
   */
  @Test
  public void notEnhancedSwitchWithoutDefault_enum() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            switch (color) {
              case RED -> throw new AssertionError();
              case GREEN -> throw new AssertionError();
              case BLUE -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * A {@code switch} with only constant labels is not enhanced if the type of the selector
   * expression is in a designated set of types. So without a {@code default} it can complete
   * normally even though every case throws.
   */
  @Test
  public void notEnhancedSwitchWithoutDefault_selectorType(
      @TestParameter({
            "(char) x",
            "(byte) x",
            "(short) x",
            "x",
            "(Character) (char) x",
            "(Byte) (byte) x",
            "(Short) (short) x",
            "(Integer) x"
          })
          String selector) {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            switch (%s) {
              case 1 -> throw new AssertionError();
              case 2 -> throw new AssertionError();
              case 3 -> throw new AssertionError();
            }
            """
                .formatted(selector))
        .doTest();
  }

  /** Similar to {@link #notEnhancedSwitchWithoutDefault_selectorType}, but with {@code String}. */
  @Test
  public void notEnhancedSwitchWithoutDefault_string() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            switch (String.valueOf(x)) {
              case "1" -> throw new AssertionError();
              case "2" -> throw new AssertionError();
              case "3" -> throw new AssertionError();
            }
            """)
        .doTest();
  }

  /**
   * An enhanced {@code switch} without a {@code default} can still complete normally if a {@code
   * break} exits it.
   */
  @Test
  public void enhancedSwitchWithoutDefault_breakExitsSwitch() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            switch (shape) {
              case Circle c:
                break;
              case Square s:
                throw new AssertionError();
              case Triangle t:
                throw new AssertionError();
            }
            """)
        .doTest();
  }

  @Test
  public void breakOutOfEnclosingLoop_switchWithoutDefault() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                switch (x) {
                  case 1:
                    l:
                    while (true) {
                      switch (x) {
                        case 1:
                          break l;
                      }
                    }
                  // BUG: Diagnostic contains:
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void breakOutOfEnclosingLoop_arrowSwitchShortCircuits() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                switch (x) {
                  case 1:
                    l:
                    while (true) {
                      switch (x) {
                        case 1 -> {}
                        case 2 -> {}
                        default -> {
                          break l;
                        }
                      }
                    }
                  // BUG: Diagnostic contains:
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void breakOutOfEnclosingLoop_arrowSwitchBreakInFirstCase() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                switch (x) {
                  case 1:
                    l:
                    while (true) {
                      switch (x) {
                        default -> {
                          break l;
                        }
                        case 1 -> {}
                        case 2 -> {}
                      }
                    }
                  // BUG: Diagnostic contains:
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void breakOutOfEnclosingLoop_arrowSwitchWithoutDefault() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                switch (x) {
                  case 1:
                    l:
                    while (true) {
                      switch (x) {
                        case 1 -> {
                          break l;
                        }
                      }
                    }
                  // BUG: Diagnostic contains:
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void continueEnclosingDoLoop_switchWithoutDefault() {
    CompilationTestHelper.newInstance(FirstCaseFallsThrough.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                switch (x) {
                  case 1:
                    l:
                    do {
                      switch (x) {
                        case 1:
                          continue l;
                      }
                      return;
                    } while (false);
                  // BUG: Diagnostic contains:
                  default:
                    break;
                }
              }
            }
            """)
        .doTest();
  }

  /**
   * Reports a {@code do} loop that can complete normally, where each statement in its body labeled
   * {@code completes} or {@code doesNotComplete} is given as an analyzed patch with that result.
   */
  @BugPattern(summary = "", severity = ERROR)
  public static class DoWhileCompletesWithAnalyzedPatches extends BugChecker
      implements DoWhileLoopTreeMatcher {

    @Override
    public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
      ImmutableMap.Builder<Tree, CanCompleteNormallyPatch> analyzedPatches = ImmutableMap.builder();
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitLabeledStatement(LabeledStatementTree labeled, Void unused) {
          switch (labeled.getLabel().toString()) {
            case "completes" ->
                analyzedPatches.put(
                    labeled,
                    new CanCompleteNormallyPatch(
                        /* analyzeInside= */ true, /* canCompleteNormally= */ true));
            case "doesNotComplete" ->
                analyzedPatches.put(
                    labeled,
                    new CanCompleteNormallyPatch(
                        /* analyzeInside= */ true, /* canCompleteNormally= */ false));
            // No label? Handle as normal
            default -> {}
          }
          return super.visitLabeledStatement(labeled, null);
        }
      }.scan(tree.getStatement(), null);
      return Reachability.canCompleteNormally(tree, analyzedPatches.buildOrThrow())
          ? describeMatch(tree)
          : NO_MATCH;
    }
  }

  /**
   * A tree patched to be unable to complete normally is still analyzed, so a {@code continue}
   * inside it is recorded and makes the enclosing {@code do} able to complete normally. (Patching
   * the same tree with {@code patches} would not descend into it, so the {@code continue} would be
   * missed.)
   */
  @Test
  public void analyzedPatches_doesNotComplete_recordsJumpsInside() {
    CompilationTestHelper.newInstance(DoWhileCompletesWithAnalyzedPatches.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void continues(boolean b) {
                // BUG: Diagnostic contains:
                do {
                  doesNotComplete:
                  if (b) {
                    continue;
                  }
                } while (b);
              }

              void noJump(boolean b) {
                do {
                  doesNotComplete:
                  if (b) {
                    throw new AssertionError();
                  }
                } while (b);
              }
            }
            """)
        .doTest();
  }

  /**
   * A tree can also be patched to be able to complete normally, overriding the analysis. It is
   * still analyzed, so a {@code break} inside it is recorded too.
   */
  @Test
  public void analyzedPatches_completes() {
    CompilationTestHelper.newInstance(DoWhileCompletesWithAnalyzedPatches.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              // The `if` cannot complete normally, but is patched to, so the loop can too.
              void patched(boolean b) {
                // BUG: Diagnostic contains:
                do {
                  completes:
                  if (b) {
                    return;
                  } else {
                    throw new AssertionError();
                  }
                } while (b);
              }

              // Control: the same loop without the patch cannot complete normally.
              void unpatched(boolean b) {
                do {
                  if (b) {
                    return;
                  } else {
                    throw new AssertionError();
                  }
                } while (b);
              }

              // The inner loop cannot complete normally whatever its body does, so the outer loop
              // can complete normally only through the `break`, which is recorded only because the
              // patched tree is still analyzed.
              void breakInsidePatched(boolean b) {
                outer:
                // BUG: Diagnostic contains:
                do {
                  while (true) {
                    completes:
                    if (b) {
                      break outer;
                    } else {
                      throw new AssertionError();
                    }
                  }
                } while (b);
              }
            }
            """)
        .doTest();
  }
}
