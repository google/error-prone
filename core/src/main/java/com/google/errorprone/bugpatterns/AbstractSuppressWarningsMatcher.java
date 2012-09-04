package com.google.errorprone.bugpatterns;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract matcher which can process changes to the a SuppressWarnings annotation.
 */
abstract class AbstractSuppressWarningsMatcher extends DescribingMatcher<AnnotationTree> {

  /**
   * Returns the matcher for the SuppressWarnings annotation.
   */
  abstract protected Matcher<AnnotationTree> getMatcher();

  /**
   * Processes the list of SuppressWarnings values in-place. Items may be added, removed or
   * re-ordered as necessary.
   */
  abstract protected void processSuppressWarningsValues(List<String> values);

  @Override
  public final boolean matches(AnnotationTree annotationTree, VisitorState state) {
    return getMatcher().matches(annotationTree, state);
  }
  
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
