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

package com.google.errorprone.bugpatterns;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "LoopConditionChecker",
  category = JDK,
  summary = "Loop condition is never modified in loop body.",
  severity = ERROR
)
public class LoopConditionChecker extends BugChecker
    implements ForLoopTreeMatcher, DoWhileLoopTreeMatcher, WhileLoopTreeMatcher {

  @Override
  public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
    return check(tree.getCondition(), ImmutableList.of(tree.getCondition(), tree.getStatement()));
  }

  @Override
  public Description matchForLoop(ForLoopTree tree, VisitorState state) {
    if (tree.getCondition() == null) {
      return NO_MATCH;
    }
    return check(
        tree.getCondition(),
        ImmutableList.<Tree>builder()
            .add(tree.getCondition())
            .add(tree.getStatement())
            .addAll(tree.getUpdate())
            .build());
  }

  @Override
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    return check(tree.getCondition(), ImmutableList.of(tree.getCondition(), tree.getStatement()));
  }

  private Description check(ExpressionTree condition, ImmutableList<Tree> loopBodyTrees) {
    ImmutableSet<Symbol.VarSymbol> conditionVars = LoopConditionVisitor.scan(condition);
    if (conditionVars.isEmpty()) {
      return NO_MATCH;
    }
    for (Tree tree : loopBodyTrees) {
      if (UpdateScanner.scan(tree, conditionVars)) {
        return NO_MATCH;
      }
    }
    return buildDescription(condition)
        .setMessage(
            String.format(
                "condition variable(s) never modified in loop body: %s",
                Joiner.on(", ").join(conditionVars)))
        .build();
  }

  /** Scan for loop conditions that are determined entirely by the state of local variables. */
  private static class LoopConditionVisitor extends SimpleTreeVisitor<Boolean, Void> {

    static ImmutableSet<Symbol.VarSymbol> scan(Tree tree) {
      ImmutableSet.Builder<Symbol.VarSymbol> conditionVars = ImmutableSet.builder();
      if (!firstNonNull(tree.accept(new LoopConditionVisitor(conditionVars), null), false)) {
        return ImmutableSet.of();
      }
      return conditionVars.build();
    }

    private final ImmutableSet.Builder<Symbol.VarSymbol> conditionVars;

    public LoopConditionVisitor(ImmutableSet.Builder<Symbol.VarSymbol> conditionVars) {
      this.conditionVars = conditionVars;
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym instanceof Symbol.VarSymbol) {
        switch (sym.getKind()) {
          case LOCAL_VARIABLE:
          case PARAMETER:
            conditionVars.add((Symbol.VarSymbol) sym);
            return true;
          default: // fall out
        }
      }
      return false;
    }

    @Override
    public Boolean visitLiteral(LiteralTree tree, Void unused) {
      return true;
    }

    @Override
    public Boolean visitUnary(UnaryTree node, Void aVoid) {
      return node.getExpression().accept(this, null);
    }

    @Override
    public Boolean visitBinary(BinaryTree node, Void aVoid) {
      return firstNonNull(node.getLeftOperand().accept(this, null), false)
          && firstNonNull(node.getRightOperand().accept(this, null), false);
    }
  }

  /** Scan for updates to the given variables. */
  private static class UpdateScanner extends TreeScanner<Void, Void> {

    public static boolean scan(Tree tree, ImmutableSet<Symbol.VarSymbol> variables) {
      UpdateScanner scanner = new UpdateScanner(variables);
      tree.accept(scanner, null);
      return scanner.modified;
    }

    private boolean modified = false;
    private final ImmutableSet<Symbol.VarSymbol> variables;

    public UpdateScanner(ImmutableSet<Symbol.VarSymbol> variables) {
      this.variables = variables;
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void unused) {
      switch (tree.getKind()) {
        case POSTFIX_INCREMENT:
        case PREFIX_INCREMENT:
        case POSTFIX_DECREMENT:
        case PREFIX_DECREMENT:
          check(tree.getExpression());
          break;
        default: // fall out
      }
      return super.visitUnary(tree, unused);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      check(ASTHelpers.getReceiver(tree));
      return super.visitMethodInvocation(tree, unused);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void unused) {
      check(tree.getVariable());
      return super.visitAssignment(tree, unused);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
      check(tree.getVariable());
      return super.visitCompoundAssignment(tree, unused);
    }

    private void check(ExpressionTree expression) {
      Symbol sym = ASTHelpers.getSymbol(expression);
      modified |= variables.contains(sym);
    }
  }
}
