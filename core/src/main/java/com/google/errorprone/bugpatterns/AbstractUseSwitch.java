/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Helper for refactoring from if-else chains to switches. */
public abstract class AbstractUseSwitch extends BugChecker implements IfTreeMatcher {

  private static final int MIN_BRANCHES = 3;

  private static final Matcher<ExpressionTree> EQUALS =
      instanceMethod().anyClass().named("equals").withParameters("java.lang.Object");

  /** Returns the source text that should appear in a {@code case} statement in the fix. */
  protected abstract @Nullable String getExpressionForCase(
      VisitorState state, ExpressionTree argument);

  private static boolean isValidCaseBlock(StatementTree tree) {
    if (!(tree instanceof JCBlock)) {
      return false;
    }
    boolean[] good = {true};
    new TreeScanner<Void, Void>() {

      @Override
      public Void visitBreak(BreakTree t, Void v) {
        // breaks would definitely break a switch!
        good[0] = false;
        return null;
      }

      @Override
      public Void visitVariable(VariableTree t, Void v) {
        // If there are variables, we'd need braces; we just forbid them blindly for now.
        good[0] = false;
        return null;
      }
    }.scan(tree, null);
    return good[0];
  }

  /** Returns the source code from a JCBlock {...} without the curly brackets. */
  private static CharSequence getBlockContents(BlockTree block, VisitorState state) {
    final List<? extends StatementTree> statements = block.getStatements();
    if (statements.isEmpty()) {
      return "";
    }
    final int start = ((JCTree) statements.get(0)).getStartPosition();
    final int end = state.getEndPosition(getLast(statements));
    return state.getSourceCode().subSequence(start, end);
  }

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    if (state.getPath().getParentPath().getLeaf().getKind() == Kind.IF) {
      return NO_MATCH;
    }
    List<String> stringConstants = new ArrayList<>();
    List<JCBlock> branches = new ArrayList<>();
    IdentifierTree var = null;
    StatementTree statementTree = tree;
    while (statementTree instanceof IfTree) {
      IfTree ifTree = (IfTree) statementTree;
      ExpressionTree cond = TreeInfo.skipParens((JCExpression) ifTree.getCondition());
      ExpressionTree lhs;
      ExpressionTree rhs;
      if (EQUALS.matches(cond, state)) {
        MethodInvocationTree call = (MethodInvocationTree) cond;
        lhs = getReceiver(call);
        rhs = getOnlyElement(call.getArguments());
      } else if (cond.getKind().equals(Kind.EQUAL_TO)) {
        BinaryTree equalTo = (BinaryTree) cond;
        lhs = equalTo.getLeftOperand();
        rhs = equalTo.getRightOperand();
      } else {
        return NO_MATCH;
      }
      if (!(lhs instanceof IdentifierTree)) {
        return NO_MATCH;
      }
      IdentifierTree identifierTree = (IdentifierTree) lhs;
      if (var == null) {
        var = identifierTree;
        // This is the first if block, and identifierTree is the string variable
      } else if (!(identifierTree.getName().equals(var.getName())
          && isValidCaseBlock(ifTree.getThenStatement()))) {
        return NO_MATCH;
      }
      String expressionForCase = getExpressionForCase(state, rhs);
      if (expressionForCase == null) {
        return NO_MATCH;
      }
      stringConstants.add(expressionForCase);
      if (ifTree.getThenStatement().getKind() == Kind.BLOCK) {
        branches.add((JCBlock) ifTree.getThenStatement());
      } else {
        TreeMaker maker = TreeMaker.instance(state.context);
        branches.add(
            maker.Block(
                0, com.sun.tools.javac.util.List.of((JCStatement) ifTree.getThenStatement())));
      }
      statementTree = ifTree.getElseStatement();
    }
    Optional<JCBlock> defaultBranch =
        (statementTree instanceof JCBlock)
            ? Optional.of((JCBlock) statementTree)
            : Optional.absent();
    if (stringConstants.size() + defaultBranch.asSet().size() < MIN_BRANCHES) {
      return NO_MATCH;
    }
    StringBuilder builder = new StringBuilder();
    builder.append("switch (").append(var.getName()).append(") {\n");
    for (int i = 0; i < stringConstants.size(); i++) {
      builder
          .append("case ")
          .append(stringConstants.get(i))
          .append(":\n")
          .append(getBlockContents(branches.get(i), state));
      if (Reachability.canCompleteNormally(branches.get(i))) {
        builder.append("\nbreak;\n");
      }
    }
    builder
        .append("default:\n")
        .append(
            defaultBranch.isPresent()
                ? getBlockContents(defaultBranch.get(), state)
                : "// fall through")
        .append("\n}");
    return describeMatch(tree, SuggestedFix.replace(tree, builder.toString()));
  }
}
