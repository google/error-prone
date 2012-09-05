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

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract matcher which can process changes to a SuppressWarnings annotation.
 */
abstract class AbstractSuppressWarningsMatcher extends DescribingMatcher<AnnotationTree> {

  /**
   * Processes the list of SuppressWarnings values in-place when creating a {@link SuggestedFix}.
   * Items may be added, removed or re-ordered as necessary. The initial input are the values
   * in the order specified in the code being processed.
   * 
   * @param values list of suppressed warnings in the order in which they appear in the code
   */
  abstract protected void processSuppressWarningsValues(List<String> values);

  @Override
  public final Description describe(AnnotationTree annotationTree, VisitorState state) {
    return new Description(
        annotationTree,
        diagnosticMessage,
        getSuggestedFix(annotationTree, state));
  }
  
  protected final SuggestedFix getSuggestedFix(AnnotationTree annotationTree, VisitorState state) {
    ListBuffer<JCTree.JCExpression> arguments = new ListBuffer<JCTree.JCExpression>();
    List<String> values = new ArrayList<String>();
    for (ExpressionTree argumentTree : annotationTree.getArguments()) {
      AssignmentTree assignmentTree = (AssignmentTree) argumentTree;
      if (assignmentTree.getVariable().toString().equals("value")) {
        ExpressionTree expressionTree = assignmentTree.getExpression();
        switch (expressionTree.getKind()) {
          case STRING_LITERAL:
            values.add(((String) ((JCTree.JCLiteral) expressionTree).value));
            break;
          case NEW_ARRAY:
            ListBuffer<JCTree.JCExpression> dimensions = new ListBuffer<JCTree.JCExpression>();
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
    
    if (values.size() == 0) {
      return new SuggestedFix().delete(annotationTree);
    } else if (values.size() == 1) {
      return new SuggestedFix().replace(annotationTree, "@SuppressWarnings(\"" + values.get(0) + "\")");
    } else {
      StringBuilder sb = new StringBuilder("@SuppressWarnings({\"" + values.get(0) + "\"");
      for (int i = 1; i < values.size(); i++) {
        sb.append(", ");
        sb.append("\"" + values.get(i) + "\"");
      }
      sb.append("})");
      return new SuggestedFix().replace(annotationTree, sb.toString());
    }
  }

}
