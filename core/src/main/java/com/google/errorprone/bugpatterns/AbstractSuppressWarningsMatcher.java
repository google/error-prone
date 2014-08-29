/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;

import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract matcher which can process changes to a SuppressWarnings annotation.
 */
abstract class AbstractSuppressWarningsMatcher extends BugChecker
    implements AnnotationTreeMatcher {

  /**
   * Processes the list of SuppressWarnings values in-place when creating a {@link SuggestedFix}.
   * Items may be added, removed or re-ordered as necessary. The initial input are the values
   * in the order specified in the code being processed.
   *
   * @param values list of suppressed warnings in the order in which they appear in the code
   */
  abstract protected void processSuppressWarningsValues(List<String> values);

  protected final Fix getSuggestedFix(AnnotationTree annotationTree) {
    List<String> values = new ArrayList<>();
    for (ExpressionTree argumentTree : annotationTree.getArguments()) {
      AssignmentTree assignmentTree = (AssignmentTree) argumentTree;
      if (assignmentTree.getVariable().toString().equals("value")) {
        ExpressionTree expressionTree = assignmentTree.getExpression();
        switch (expressionTree.getKind()) {
          case STRING_LITERAL:
            values.add(((String) ((JCTree.JCLiteral) expressionTree).value));
            break;
          case NEW_ARRAY:
            NewArrayTree newArrayTree = (NewArrayTree) expressionTree;
            for (ExpressionTree elementTree : newArrayTree.getInitializers()) {
              values.add((String) ((JCTree.JCLiteral) elementTree).value);
            }
            break;
          default:
            throw new AssertionError("Unknown kind: " + expressionTree.getKind());
        }
        processSuppressWarningsValues(values);
      } else {
        throw new AssertionError("SuppressWarnings has an element other than value=");
      }
    }

    if (values.isEmpty()) {
      return SuggestedFix.delete(annotationTree);
    } else if (values.size() == 1) {
      return SuggestedFix
          .replace(annotationTree, "@SuppressWarnings(\"" + values.get(0) + "\")");
    } else {
      StringBuilder sb = new StringBuilder("@SuppressWarnings({\"" + values.get(0) + "\"");
      for (int i = 1; i < values.size(); i++) {
        sb.append(", ");
        sb.append("\"" + values.get(i) + "\"");
      }
      sb.append("})");
      return SuggestedFix.replace(annotationTree, sb.toString());
    }
  }

}
