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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Refactor explicit null checks to Objects.requireNonNull",
    severity = SUGGESTION)
public final class RequireNonNullRefactoring extends BugChecker implements IfTreeMatcher {

  private static final Supplier<Type> NULL_POINTER_EXCEPTION =
      typeFromString("java.lang.NullPointerException");

  @Override
  public Description matchIf(IfTree ifTree, VisitorState state) {
    if (ifTree.getElseStatement() != null) {
      return NO_MATCH;
    }
    ExpressionTree targetExpr = getNullCheckedExpression(ifTree.getCondition());
    if (targetExpr == null) {
      return NO_MATCH;
    }
    ThrowTree throwTree = getThrowStatement(ifTree.getThenStatement());
    if (throwTree == null) {
      return NO_MATCH;
    }
    ExpressionTree thrownExpr = stripParentheses(throwTree.getExpression());
    if (!(thrownExpr instanceof NewClassTree newClassTree)) {
      return NO_MATCH;
    }
    if (!isSameType(getType(newClassTree), NULL_POINTER_EXCEPTION.get(state), state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = newClassTree.getArguments();
    @Nullable ExpressionTree messageExpr;
    if (arguments.isEmpty()) {
      messageExpr = null;
    } else if (arguments.size() == 1) {
      messageExpr = arguments.get(0);
      if (constValue(messageExpr, String.class) == null) {
        return NO_MATCH;
      }
    } else {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    buildFix(fix, ifTree, targetExpr, messageExpr, state);
    return describeMatch(ifTree, fix.build());
  }

  private static void buildFix(
      SuggestedFix.Builder fix,
      IfTree ifTree,
      ExpressionTree targetExpr,
      @Nullable ExpressionTree messageExpr,
      VisitorState state) {
    String requireNonNullQualified =
        SuggestedFixes.qualifyStaticImport("java.util.Objects.requireNonNull", fix, state);
    String targetExprSource = state.getSourceForNode(targetExpr);
    String replacement =
        String.format(
            "%s(%s%s);",
            requireNonNullQualified,
            targetExprSource,
            messageExpr == null ? "" : ", " + state.getSourceForNode(messageExpr));
    fix.replace(ifTree, replacement);
  }

  private static @Nullable ExpressionTree getNullCheckedExpression(ExpressionTree condition) {
    condition = stripParentheses(condition);
    if (condition.getKind() != Kind.EQUAL_TO) {
      return null;
    }
    BinaryTree binary = (BinaryTree) condition;
    if (binary.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
      return binary.getRightOperand();
    }
    if (binary.getRightOperand().getKind() == Kind.NULL_LITERAL) {
      return binary.getLeftOperand();
    }
    return null;
  }

  private static @Nullable ThrowTree getThrowStatement(StatementTree then) {
    while (then instanceof BlockTree block) {
      var statements = block.getStatements();
      if (statements.size() != 1) {
        return null;
      }
      then = statements.getFirst();
    }
    return then instanceof ThrowTree throwTree ? throwTree : null;
  }
}
