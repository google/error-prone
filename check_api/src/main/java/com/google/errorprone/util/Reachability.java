/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.Objects.requireNonNull;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** An implementation of JLS 14.21 reachability. */
public class Reachability {

  /**
   * Returns true if the given statement can complete normally, as defined by JLS 14.21.
   *
   * <p>An exception is made for {@code System.exit}, which cannot complete normally in practice.
   */
  public static boolean canCompleteNormally(StatementTree statement) {
    return statement.accept(new CanCompleteNormallyVisitor(), null);
  }

  /**
   * Returns true if the given case tree can complete normally, as defined by JLS 14.21.
   *
   * <p>An exception is made for {@code System.exit}, which cannot complete normally in practice.
   */
  public static boolean canCompleteNormally(CaseTree caseTree) {
    List<? extends StatementTree> statements = caseTree.getStatements();
    if (statements.isEmpty()) {
      return true;
    }
    // We only care whether the last statement completes; javac would have already
    // reported an error if that statement wasn't reachable, and the answer is
    // independent of any preceding statements.
    // TODO(cushon): This isn't really making an exception for System.exit in the prior statements.
    return canCompleteNormally(getLast(statements));
  }

  private static class CanCompleteNormallyVisitor extends SimpleTreeVisitor<Boolean, Void> {

    /** Trees that are the target of a reachable break statement. */
    private final Set<Tree> breaks = new HashSet<>();

    /** Trees that are the target of a reachable continue statement. */
    private final Set<Tree> continues = new HashSet<>();

    boolean scan(List<? extends StatementTree> trees) {
      boolean completes = true;
      for (StatementTree tree : trees) {
        completes = scan(tree);
      }
      return completes;
    }

    private boolean scan(Tree tree) {
      return tree.accept(this, null);
    }

    /* A break statement cannot complete normally. */
    @Override
    public Boolean visitBreak(BreakTree tree, Void unused) {
      breaks.add(skipLabel(requireNonNull(((JCTree.JCBreak) tree).target)));
      return false;
    }

    /* A continue statement cannot complete normally. */
    @Override
    public Boolean visitContinue(ContinueTree tree, Void unused) {
      continues.add(skipLabel(requireNonNull(((JCTree.JCContinue) tree).target)));
      return false;
    }

    private Tree skipLabel(JCTree tree) {
      return tree.hasTag(JCTree.Tag.LABELLED) ? ((JCTree.JCLabeledStatement) tree).body : tree;
    }

    @Override
    public Boolean visitBlock(BlockTree tree, Void unused) {
      return scan(tree.getStatements());
    }

    /* A local class declaration statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitClass(ClassTree tree, Void unused) {
      return true;
    }

    /* A local variable declaration statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitVariable(VariableTree tree, Void unused) {
      return true;
    }

    /* An empty statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitEmptyStatement(EmptyStatementTree tree, Void unused) {
      return true;
    }

    @Override
    public Boolean visitLabeledStatement(LabeledStatementTree tree, Void unused) {
      // break/continue targets have already been resolved by javac, so
      // there's nothing to do here
      return scan(tree.getStatement());
    }

    /* An expression statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitExpressionStatement(ExpressionStatementTree tree, Void unused) {
      if (isSystemExit(tree.getExpression())) {
        // The spec doesn't have any special handling for {@code System.exit}, but in practice it
        // cannot complete normally
        return false;
      }
      return true;
    }

    private boolean isSystemExit(ExpressionTree expression) {
      if (!(expression instanceof MethodInvocationTree)) {
        return false;
      }
      MethodSymbol sym = getSymbol((MethodInvocationTree) expression);
      if (sym == null) {
        return false;
      }
      return sym.owner.getQualifiedName().contentEquals("java.lang.System")
          && sym.getSimpleName().contentEquals("exit");
    }

    /*
     * An if-then statement can complete normally iff it is reachable.
     *
     * The then-statement is reachable iff the if-then statement is reachable.
     * An if-then-else statement can complete normally iff the then-statement
     * can complete normally or the else-statement can complete normally.
     *
     * The then-statement is reachable iff the if-then-else statement is
     * reachable.
     *
     * The else-statement is reachable iff the if-then-else statement is
     * reachable.
     */
    @Override
    public Boolean visitIf(IfTree tree, Void unused) {
      boolean thenCompletes = scan(tree.getThenStatement());
      boolean elseCompletes = tree.getElseStatement() == null || scan(tree.getElseStatement());
      return thenCompletes || elseCompletes;
    }

    /* An assert statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitAssert(AssertTree tree, Void unused) {
      return true;
    }

    /*
     * A switch statement can complete normally iff at least one of the
     * following is true:
     *
     *  1) The switch block is empty or contains only switch labels.
     *  2) The last statement in the switch block can complete normally.
     *  3) There is at least one switch label after the last switch block
     *     statement group.
     *  4) The switch block does not contain a default label.
     *  5) There is a reachable break statement that exits the switch statement.
     *
     * A switch block is reachable iff its switch statement is reachable.
     *
     * A statement in a switch block is reachable iff its switch statement is
     * reachable and at least one of the following is true:
     *
     *  - It bears a case or default label.
     *
     *  - There is a statement preceding it in the switch block and that
     *    preceding statement can complete normally.
     */
    @Override
    public Boolean visitSwitch(SwitchTree tree, Void unused) {
      // (1)
      if (tree.getCases().stream().allMatch(c -> c.getStatements().isEmpty())) {
        return true;
      }
      // (2)
      boolean lastCompletes = true;
      for (CaseTree c : tree.getCases()) {
        lastCompletes = scan(c.getStatements());
      }
      if (lastCompletes) {
        return true;
      }
      // (3)
      if (getLast(tree.getCases()).getStatements().isEmpty()) {
        return true;
      }
      // (4)
      if (tree.getCases().stream().noneMatch(c -> c.getExpression() == null)) {
        return true;
      }
      // (5)
      if (breaks.contains(tree)) {
        return true;
      }
      return false;
    }

