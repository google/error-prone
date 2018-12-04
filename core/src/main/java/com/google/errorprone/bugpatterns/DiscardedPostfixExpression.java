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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCLambda;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "DiscardedPostfixExpression",
    summary = "The result of this unary operation on a lambda parameter is discarded",
    severity = SeverityLevel.ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class DiscardedPostfixExpression extends BugChecker implements UnaryTreeMatcher {

  @Override
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case POSTFIX_INCREMENT:
      case POSTFIX_DECREMENT:
        break;
      default:
        return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() != Kind.LAMBDA_EXPRESSION) {
      return NO_MATCH;
    }
    JCLambda lambda = (JCLambda) parent;
    Symbol sym = ASTHelpers.getSymbol(tree.getExpression());
    if (lambda.getParameters().stream().noneMatch(p -> ASTHelpers.getSymbol(p) == sym)) {
      return NO_MATCH;
    }
    return describeMatch(
        tree,
        SuggestedFix.replace(
            tree,
            String.format(
                "%s %s 1",
                state.getSourceForNode(tree.getExpression()),
                tree.getKind() == Kind.POSTFIX_INCREMENT ? "+" : "-")));
  }
}
