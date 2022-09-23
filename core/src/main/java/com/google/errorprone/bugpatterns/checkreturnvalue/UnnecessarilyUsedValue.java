/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Name;

/**
 * Checker that warns when capturing the result of an ignorable API into an {@code unused} variable.
 */
@BugPattern(
    summary =
        "The result of this API is ignorable, so it does not need to be captured / assigned into an"
            + " `unused` variable.",
    severity = WARNING)
public final class UnnecessarilyUsedValue extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchAssignment(AssignmentTree assignmentTree, VisitorState state) {
    if (isTryResource(assignmentTree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree expressionTree = assignmentTree.getExpression();
    if (isMethodInvocationLike(expressionTree)
        && assignmentTree.getVariable() instanceof IdentifierTree
        && isIgnorable(expressionTree, ((IdentifierTree) assignmentTree.getVariable()).getName())) {
      return describeMatch(
          assignmentTree,
          SuggestedFix.replace(assignmentTree, state.getSourceForNode(expressionTree)));
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    if (isTryResource(variableTree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree initializer = variableTree.getInitializer();
    if (isMethodInvocationLike(initializer) && isIgnorable(initializer, variableTree.getName())) {
      return describeMatch(
          variableTree,
          SuggestedFix.replace(variableTree, state.getSourceForNode(initializer) + ";"));
    }
    return Description.NO_MATCH;
  }

  private static boolean isMethodInvocationLike(ExpressionTree initializer) {
    return (initializer instanceof MethodInvocationTree || initializer instanceof NewClassTree);
  }

  private static boolean isTryResource(Tree expression, VisitorState state) {
    TryTree tryTree = findEnclosingNode(state.getPath(), TryTree.class);
    return (tryTree != null) && tryTree.getResources().contains(expression);
  }

  private static boolean isIgnorable(ExpressionTree methodInvocationTree, Name name) {
    // TODO(kak): use the ResultUsePolicyEvaluator from the CheckReturnValue checker
    return hasDirectAnnotationWithSimpleName(
            getSymbol(methodInvocationTree), "CanIgnoreReturnValue")
        && name.contentEquals("unused");
  }
}