    /*
     * A while statement can complete normally iff at least one of the
     * following is true:
     *
     *  1) The while statement is reachable and the condition expression is not
     *     a constant expression (§15.28) with value true.
     *  2) There is a reachable break statement that exits the while statement.
     *
     *  The contained statement is reachable iff the while statement is
     *  reachable and the condition expression is not a constant expression
     *  whose value is false.
     */
    @Override
    public Boolean visitWhileLoop(WhileLoopTree tree, Void unused) {
      Boolean condValue = ASTHelpers.constValue(tree.getCondition(), Boolean.class);
      if (!Objects.equals(condValue, false)) {
        scan(tree.getStatement());
      }
      // (1)
      if (!Objects.equals(condValue, true)) {
        return true;
      }
      // (2)
      if (breaks.contains(tree)) {
        return true;
      }
      return false;
    }

    /*
     * A do statement can complete normally iff at least one of the following
     * is true:
     *
     *  1) The contained statement can complete normally and the condition
     *     expression is not a constant expression (§15.28) with value true.
     *
     *  2) The do statement contains a reachable continue statement with no
     *     label, and the do statement is the innermost while, do, or for
     *     statement that contains that continue statement, and the continue
     *     statement continues that do statement, and the condition expression
     *     is not a constant expression with value true.
     *
     *  3) The do statement contains a reachable continue statement with a
     *     label L, and the do statement has label L, and the continue
     *     statement continues that do statement, and the condition expression
     *     is not a constant expression with value true.
     *
     *  4) There is a reachable break statement that exits the do statement.
     *
     * The contained statement is reachable iff the do statement is reachable.
     */
    @Override
    public Boolean visitDoWhileLoop(DoWhileLoopTree that, Void unused) {
      boolean completes = scan(that.getStatement());
      boolean conditionIsAlwaysTrue =
          firstNonNull(ASTHelpers.constValue(that.getCondition(), Boolean.class), false);
      // (1)
      if (completes && !conditionIsAlwaysTrue) {
        return true;
      }
      // (2) or (3)
      if (continues.contains(that) && !conditionIsAlwaysTrue) {
        return true;
      }
      // (4)
      if (breaks.contains(that)) {
        return true;
      }
      return false;
    }

    /*
     * A basic for statement can complete normally iff at least one of the
     * following is true:
     *
     * 1) The for statement is reachable, there is a condition expression, and
     *    the condition expression is not a constant expression (§15.28) with
     *    value true.
     *
     * 2) There is a reachable break statement that exits the for statement.
     *
     * The contained statement is reachable iff the for statement is reachable
     * and the condition expression is not a constant expression whose value is
     * false.
     */
    @Override
    public Boolean visitForLoop(ForLoopTree that, Void unused) {
      Boolean condValue = ASTHelpers.constValue(that.getCondition(), Boolean.class);
      if (!Objects.equals(condValue, false)) {
        scan(that.getStatement());
      }
      // (1)
      if (that.getCondition() != null && !Objects.equals(condValue, true)) {
        return true;
      }
      // (2)
      if (breaks.contains(that)) {
        return true;
      }
      return false;
    }

    /* An enhanced for statement can complete normally iff it is reachable. */
    @Override
    public Boolean visitEnhancedForLoop(EnhancedForLoopTree that, Void unused) {
      scan(that.getStatement());
      return true;
    }

    /* A return statement cannot complete normally. */
    @Override
    public Boolean visitReturn(ReturnTree tree, Void unused) {
      return false;
    }

    /* A throw statement cannot complete normally. */
    @Override
    public Boolean visitThrow(ThrowTree tree, Void unused) {
      return false;
    }

    /*
     * A synchronized statement can complete normally iff the contained
     * statement can complete normally.
     *
     * The contained statement is reachable iff the synchronized statement
     * is reachable.
     */
    @Override
    public Boolean visitSynchronized(SynchronizedTree tree, Void unused) {
      return scan(tree.getBlock());
    }

    /*
     * A try statement can complete normally iff both of the following are true:
     *
     *  1) The try block can complete normally or any catch block can complete
     *     normally.
     *
     *  2) If the try statement has a finally block, then the finally block can
     *     complete normally.
     */
    @Override
    public Boolean visitTry(TryTree that, Void unused) {
      boolean completes = scan(that.getBlock());
      // assume all catch blocks are reachable; javac has already rejected unreachable
      // checked exception handlers
      for (CatchTree catchTree : that.getCatches()) {
        completes |= scan(catchTree.getBlock());
      }
      if (that.getFinallyBlock() != null && !scan(that.getFinallyBlock())) {
        completes = false;
      }
      return completes;
    }
  }
}
