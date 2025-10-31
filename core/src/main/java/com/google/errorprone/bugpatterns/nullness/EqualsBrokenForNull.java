/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * {@link BugChecker} adds a null check to {@code equals()} method implementations which don't
 * satisfy the null contract of {@link Object#equals} i.e. {@code equals(null)} should return false.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "equals() implementation may throw NullPointerException when given null",
    severity = SeverityLevel.WARNING)
public class EqualsBrokenForNull extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!Matchers.equalsMethodDeclaration().matches(tree, state)) {
      return NO_MATCH;
    }
    // Keep track of variables which, if true, imply that the incoming variable is non-null.
    Set<VarSymbol> impliesNonNull = new HashSet<>();
    Set<VarSymbol> incomingVariableSymbols = new HashSet<>();
    VarSymbol varSymbol = getSymbol(getOnlyElement(tree.getParameters()));
    incomingVariableSymbols.add(varSymbol);
    NullnessAnalysis analysis = NullnessAnalysis.instance(state.context);
    // we run nullness analysis on all the subtrees and match if there is a method invocation on
    // the argument to the equals method.
    boolean[] crashesWithNull = {false};
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree node, Void unused) {
        if (!crashesWithNull[0]) {
          Symbol symbol = getSymbol(node.getExpression());
          if (symbol instanceof VarSymbol && incomingVariableSymbols.contains(symbol)) {
            Nullness nullness =
                analysis.getNullness(
                    new TreePath(getCurrentPath(), node.getExpression()), state.context);
            if (nullness == Nullness.NULLABLE) {
              crashesWithNull[0] = true;
            }
          }
        }
        return super.visitMemberSelect(node, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        // Track variables assigned from our parameter.
        Tree initializer = variableTree.getInitializer();
        VarSymbol symbol = getSymbol(variableTree);
        if (isConsideredFinal(symbol) && initializer instanceof InstanceOfTree instanceOf) {
          if (instanceOf.getExpression() instanceof IdentifierTree
              && incomingVariableSymbols.contains(getSymbol(instanceOf.getExpression()))) {
            impliesNonNull.add(getSymbol(variableTree));
          }
        }
        if (incomingVariableSymbols.contains(findVariable(variableTree.getInitializer()))) {
          incomingVariableSymbols.add(getSymbol(variableTree));
        }
        return super.visitVariable(variableTree, null);
      }

      @Override
      public Void visitIf(IfTree ifTree, Void unused) {
        ExpressionTree condition = ASTHelpers.stripParentheses(ifTree.getCondition());
        if (condition instanceof IdentifierTree && impliesNonNull.contains(getSymbol(condition))) {
          return scan(ifTree.getElseStatement(), null);
        }
        return super.visitIf(ifTree, null);
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        // Our nullness analyzer isn't good at straddling lambda boundaries.
        return null;
      }

      /**
       * Unwraps expressions like `(Foo) foo` or `((Foo) foo)` to return the VarSymbol of `foo`, or
       * null if the expression wasn't of this form.
       */
      private @Nullable VarSymbol findVariable(Tree tree) {
        while (tree != null) {
          switch (tree) {
            case TypeCastTree typeCastTree -> tree = typeCastTree.getExpression();
            case ParenthesizedTree parenthesizedTree -> tree = parenthesizedTree.getExpression();
            case IdentifierTree identifierTree -> {
              Symbol symbol = getSymbol(identifierTree);
              return symbol instanceof VarSymbol varSymbol ? varSymbol : null;
            }
            default -> {
              return null;
            }
          }
        }
        return null;
      }
    }.scan(state.getPath(), null);
    if (!crashesWithNull[0]) {
      return NO_MATCH;
    }
    String stringAddition = String.format("if (%s == null) { return false; }\n", varSymbol.name);
    Fix fix = SuggestedFix.prefixWith(tree.getBody().getStatements().getFirst(), stringAddition);
    return describeMatch(tree, fix);
  }
}
