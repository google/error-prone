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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.Reachability.CanCompleteNormallyPatch.CANNOT_COMPLETE_NORMALLY_ANALYZE_INSIDE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LabeledStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability.CanCompleteNormallyPatch;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import org.junit.Test;
import org.junit.runner.RunWith;

/// [Reachability]Test.
@RunWith(TestParameterInjector.class)
public class ReachabilityTest {

  /// Reports an error if the first case in a switch falls through to the second.
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

  /// Reports whether [Reachability] thinks that the statement contained in each labeled statement
  /// in a method named `f` can complete normally.
  @BugPattern(summary = "", severity = ERROR)
  public static class ContainedStatementCanCompleteNormally extends BugChecker
      implements LabeledStatementTreeMatcher {

    @Override
    public Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state) {
      MethodTree method = state.findEnclosing(MethodTree.class);
      if (method == null || !method.getName().contentEquals("f")) {
        return NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage("Reachability: " + Reachability.canCompleteNormally(tree.getStatement()))
          .build();
    }
  }

  /// Returns a test that [Reachability] and javac agree on whether `statement` can complete
  /// normally. `expected` is the ground truth of whether the `statement` can complete normally.
  ///
  /// The `statement` may use (but doesn't need to use) the parameters `x`, `o`, `shape`, `paint`
  /// and `color`, of the types declared in the generated source.
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

