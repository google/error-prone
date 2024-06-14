/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.SourceVersion.supportsPatternMatchingInstanceof;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This code can be simplified to use a pattern-matching instanceof.")
public final class PatternMatchingInstanceof extends BugChecker implements IfTreeMatcher {
  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    if (!supportsPatternMatchingInstanceof(state.context)) {
      return NO_MATCH;
    }
    ImmutableSet<InstanceOfTree> instanceofChecks = scanForInstanceOf(tree.getCondition());
    if (instanceofChecks.isEmpty()) {
      return NO_MATCH;
    }
    var body = tree.getThenStatement();
    if (!(body instanceof BlockTree)) {
      return NO_MATCH;
    }
    var block = (BlockTree) body;
    if (block.getStatements().isEmpty()) {
      return NO_MATCH;
    }
    var firstStatement = block.getStatements().get(0);
    if (!(firstStatement instanceof VariableTree)) {
      return NO_MATCH;
    }
    var variableTree = (VariableTree) firstStatement;
    if (!(variableTree.getInitializer() instanceof TypeCastTree)) {
      return NO_MATCH;
    }
    var typeCast = (TypeCastTree) variableTree.getInitializer();
    var matchingInstanceof =
        instanceofChecks.stream()
            .filter(
                i ->
                    isSameType(getType(i.getType()), getType(typeCast.getType()), state)
                        && getSymbol(i.getExpression()) instanceof VarSymbol
                        && getSymbol(i.getExpression()).equals(getSymbol(typeCast.getExpression())))
            .findFirst()
            .orElse(null);
    if (matchingInstanceof == null) {
      return NO_MATCH;
    }
    return describeMatch(
        firstStatement,
        SuggestedFix.builder()
            .delete(variableTree)
            .postfixWith(matchingInstanceof, " " + variableTree.getName().toString())
            .build());
  }

  private ImmutableSet<InstanceOfTree> scanForInstanceOf(ExpressionTree condition) {
    ImmutableSet.Builder<InstanceOfTree> instanceOfs = ImmutableSet.builder();
    new SimpleTreeVisitor<Void, Void>() {
      @Override
      public Void visitParenthesized(ParenthesizedTree tree, Void unused) {
        return visit(tree.getExpression(), null);
      }

      @Override
      public Void visitBinary(BinaryTree tree, Void unused) {
        if (tree.getKind() != Kind.CONDITIONAL_AND) {
          return null;
        }
        visit(tree.getLeftOperand(), null);
        visit(tree.getRightOperand(), null);
        return null;
      }

      @Override
      public Void visitInstanceOf(InstanceOfTree tree, Void unused) {
        instanceOfs.add(tree);
        return null;
      }
    }.visit(condition, null);
    return instanceOfs.build();
  }
}
