/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.assignment;
import static com.google.errorprone.matchers.Matchers.binaryTree;
import static com.google.errorprone.matchers.Matchers.inSynchronized;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.sameVariable;
import static com.google.errorprone.matchers.Matchers.toType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import javax.lang.model.element.Modifier;

/** Detects non-atomic updates to volatile variables. */
@BugPattern(
  name = "NonAtomicVolatileUpdate",
  summary = "This update of a volatile variable is non-atomic",
  explanation =
      "The volatile modifier ensures that updates to a variable are propagated "
          + "predictably to other threads.  A read of a volatile variable always returns the most "
          + "recent write by any thread.\n\n"
          + "However, this does not mean that all updates to a volatile variable are atomic.  For "
          + "example, if you increment or decrement a volatile variable, you are actually doing "
          + "(1) a read of the variable, (2) an increment or decrement of a local copy, and (3) a "
          + "write back to the variable. Each step is atomic individually, but the whole sequence "
          + "is not, and it will cause a race condition if two threads try to increment or "
          + "decrement a volatile variable at the same time.  The same is true for compound "
          + "assignment, e.g. foo += bar.\n\n"
          + "If you intended for this update to be atomic, you should wrap all update operations "
          + "on this variable in a synchronized block.  If the variable is an integer, you could "
          + "use an AtomicInteger instead of a volatile int.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class NonAtomicVolatileUpdate extends BugChecker
    implements UnaryTreeMatcher, CompoundAssignmentTreeMatcher, AssignmentTreeMatcher {

  /** Extracts the expression from a UnaryTree and applies a matcher to it. */
  private static Matcher<UnaryTree> expressionFromUnaryTree(
      final Matcher<ExpressionTree> exprMatcher) {
    return new Matcher<UnaryTree>() {
      @Override
      public boolean matches(UnaryTree tree, VisitorState state) {
        return exprMatcher.matches(tree.getExpression(), state);
      }
    };
  }

  /** Extracts the variable from a CompoundAssignmentTree and applies a matcher to it. */
  private static Matcher<CompoundAssignmentTree> variableFromCompoundAssignmentTree(
      final Matcher<ExpressionTree> exprMatcher) {
    return new Matcher<CompoundAssignmentTree>() {
      @Override
      public boolean matches(CompoundAssignmentTree tree, VisitorState state) {
        return exprMatcher.matches(tree.getVariable(), state);
      }
    };
  }

  /** Extracts the variable from an AssignmentTree and applies a matcher to it. */
  private static Matcher<AssignmentTree> variableFromAssignmentTree(
      final Matcher<ExpressionTree> exprMatcher) {
    return new Matcher<AssignmentTree>() {
      @Override
      public boolean matches(AssignmentTree tree, VisitorState state) {
        return exprMatcher.matches(tree.getVariable(), state);
      }
    };
  }

  /**
   * Matches patterns like i++ and i-- in which i is volatile, and the pattern is not enclosed by a
   * synchronized block.
   */
  private static final Matcher<UnaryTree> unaryIncrementDecrementMatcher =
      allOf(
          expressionFromUnaryTree(Matchers.<ExpressionTree>hasModifier(Modifier.VOLATILE)),
          not(inSynchronized()),
          anyOf(
              kindIs(Kind.POSTFIX_INCREMENT),
              kindIs(Kind.PREFIX_INCREMENT),
              kindIs(Kind.POSTFIX_DECREMENT),
              kindIs(Kind.PREFIX_DECREMENT)));

  @Override
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    if (unaryIncrementDecrementMatcher.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  /**
   * Matches patterns like i += 1 and i -= 1 in which i is volatile, and the pattern is not enclosed
   * by a synchronized block.
   */
  private static final Matcher<CompoundAssignmentTree> compoundAssignmentIncrementDecrementMatcher =
      allOf(
          variableFromCompoundAssignmentTree(
              Matchers.<ExpressionTree>hasModifier(Modifier.VOLATILE)),
          not(inSynchronized()),
          anyOf(kindIs(Kind.PLUS_ASSIGNMENT), kindIs(Kind.MINUS_ASSIGNMENT)));

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    if (compoundAssignmentIncrementDecrementMatcher.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  /**
   * Matches patterns like i = i + 1 and i = i - 1 in which i is volatile, and the pattern is not
   * enclosed by a synchronized block.
   */
  private static Matcher<AssignmentTree> assignmentIncrementDecrementMatcher(
      ExpressionTree variable) {
    return allOf(
        variableFromAssignmentTree(Matchers.<ExpressionTree>hasModifier(Modifier.VOLATILE)),
        not(inSynchronized()),
        assignment(
            Matchers.<ExpressionTree>anything(),
            toType(
                BinaryTree.class,
                Matchers.<BinaryTree>allOf(
                    Matchers.<BinaryTree>anyOf(kindIs(Kind.PLUS), kindIs(Kind.MINUS)),
                    binaryTree(sameVariable(variable), Matchers.<ExpressionTree>anything())))));
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    if (assignmentIncrementDecrementMatcher(tree.getVariable()).matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
