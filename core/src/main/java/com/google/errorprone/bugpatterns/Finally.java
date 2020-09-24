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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BreakTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ContinueTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.util.Name;

/**
 * Matches the behaviour of javac's finally Xlint warning.
 *
 * <p>1) Any return statement in a finally block is an error 2) An uncaught throw statement in a
 * finally block is an error. We can't always know whether a specific exception will be caught, so
 * we report errors for throw statements that are not contained in a try with at least one catch
 * block. 3) A continue statement in a finally block is an error if it breaks out of a (possibly
 * labeled) loop that is outside the enclosing finally. 4) A break statement in a finally block is
 * an error if it breaks out of a (possibly labeled) loop or a switch statement that is outside the
 * enclosing finally.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    name = "Finally",
    altNames = {"finally", "ThrowFromFinallyBlock"},
    summary =
        "If you return or throw from a finally, then values returned or thrown from the"
            + " try-catch block will be ignored. Consider using try-with-resources instead.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class Finally extends BugChecker
    implements ContinueTreeMatcher, ThrowTreeMatcher, BreakTreeMatcher, ReturnTreeMatcher {

  @Override
  public Description matchContinue(ContinueTree tree, VisitorState state) {
    if (new FinallyJumpMatcher((JCContinue) tree).matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchBreak(BreakTree tree, VisitorState state) {
    if (new FinallyJumpMatcher((JCBreak) tree).matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchThrow(ThrowTree tree, VisitorState state) {
    if (new FinallyThrowMatcher().matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    if (new FinallyCompletionMatcher<ReturnTree>().matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private enum MatchResult {
    KEEP_LOOKING,
    NO_MATCH,
    FOUND_ERROR;
  }

  /**
   * Base class for all finally matchers. Walks up the tree of enclosing statements and reports an
   * error if it finds an enclosing finally block.
   *
   * @param <T> The type of the tree node to match against
   */
  private static class FinallyCompletionMatcher<T extends StatementTree> implements Matcher<T> {

    /**
     * Matches a StatementTree type by walking that statement's ancestor chain.
     *
     * @return true if an error is found.
     */
    @Override
    public boolean matches(T tree, VisitorState state) {
      Tree prevTree = null;
      for (Tree leaf : state.getPath()) {
        switch (leaf.getKind()) {
          case METHOD:
          case LAMBDA_EXPRESSION:
          case CLASS:
            return false;
          default:
            break;
        }
        MatchResult mr = matchAncestor(leaf, prevTree);
        if (mr != MatchResult.KEEP_LOOKING) {
          return mr == MatchResult.FOUND_ERROR;
        }
        prevTree = leaf;
      }

      return false;
    }

    /** Match a tree in the ancestor chain given the ancestor's immediate descendant. */
    protected MatchResult matchAncestor(Tree leaf, Tree prevTree) {
      if (leaf instanceof TryTree) {
        TryTree tryTree = (TryTree) leaf;
        if (tryTree.getFinallyBlock() != null && tryTree.getFinallyBlock().equals(prevTree)) {
          return MatchResult.FOUND_ERROR;
        }
      }

      return MatchResult.KEEP_LOOKING;
    }
  }

  /** Ancestor matcher for statements that break or continue out of a finally block. */
  private static class FinallyJumpMatcher extends FinallyCompletionMatcher<StatementTree> {
    private final Name label;
    private final JumpType jumpType;

    private enum JumpType {
      BREAK,
      CONTINUE
    }

    public FinallyJumpMatcher(JCContinue jcContinue) {
      this.label = jcContinue.getLabel();
      this.jumpType = JumpType.CONTINUE;
    }

    public FinallyJumpMatcher(JCBreak jcBreak) {
      this.label = jcBreak.getLabel();
      this.jumpType = JumpType.BREAK;
    }

    /**
     * The target of a jump statement (break or continue) is (1) the enclosing loop if the jump is
     * unlabeled (2) the enclosing LabeledStatementTree with matching label if the jump is labeled
     * (3) the enclosing switch statement if the jump is a break
     *
     * <p>If the target of a break or continue statement is encountered before reaching a finally
     * block, return NO_MATCH.
     */
    @Override
    protected MatchResult matchAncestor(Tree leaf, Tree prevTree) {

      // (1)
      if (label == null) {
        switch (leaf.getKind()) {
          case WHILE_LOOP:
          case DO_WHILE_LOOP:
          case FOR_LOOP:
          case ENHANCED_FOR_LOOP:
            return MatchResult.NO_MATCH;
          default:
            break;
        }
      }

      // (2)
      if (label != null
          && leaf instanceof LabeledStatementTree
          && label.equals(((LabeledStatementTree) leaf).getLabel())) {
        return MatchResult.NO_MATCH;
      }

      // (3)
      if (jumpType == JumpType.BREAK && leaf instanceof SwitchTree) {
        return MatchResult.NO_MATCH;
      }

      return super.matchAncestor(leaf, prevTree);
    }
  }

  /** Match throw statements that are not caught. */
  private static class FinallyThrowMatcher extends FinallyCompletionMatcher<ThrowTree> {
    @Override
    protected MatchResult matchAncestor(Tree tree, Tree prevTree) {
      if (tree instanceof TryTree) {
        TryTree tryTree = (TryTree) tree;
        if (tryTree.getBlock().equals(prevTree) && !tryTree.getCatches().isEmpty()) {
          // The current ancestor is a try block with associated catch blocks.
          return MatchResult.NO_MATCH;
        }
      }

      return super.matchAncestor(tree, prevTree);
    }
  }
}
