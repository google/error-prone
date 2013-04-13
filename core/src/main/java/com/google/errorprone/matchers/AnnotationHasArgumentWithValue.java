/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author pepstein@google.com (Peter Epstein)
 */
public class AnnotationHasArgumentWithValue implements Matcher<AnnotationTree> {

  private final String element;
  private final Matcher<ExpressionTree> valueMatcher;

  public AnnotationHasArgumentWithValue(String element, Matcher<ExpressionTree> valueMatcher) {
    this.element = element;
    this.valueMatcher = valueMatcher;
  }

  @Override
  public boolean matches(AnnotationTree annotationTree, VisitorState state) {
    for (ExpressionTree argumentTree : annotationTree.getArguments()) {
      if (argumentTree.getKind() == Tree.Kind.ASSIGNMENT) {
        AssignmentTree assignmentTree = (AssignmentTree) argumentTree;
        if (assignmentTree.getVariable().toString().equals(element)) {
          ExpressionTree expressionTree = assignmentTree.getExpression();
          while (expressionTree instanceof ParenthesizedTree) {
            expressionTree = ((ParenthesizedTree) expressionTree).getExpression();
          }

          if (expressionTree instanceof NewArrayTree) {
            NewArrayTree arrayTree = (NewArrayTree) expressionTree;
            for (ExpressionTree elementTree : arrayTree.getInitializers()) {
              if (valueMatcher.matches(elementTree, state)) {
                return true;
              }
            }
            return false;
          }

          return valueMatcher.matches(expressionTree, state);
        }
      }
    }
    return false;
  }
}
