/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Types;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    name = "NestedInstanceOfConditions",
    summary =
        "Nested instanceOf conditions of disjoint types create blocks of code that never execute",
    severity = WARNING)
public class NestedInstanceOfConditions extends BugChecker implements IfTreeMatcher {

  @Override
  public Description matchIf(IfTree ifTree, VisitorState visitorState) {

    ExpressionTree expressionTree = stripParentheses(ifTree.getCondition());

    if (expressionTree instanceof InstanceOfTree) {
      InstanceOfTree instanceOfTree = (InstanceOfTree) expressionTree;

      if (!(instanceOfTree.getExpression() instanceof IdentifierTree)) {
        return Description.NO_MATCH;
      }

      Matcher<Tree> assignmentTreeMatcher =
          new AssignmentTreeMatcher(instanceOfTree.getExpression());
      Matcher<Tree> containsAssignmentTreeMatcher = contains(assignmentTreeMatcher);

      if (containsAssignmentTreeMatcher.matches(ifTree, visitorState)) {
        return Description.NO_MATCH;
      }

      // set expression and type to look for in matcher
      Matcher<Tree> nestedInstanceOfMatcher =
          new NestedInstanceOfMatcher(instanceOfTree.getExpression(), instanceOfTree.getType());

      Matcher<Tree> containsNestedInstanceOfMatcher = contains(nestedInstanceOfMatcher);

      if (containsNestedInstanceOfMatcher.matches(ifTree.getThenStatement(), visitorState)) {
        return describeMatch(ifTree);
      }
    }

    return Description.NO_MATCH;
  }

  private static class AssignmentTreeMatcher implements Matcher<Tree> {
    private final ExpressionTree variableExpressionTree;

    public AssignmentTreeMatcher(ExpressionTree e) {
      variableExpressionTree = e;
    }

    @Override
    public boolean matches(Tree tree, VisitorState visitorState) {
      if (tree instanceof AssignmentTree) {
        return visitorState
            .getSourceForNode(variableExpressionTree)
            .equals(visitorState.getSourceForNode(((AssignmentTree) tree).getVariable()));
      }

      return false;
    }
  }

  /**
   * Matches if current tree is an if tree with an instanceof statement as a condition that checks
   * if an expression is an instanceof a type that is disjoint from the matcher's type that is set
   * beforehand.
   */
  private static class NestedInstanceOfMatcher implements Matcher<Tree> {
    private final ExpressionTree expressionTree;
    private final Tree typeTree;

    public NestedInstanceOfMatcher(ExpressionTree e, Tree t) {
      expressionTree = e;
      typeTree = t;
    }

    @Override
    public boolean matches(Tree tree, VisitorState state) {
      if (tree instanceof IfTree) {
        ExpressionTree conditionTree = ASTHelpers.stripParentheses(((IfTree) tree).getCondition());

        if (conditionTree instanceof InstanceOfTree) {
          InstanceOfTree instanceOfTree = (InstanceOfTree) conditionTree;

          Types types = state.getTypes();

          boolean isCastable =
              types.isCastable(
                  types.erasure(ASTHelpers.getType(instanceOfTree.getType())),
                  types.erasure(ASTHelpers.getType(typeTree)));

          boolean isSameExpression =
              instanceOfTree.getExpression().toString().equals(expressionTree.toString());

          return isSameExpression && !isCastable;
        }
      }
      return false;
    }
  }
}
