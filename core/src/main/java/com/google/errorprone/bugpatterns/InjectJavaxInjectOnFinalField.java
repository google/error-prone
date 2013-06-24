/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import static javax.lang.model.element.Modifier.FINAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "InjectJavaxInjectOnFinalField",
    summary = "@javac.inject.Inject cannot be put on a final field.", explanation =
        "According to the JSR-330 spec, the @javax.inject.Inject annotation "
        + "cannot go on final fields.)", category = INJECT, severity = ERROR,
    maturity = EXPERIMENTAL)
public class InjectJavaxInjectOnFinalField extends DescribingMatcher<AnnotationTree> {

  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  private static final Matcher<Tree> FINAL_FIELD_MATCHER = new Matcher<Tree>() {
    @Override
    public boolean matches(Tree t, VisitorState state) {
      return isField(t, state) && ((VariableTree) t).getModifiers().getFlags().contains(FINAL);
    }
  };

  private static final Matcher<AnnotationTree> JAVAX_INJECT_ANNOTATION_MATCHER =
      new Matcher<AnnotationTree>() {
        @Override
        public boolean matches(AnnotationTree t, VisitorState state) {
          return ASTHelpers.getSymbol(t)
              .equals(state.getSymbolFromString(JAVAX_INJECT_ANNOTATION));
        }
      };

  @Override
  @SuppressWarnings("unchecked")
  public final boolean matches(AnnotationTree annotationTree, VisitorState state) {
    return JAVAX_INJECT_ANNOTATION_MATCHER.matches(annotationTree, state)
        && FINAL_FIELD_MATCHER.matches(getAnnotatedNode(state), state);
  }

  @Override
  public Description describe(AnnotationTree annotationTree, VisitorState state) {
    return new Description(
        annotationTree, getDiagnosticMessage(), new SuggestedFix().delete(annotationTree));
  }

  private static Tree getAnnotatedNode(VisitorState state) {
    return state.getPath().getParentPath().getParentPath().getLeaf();
  }

  private static boolean isField(Tree tree, VisitorState state) {
    return tree.getKind().equals(VARIABLE)
        && ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class)
            .getMembers().contains(tree);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<AnnotationTree> annotationMatcher =
        new InjectJavaxInjectOnFinalField();

    @Override
    public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
      evaluateMatch(annotationTree, visitorState, annotationMatcher);
      return super.visitAnnotation(annotationTree, visitorState);
    }
  }
}
