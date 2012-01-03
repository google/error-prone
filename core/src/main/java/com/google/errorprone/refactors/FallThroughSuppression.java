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

package com.google.errorprone.refactors;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.RefactoringVisitorState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;

import java.util.Collection;

import static com.google.errorprone.BugPattern.Category.APPLICATION_SPECIFIC;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author pepstein@google.com (Peter Epstein)
 */
@BugPattern(
    name = "Fallthrough suppression",
    category = APPLICATION_SPECIFIC,
    severity = WARNING,
    maturity = EXPERIMENTAL,
    summary = "Remove fallthrough warning suppression annotations",
    explanation =
        "Remove all arguments to @SuppressWarnings annotations that suppress the Java " +
        "compiler's fallthrough warning. If there are no more arguments in a " +
        "@SuppressWarnings annotation, remove the whole annotation.\n" +
        "Note: This checker was specific to a refactoring we performed and should not be " +
        "used as a general error or warning.")
public class FallThroughSuppression extends RefactoringMatcher<AnnotationTree> {

  @Override
  @SuppressWarnings({"varargs", "unchecked"})
  public boolean matches(AnnotationTree annotationTree, VisitorState state) {
    return allOf(isType("java.lang.SuppressWarnings"),
                 hasElementWithValue("value", stringLiteral("fallthrough")))
        .matches(annotationTree, state);
  }

  @Override
  public Refactor refactor(AnnotationTree annotationTree, RefactoringVisitorState state) {
    return new Refactor(
        annotationTree,
        "this has no effect if fallthrough warning is suppressed",
        getSuggestedFix(annotationTree, state));
  }

  private SuggestedFix getSuggestedFix(AnnotationTree annotationTree, RefactoringVisitorState state) {
    ListBuffer<JCTree.JCExpression> arguments = new ListBuffer<JCTree.JCExpression>();
    for (ExpressionTree argumentTree : annotationTree.getArguments()) {
      AssignmentTree assignmentTree = (AssignmentTree) argumentTree;
      if (assignmentTree.getVariable().toString().equals("value")) {
        ExpressionTree expressionTree = assignmentTree.getExpression();
        switch (expressionTree.getKind()) {
          case STRING_LITERAL:
              // Fallthrough was the only thing suppressed, so remove line.
            return new SuggestedFix().delete(annotationTree);
          case NEW_ARRAY:
            JCTree.JCNewArray newArray =
                initializersWithoutFallthrough(state, (NewArrayTree) expressionTree);
            if (newArray.getInitializers().size() >= 2) {
              arguments.add(newArray);
            } else if (newArray.getInitializers().size() == 1) {
              // Only one left, so get rid of the curly braces.
              arguments.add(
                  singleInitializer((JCTree.JCLiteral) newArray.getInitializers().get(0), state));
            } else {
              // Fallthrough was the only thing suppressed, so remove line.
              return new SuggestedFix().delete(annotationTree);
            }
            break;
        }
      } else {
        // SuppressWarnings only has a value element, but if they ever add more,
        // we want to keep them.
        arguments.add((JCTree.JCExpression) argumentTree);
      }
    }
    JCTree.JCAnnotation replacement = state.getTreeMaker()
        .Annotation((JCTree) annotationTree.getAnnotationType(), arguments.toList());
    return new SuggestedFix()
        .replace(annotationTree, replacement.toString());
  }

  private JCTree.JCLiteral singleInitializer(JCTree.JCLiteral literalExpression,
      RefactoringVisitorState state) {
    return state.getTreeMaker().Literal(literalExpression.getValue());
  }

  @SuppressWarnings("unchecked")
  private JCTree.JCNewArray initializersWithoutFallthrough(RefactoringVisitorState state,
      NewArrayTree expressionTree) {
    ListBuffer<JCTree.JCExpression> replacementInitializers = new ListBuffer<JCTree.JCExpression>();
    ListBuffer<JCTree.JCExpression> dimensions = new ListBuffer<JCTree.JCExpression>();
    dimensions.addAll((Collection<? extends JCTree.JCExpression>) expressionTree.getDimensions());
    for (ExpressionTree elementTree : expressionTree.getInitializers()) {
      if (!stringLiteral("fallthrough").matches(elementTree, state)) {
        replacementInitializers.add((JCTree.JCExpression) elementTree);
      }
    }
    return state.getTreeMaker().NewArray((JCTree.JCExpression) expressionTree.getType(),
        dimensions.toList(), replacementInitializers.toList());
  }


  public static class Scanner extends TreePathScanner<Void, RefactoringVisitorState> {
    public RefactoringMatcher<AnnotationTree> annotationChecker = new FallThroughSuppression();

    @Override
    public Void visitAnnotation(AnnotationTree annotationTree, RefactoringVisitorState visitorState) {
      RefactoringVisitorState state = visitorState.withPath(getCurrentPath());
      if (annotationChecker.matches(annotationTree, state)) {
        visitorState.getReporter().report(annotationChecker.refactor(annotationTree, state));
      }

      super.visitAnnotation(annotationTree, visitorState);
      return null;
    }
  }
}
