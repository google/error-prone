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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "GuiceAssistedInjectScoping",
    summary = "Refactor uses of the Guice @ScopeAnnotation",
    explanation =
        "If a class is annotated with an annotation that itself is annotated with " +
        "@ScopeAnnotation, and any of the parameters of its constructor are annotated with " +
        "@Assisted, remove the annotation on the class.",
    category = ONE_OFF, severity = NOT_A_PROBLEM, maturity = EXPERIMENTAL)
public class GuiceAssistedInjectScoping extends DescribingMatcher<ClassTree> {

  private static final String SCOPE_ANNOTATION_STRING = "com.google.inject.ScopeAnnotation";
  private static final String ASSISTED_ANNOTATION_STRING =
      "com.google.inject.assistedinject.Assisted";
  private static final String INJECT_ANNOTATION_STRING = "com.google.inject.Inject";

  private Matcher<ClassTree> classAnnotationMatcher = new Matcher<ClassTree>() {
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      return annotations(true, hasAnnotation(SCOPE_ANNOTATION_STRING, AnnotationTree.class))
          .matches(classTree, state);
    }
  };

  /**
   * Assume there is <= 1 @Inject constructor.  If there is an @Inject constructor, then the
   * @Inject constructor must match.  Otherwise, any constructor may match.
   */
  private Matcher<ClassTree> constructorHasAssistedParams = new Matcher<ClassTree>() {
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      // TODO(eaftan): We're repeating a lot of work here.  Perhaps the Matcher interface should
      // give access to the tree node that matched?
      if (constructor(true, hasAnnotation(INJECT_ANNOTATION_STRING, MethodTree.class))
          .matches(classTree, state)) {
        return constructor(true, allOf(
            hasAnnotation(INJECT_ANNOTATION_STRING, MethodTree.class),
            methodHasParameters(true, hasAnnotation(ASSISTED_ANNOTATION_STRING, VariableTree.class))))
            .matches(classTree, state);
      }

      return constructor(true, methodHasParameters(true,
          hasAnnotation(ASSISTED_ANNOTATION_STRING, VariableTree.class)))
          .matches(classTree, state);
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
    public DescribingMatcher<ClassTree> classMatcher = new GuiceAssistedInjectScoping();

    @Override
    public Void visitClass(ClassTree classTree, VisitorState visitorState) {
      evaluateMatch(classTree, visitorState, classMatcher);
      return super.visitClass(classTree, visitorState);
    }
  }
}
