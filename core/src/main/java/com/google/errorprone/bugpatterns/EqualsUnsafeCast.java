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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks for {@code equals} implementations making unsafe casts.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "EqualsUnsafeCast",
    summary =
        "The contract of #equals states that it should return false for incompatible types, "
            + "while this implementation may throw ClassCastException.",
    providesFix = REQUIRES_HUMAN_ATTENTION,
    severity = WARNING)
public final class EqualsUnsafeCast extends BugChecker implements MethodTreeMatcher {

  private static final String INSTANCEOF_CHECK = "if (!(%s instanceof %s)) { return false; }";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!equalsMethodDeclaration().matches(tree, state)) {
      return NO_MATCH;
    }

    Symbol parameter = getSymbol(getOnlyElement(tree.getParameters()));

    new TreePathScanner<Void, Void>() {
      private boolean methodInvoked = false;
      private final List<Type> checkedTypes = new ArrayList<>();

      @Override
      public Void visitInstanceOf(InstanceOfTree node, Void unused) {
        checkedTypes.add(getType(node.getType()));
        return super.visitInstanceOf(node, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        // Some equals implementations rely on super#equals for their class check. To avoid false
        // positives there, consider any method invocation to imply that this might be safe.
        // This also matches for any class comparisons with getClass.
        methodInvoked = true;
        return null;
      }

      @Override
      public Void visitTypeCast(TypeCastTree node, Void unused) {
        ExpressionTree expression = node.getExpression();
        if (!methodInvoked
            && expression.getKind() == Kind.IDENTIFIER
            && parameter.equals(getSymbol(expression))
            && checkedTypes.stream().noneMatch(t -> isSubtype(t, getType(node.getType()), state))) {
          StatementTree enclosingStatement =
              findEnclosingNode(getCurrentPath(), StatementTree.class);
          state.reportMatch(
              describeMatch(
                  node,
                  SuggestedFix.prefixWith(
                      enclosingStatement,
                      String.format(
                          INSTANCEOF_CHECK,
                          state.getSourceForNode(expression),
                          state.getSourceForNode(node.getType())))));
        }
        return super.visitTypeCast(node, null);
      }
    }.scan(state.getPath(), null);

    return NO_MATCH;
  }
}
