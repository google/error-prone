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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.NOT_A_PROBLEM;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "GuiceScopingRefactor",
    summary = "Refactor uses of the Guice @ScopeAnnotation",
    explanation =
        "If a class is annotated with an annotation that itself is annotated with " +
        "@ScopeAnnotation, and any of the parameters of its constructor are annotated with " +
        "@Assisted, remove the annotation on the class.",
    category = ONE_OFF, severity = NOT_A_PROBLEM, maturity = EXPERIMENTAL)
public class GuiceScopingRefactor extends DescribingMatcher<ClassTree> {

  private static final String SCOPE_ANNOTATION_STRING = "com.google.inject.ScopeAnnotation";
  private static final String ASSISTED_ANNOTATION_STRING =
      "com.google.inject.assistedinject.Assisted";

  private Matcher<ClassTree> classAnnotationMatcher = new Matcher<ClassTree>() {
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      return Matchers.annotations(true,
          Matchers.hasAnnotation(SCOPE_ANNOTATION_STRING, AnnotationTree.class))
          .matches(classTree, state);
    }
  };

  private Matcher<ClassTree> constructorHasAssistedParams = new Matcher<ClassTree>() {
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      Symbol assistedAnnotation = state.getSymbolFromString(ASSISTED_ANNOTATION_STRING);
      if (assistedAnnotation == null) {
        return false;
      }

      JCClassDecl classDecl = (JCClassDecl) classTree;
      // Iterate over members of class (methods and fields).
      for (JCTree member : classDecl.getMembers()) {
        Symbol sym = ASTHelpers.getSymbol(member);
        // If this member is a constructor...
        if (sym.isConstructor()) {
          // Iterate over its parameters.
          for (JCVariableDecl param : ((JCMethodDecl) member).getParameters()) {
            // Does this param have the @Assisted annotation?
            if (Matchers.hasAnnotation(ASSISTED_ANNOTATION_STRING).matches(param, state)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  };

  @Override
  @SuppressWarnings("unchecked")
  public final boolean matches(ClassTree classTree, VisitorState state) {
    return Matchers.allOf(classAnnotationMatcher, constructorHasAssistedParams)
        .matches(classTree, state);
  }

  @Override
  public Description describe(ClassTree classTree, VisitorState state) {
    // TODO(eaftan): This recreates logic in the annotation matcher in matches() above.
    // We need a better way to do this.  Perhaps the matcher should keep track of the node that
    // matched/didn't match.
    for (AnnotationTree annotationTree : classTree.getModifiers().getAnnotations()) {
      if (Matchers.hasAnnotation(SCOPE_ANNOTATION_STRING).matches(annotationTree, state)) {
        return new Description(
            annotationTree,
            diagnosticMessage,
            new SuggestedFix().delete(annotationTree));
      }
    }

    throw new IllegalStateException("Expected to find an annotation that was annotated " +
        "with @ScopeAnnotation");
  }


  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<ClassTree> classMatcher = new GuiceScopingRefactor();

    @Override
    public Void visitClass(ClassTree classTree, VisitorState visitorState) {
      evaluateMatch(classTree, visitorState, classMatcher);
      return super.visitClass(classTree, visitorState);
    }
  }
}