  /// Returns a test that [Reachability] and javac agree on whether the statement contained in
  /// `labeledStatement` (call it `S`) can complete normally. `expected` is the ground truth of
  /// whether `S` can complete normally per the JLS.
  ///
  /// A unit test can't easily access the answer for `S`, because any code after `S` is also after
  /// `labeledStatement`. However, this can be addressed with a couple tricks, depending on the
  /// ground truth value:
  ///
  /// Ground truth: the statement `S` can complete normally 1. We put `S` in `if (!(o instanceof
  /// String s)) S`, inside a block labeled `l`. 2. The pattern variable `s` is in scope after the
  /// `if` statement iff `S` cannot complete normally. 3. declare a new local variable `s`, which
  /// would clash with the pattern variable if it were in scope.
  ///
  /// Ground truth: the statement `S` cannot complete normally 1.-2. as above, 3. code after the
  /// `if` references the pattern variable `s`, which would otherwise not be in scope.
  private CompilationTestHelper containedStatementCanCompleteNormallyTest(
      boolean expected, String labeledStatement) {
    String label = "l:\n";
    checkArgument(labeledStatement.startsWith(label), labeledStatement);
    String statement = labeledStatement.substring(label.length());
    return CompilationTestHelper.newInstance(
            ContainedStatementCanCompleteNormally.class, getClass())
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void f(int x) {
                // BUG: Diagnostic contains: Reachability: %s
            %s
              }

              void javacAgrees(int x, Object o) {
                l:
                {
                  if (!(o instanceof String s))
            %s
                  %s
                }
              }
            }
            """
                .formatted(
                    expected,
                    labeledStatement,
                    statement,
                    expected ? "String s = \"\";" : "s.length();"));
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

  /// A labeled block exited by a `break` can complete normally (JLS 21 §14.22).
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

  /// A labeled `if` statement exited by a `break` can complete normally (JLS 21 §14.22).
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

  /// A labeled `try` statement exited by a `break` can complete normally (JLS 21 §14.22).
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

  /// A labeled `synchronized` statement exited by a `break` can complete normally (JLS 21 §14.22).
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

  /// With two labels on a loop, a `break` of the outer label exits the outer labeled statement, so
  /// that statement can complete normally (JLS 21 §14.22) even though the loop cannot.
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

  /// With two labels on a `switch`, a `break` of the outer label exits the outer labeled statement,
  /// so that statement can complete normally (JLS 21 §14.22) even though the `switch` cannot.
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

  /// A `break l` exits the labeled statement, not the `while` statement it contains (JLS 21
  /// §14.15), so only the labeled statement can complete normally (JLS 21 §14.22).
  @Test
  public void breakExitsLabeledStatement_while() {
    String statement =
        """
        l:
        while (true) {
          break l;
        }
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ false, statement).doTest();
  }

  /// A `break l` exits the labeled statement, not the `do` statement it contains (JLS 21 §14.15),
  /// so only the labeled statement can complete normally (JLS 21 §14.22).
  @Test
  public void breakExitsLabeledStatement_do() {
    String statement =
        """
        l:
        do {
          break l;
        } while (true);
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ false, statement).doTest();
  }

  /// A `break l` exits the labeled statement, not the `for` statement it contains (JLS 21 §14.15),
  /// so only the labeled statement can complete normally (JLS 21 §14.22).
  @Test
  public void breakExitsLabeledStatement_for() {
    String statement =
        """
        l:
        for (;;) {
          break l;
        }
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ false, statement).doTest();
  }

  /// A `break l` exits the labeled statement, not the `switch` statement it contains (JLS 21
  /// §14.15), so only the labeled statement can complete normally (JLS 21 §14.22).
  @Test
  public void breakExitsLabeledStatement_switch() {
    String statement =
        """
        l:
        switch (x) {
          case 1:
            break l;
          default:
            throw new AssertionError();
        }
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ false, statement).doTest();
  }

  /// Like [#breakExitsLabeledStatement_switch], but with switch rules.
  @Test
  public void breakExitsLabeledStatement_arrowSwitch() {
    String statement =
        """
        l:
        switch (x) {
          case 1 -> {
            break l;
          }
          case 2 -> throw new AssertionError();
          default -> throw new AssertionError();
        }
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ false, statement).doTest();
  }

  /// A `break` with no label exits the innermost `switch`, `while`, `do` or `for` statement around
  /// it (JLS 21 §14.15). So here the `while` statement can complete normally (JLS 21 §14.22), even
  /// though it has a label.
  @Test
  public void breakExitsContainedStatement_withoutLabel() {
    String statement =
        """
        l:
        while (true) {
          break;
        }
        """;
    canCompleteNormallyTest(/* expected= */ true, statement).doTest();
    containedStatementCanCompleteNormallyTest(/* expected= */ true, statement).doTest();
  }

  /// An unlabeled `break` exits only the loop it is in, not the labeled block around it, which here
  /// cannot complete normally.
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

  /// `break inner` exits only the inner labeled block, not the outer one, which here cannot
  /// complete normally.
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

  /// `continue l` does not exit the labeled loop, which here cannot complete normally.
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

  /// With two labels on a `do` statement, `continue inner` continues the `do` statement (JLS 21
  /// §14.16), so the `do` statement can complete normally (JLS 21 §14.22). (`continue outer` is a
  /// compile-time error, because the statement that `outer` labels is a labeled statement, not a
  /// loop.)
  @Test
  public void continueContinuesLabeledLoop_nestedLabelsOnDo() {
    canCompleteNormallyTest(
            /* expected= */ true,
            """
            outer:
            inner:
            do {
              continue inner;
            } while (x == 1);
            """)
        .doTest();
  }

  /// Like [#continueContinuesLabeledLoop_nestedLabelsOnDo], but the condition is the constant
  /// `true`, so the `do` statement can't complete normally (JLS 21 §14.22).
  @Test
  public void continueContinuesLabeledLoop_nestedLabelsOnDo_constantTrue() {
    canCompleteNormallyTest(
            /* expected= */ false,
            """
            outer:
            inner:
            do {
              continue inner;
            } while (true);
            """)
        .doTest();
  }

  /// A `break` in a `try` block does not exit the loop if the `finally` block cannot complete
  /// normally, because the `finally` block discards it (JLS 21 §14.20.2, §14.22).
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

  /// A `break` in a `catch` block does not exit the loop if the `finally` block cannot complete
  /// normally, because the `finally` block discards it (JLS 21 §14.20.2).
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

  /// A `continue` in a `try` block does not continue the `do` statement if the `finally` block
  /// cannot complete normally, because the `finally` block discards it (JLS 21 §14.20.2, §14.22).
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

  /// Similar to [#jumpDiscardedByFinally_continueInTryBlock], but the `continue` is camouflaged as
  /// a larger try statement.
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

  /// A `break` in a `try` block does not exit the `switch` if the `finally` block cannot complete
  /// normally, because the `finally` block discards it (JLS 21 §14.20.2, §14.22).
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

  /// A `break l` in a `try` block does not exit the labeled statement if the `finally` block cannot
  /// complete normally, because the `finally` block discards it (JLS 21 §14.20.2, §14.22).
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

  /// A `break` does not exit the loop if the `finally` block of any enclosing `try` statement
  /// within the loop cannot complete normally, even if an inner one can (JLS 21 §14.22).
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

  /// A `break` in a `try` block exits the loop if the `finally` block can complete normally.
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

  /// A `break` in the `finally` block itself exits the loop, even though it means that the
  /// `finally` block cannot complete normally.
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

  /// A `break` before the `try` statement still exits the loop, even though a `break` to the same
  /// loop inside the `try` block is discarded.
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

  /// A `switch` over a sealed interface, with a pattern for each permitted subclass and no
  /// `default`, is enhanced, so it cannot complete normally when every case throws.
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

  /// Like [#enhancedSwitchWithoutDefault_sealedPatterns], but with old-style colon switch.
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

  /// A `switch` over an enum with a `case null` is enhanced, so without a `default` it cannot
  /// complete normally when every case throws (§14.22).
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

  /// An unconditional pattern is similar to but not the same as a `default`: a `switch` with
  /// patterns is enhanced, so it cannot complete normally when every case throws.
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

  /// A `switch` over a sealed interface is enhanced (JLS 21 §14.11.2) even if its only labels are
  /// constants of an enum that implements the interface, so it cannot complete normally when every
  /// case throws (§14.22).
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

  /// A `switch` over an enum with only constant labels is not enhanced (JLS 21 §14.11.2), so
  /// without a `default` it can complete normally, even though it names every constant and every
  /// case throws (§14.22).
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

  /// A `switch` with only constant labels is not enhanced if the type of the selector expression is
  /// in a designated set of types. So without a `default` it can complete normally even though
  /// every case throws.
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

  /// Similar to [#notEnhancedSwitchWithoutDefault_selectorType], but with `String`.
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

  /// An enhanced `switch` without a `default` can still complete normally if a `break` exits it.
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

  /// Reports a `do` loop that can complete normally, where each statement in its body labeled
  /// `doesNotComplete` is patched with `CANNOT_COMPLETE_NORMALLY_ANALYZE_INSIDE`.
  @BugPattern(summary = "", severity = ERROR)
  public static class DoWhileCompletesWithAnalyzedPatches extends BugChecker
      implements DoWhileLoopTreeMatcher {

    @Override
    public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
      ImmutableMap.Builder<Tree, CanCompleteNormallyPatch> analyzedPatches = ImmutableMap.builder();
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitLabeledStatement(LabeledStatementTree labeled, Void unused) {
          if (labeled.getLabel().contentEquals("doesNotComplete")) {
            analyzedPatches.put(labeled, CANNOT_COMPLETE_NORMALLY_ANALYZE_INSIDE);
          }
          return super.visitLabeledStatement(labeled, null);
        }
      }.scan(tree.getStatement(), null);
      return Reachability.canCompleteNormally(tree, analyzedPatches.buildOrThrow())
          ? describeMatch(tree)
          : NO_MATCH;
    }
  }

  /// A tree patched to be unable to complete normally is still analyzed, so a `continue` inside it
  /// is recorded and makes the enclosing `do` able to complete normally. (Patching the same tree
  /// with `CANNOT_COMPLETE_NORMALLY_SKIP_INSIDE` would not descend into it, so the `continue` would
  /// be missed.)
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
}
