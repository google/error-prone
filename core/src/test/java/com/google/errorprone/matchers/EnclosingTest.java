/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.matchers.Matchers.enclosingBlock;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.parentNode;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Matchers#enclosingNode}, {@link Matchers#parentNode}, {@link
 * Matchers#enclosingBlock}, {@link Enclosing.BlockOrCase}, and at least some of the code paths also
 * used by {@link Matchers#enclosingClass} and {@link Matchers#enclosingMethod}. The tests focus on
 * verifying that the {@link TreePath} is set correctly.
 *
 * @author cpovirk@google.com (Chris Povirk)
 */
@RunWith(JUnit4.class)
public class EnclosingTest extends CompilerBasedAbstractTest {
  private abstract static class IsInterestingLoopSubNode<T extends Tree> implements Matcher<T> {
    @Override
    public boolean matches(T t, VisitorState state) {
      if (state.getPath().getParentPath() == null) {
        return false;
      }
      Tree parent = state.getPath().getParentPath().getLeaf();
      return (parent instanceof ForLoopTree && (interestingPartOfLoop((ForLoopTree) parent) == t));
    }

    abstract Object interestingPartOfLoop(ForLoopTree loop);
  }

  private static final Matcher<Tree> IS_LOOP_CONDITION =
      new IsInterestingLoopSubNode<Tree>() {
        @Override
        Object interestingPartOfLoop(ForLoopTree loop) {
          return loop.getCondition();
        }
      };
  private static final Matcher<BlockTree> IS_LOOP_STATEMENT =
      new IsInterestingLoopSubNode<BlockTree>() {
        @Override
        Object interestingPartOfLoop(ForLoopTree loop) {
          return loop.getStatement();
        }
      };
  private static final Matcher<Tree> ENCLOSED_IN_LOOP_CONDITION = enclosingNode(IS_LOOP_CONDITION);
  private static final Matcher<Tree> CHILD_OF_LOOP_CONDITION = parentNode(IS_LOOP_CONDITION);
  private static final Matcher<Tree> USED_UNDER_LOOP_STATEMENT = enclosingBlock(IS_LOOP_STATEMENT);
  private static final Matcher<Tree> USED_UNDER_LOOP_STATEMENT_ACCORDING_TO_BLOCK_OR_CASE =
      new Enclosing.BlockOrCase<>(IS_LOOP_STATEMENT, Matchers.<CaseTree>nothing());

  final List<ScannerTest> tests = new ArrayList<>();

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  /** Tests that a node is not enclosed by itself. */
  @Test
  public void usedDirectlyInLoopCondition() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    boolean foo = true;",
        "    for (int i = 0; foo; i++) {}",
        "  }",
        "}");
    assertCompiles(fooIsUsedUnderLoopCondition(false));
    assertCompiles(fooIsChildOfLoopCondition(false));
    assertCompiles(fooIsUsedUnderLoopStatement(false));
    assertCompiles(fooIsUsedUnderLoopStatementAccordingToBlockOrCase(false));
  }

  /** Tests that a node is enclosed by its parent. */
  @Test
  public void usedAsChildTreeOfLoopCondition() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    boolean foo = true;",
        "    for (int i = 0; !foo; i++) {}",
        "  }",
        "}");
    assertCompiles(fooIsUsedUnderLoopCondition(true));
    assertCompiles(fooIsChildOfLoopCondition(true));
    assertCompiles(fooIsUsedUnderLoopStatement(false));
    assertCompiles(fooIsUsedUnderLoopStatementAccordingToBlockOrCase(false));
  }

  /** Tests that a node is enclosed by a node many levels up the tree. */
  @Test
  public void usedInSubTreeOfLoopCondition() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    boolean foo = true;",
        "    for (int i = 0; !!!!!!!!!foo; i++) {}",
        "  }",
        "}");
    assertCompiles(fooIsUsedUnderLoopCondition(true));
    assertCompiles(fooIsChildOfLoopCondition(false));
    assertCompiles(fooIsUsedUnderLoopStatement(false));
    assertCompiles(fooIsUsedUnderLoopStatementAccordingToBlockOrCase(false));
  }

  /** Tests enclosing blocks. */
  @Test
  public void usedInStatement() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    boolean foo = true;",
        "    for (int i = 0; i < 100; i++) {",
        "      foo = !foo;",
        "    }",
        "  }",
        "}");
    assertCompiles(fooIsUsedUnderLoopCondition(false));
    assertCompiles(fooIsChildOfLoopCondition(false));
    assertCompiles(fooIsUsedUnderLoopStatement(true));
    assertCompiles(fooIsUsedUnderLoopStatementAccordingToBlockOrCase(true));
  }

  /** Make sure the scanners are doing what we expect. */
  @Test
  public void usedElsewhereInLoop() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    boolean foo = true;",
        "    for (int i = foo ? 0 : 1; i < 100; foo = !foo) {}",
        "  }",
        "}");
    assertCompiles(fooIsUsedUnderLoopCondition(false));
    assertCompiles(fooIsChildOfLoopCondition(false));
    assertCompiles(fooIsUsedUnderLoopStatement(false));
    assertCompiles(fooIsUsedUnderLoopStatementAccordingToBlockOrCase(false));
  }

  private abstract class ScannerTest extends Scanner {
    abstract void assertDone();
  }

  private Scanner fooIsUsedUnderLoopCondition(boolean shouldMatch) {
    return fooMatches(shouldMatch, ENCLOSED_IN_LOOP_CONDITION);
  }

  private Scanner fooIsChildOfLoopCondition(boolean shouldMatch) {
    return fooMatches(shouldMatch, CHILD_OF_LOOP_CONDITION);
  }

  private Scanner fooIsUsedUnderLoopStatement(boolean shouldMatch) {
    return fooMatches(shouldMatch, USED_UNDER_LOOP_STATEMENT);
  }

  private Scanner fooIsUsedUnderLoopStatementAccordingToBlockOrCase(boolean shouldMatch) {
    return fooMatches(shouldMatch, USED_UNDER_LOOP_STATEMENT_ACCORDING_TO_BLOCK_OR_CASE);
  }

  private Scanner fooMatches(final boolean shouldMatch, final Matcher<Tree> matcher) {
    ScannerTest test =
        new ScannerTest() {
          boolean matched = false;

          @Override
          public Void visitIdentifier(IdentifierTree tree, VisitorState state) {
            // Normally handled by ErrorProneMatcher:
            // TODO(cpovirk): Find a way for this to be available by default to Matcher tests.
            state = state.withPath(getCurrentPath());

            matched |= tree.getName().contentEquals("foo") && matcher.matches(tree, state);
            return null;
          }

          @Override
          void assertDone() {
            assertThat(matched).isEqualTo(shouldMatch);
          }
        };
    tests.add(test);
    return test;
  }
}
