/*
 * Copyright 2025 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

/** Simplifies Boolean.TRUE/FALSE to true/false. */
@BugPattern(
    summary = "This expression can be written more clearly with a boolean literal.",
    severity = WARNING)
public class BooleanLiteral extends BugChecker
    implements IdentifierTreeMatcher, MemberSelectTreeMatcher {
  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return match(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return match(tree, state);
  }

  private Description match(ExpressionTree tree, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (sym == null || sym.owner == null) {
      return NO_MATCH;
    }
    if (!sym.owner.equals(state.getTypes().boxedClass(state.getSymtab().booleanType))) {
      return NO_MATCH;
    }
    if (state.getPath().getParentPath().getLeaf() instanceof MemberReferenceTree) {
      return NO_MATCH;
    }
    boolean value;
    switch (sym.getSimpleName().toString()) {
      case "TRUE" -> value = true;
      case "FALSE" -> value = false;
      default -> {
        return NO_MATCH;
      }
    }
    TreePath parentPath = state.getPath().getParentPath();
    if (parentPath.getLeaf() instanceof MemberSelectTree memberSelectTree
        && parentPath.getParentPath().getLeaf() instanceof MethodInvocationTree invocationTree
        && invocationTree.getMethodSelect().equals(memberSelectTree)) {
      Symbol.MethodSymbol methodSym = getSymbol(invocationTree);
      return switch (methodSym.getSimpleName().toString()) {
        case "toString" ->
            describeMatch(
                tree,
                SuggestedFix.replace(
                    invocationTree, state.getConstantExpression(Boolean.toString(value))));
        case "booleanValue" ->
            describeMatch(tree, SuggestedFix.replace(invocationTree, Boolean.toString(value)));
        default -> NO_MATCH;
      };
    }
    return describeMatch(tree, SuggestedFix.replace(tree, Boolean.toString(value)));
  }
}
