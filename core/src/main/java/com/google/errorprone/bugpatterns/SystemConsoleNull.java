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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import java.util.Objects;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "System.console() no longer returns null in JDK 22 and newer versions",
    severity = WARNING)
public class SystemConsoleNull extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("java.lang.System").named("console");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    Tree enclosing = state.getPath().getParentPath().getLeaf();
    if (isNullCheck(enclosing)) {
      return describeMatch(enclosing, buildFix(enclosing));
    }
    if (enclosing instanceof VariableTree
        && Objects.equals(((VariableTree) enclosing).getInitializer(), tree)) {
      Symbol sym = ASTHelpers.getSymbol(enclosing);
      if (ASTHelpers.isConsideredFinal(sym)) {
        new SuppressibleTreePathScanner<Void, Void>(state) {
          @Override
          public Void visitIdentifier(IdentifierTree tree, Void unused) {
            if (sym.equals(ASTHelpers.getSymbol(tree))) {
              Tree enclosing = getCurrentPath().getParentPath().getLeaf();
              if (isNullCheck(enclosing)) {
                state.reportMatch(describeMatch(tree, buildFix(enclosing, tree, state)));
              }
            }
            return null;
          }
        }.scan(state.getPath().getCompilationUnit(), null);
      }
    }
    return NO_MATCH;
  }

  private static SuggestedFix buildFix(Tree enclosing) {
    return SuggestedFix.emptyFix();
  }

  private static SuggestedFix buildFix(
      Tree enclosing, IdentifierTree identifier, VisitorState state) {
    return SuggestedFix.emptyFix();
  }

  private static boolean isNullCheck(Tree tree) {
    if (!(tree instanceof BinaryTree)) {
      return false;
    }
    BinaryTree binaryTree = (BinaryTree) tree;
    switch (binaryTree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO -> {}
      default -> {
        return false;
      }
    }
    return binaryTree.getLeftOperand().getKind().equals(Tree.Kind.NULL_LITERAL)
        || binaryTree.getRightOperand().getKind().equals(Tree.Kind.NULL_LITERAL);
  }
}
