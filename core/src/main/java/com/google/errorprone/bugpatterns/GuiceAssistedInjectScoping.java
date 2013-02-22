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

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;

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
    category = GUICE, severity = ERROR, maturity = MATURE)
public class GuiceAssistedInjectScoping extends DescribingMatcher<ClassTree> {

  private static final String SCOPE_ANNOTATION_STRING = "com.google.inject.ScopeAnnotation";
  private static final String ASSISTED_ANNOTATION_STRING =
      "com.google.inject.assistedinject.Assisted";
  private static final String INJECT_ANNOTATION_STRING = "com.google.inject.Inject";

  /**
   * Matches classes that have an annotation that itself is annotated with @ScopeAnnotation.
   */
  private MultiMatcher<ClassTree, AnnotationTree> classAnnotationMatcher =
     annotations(ANY, hasAnnotation(SCOPE_ANNOTATION_STRING, AnnotationTree.class));

  /**
   * Matches a class that has a constructor with at least one parameter that has been annotated
   * with @AssistedInject.  Assumes there is <= 1 @Inject constructor.  If there is an @Inject
   * constructor, then the @Inject constructor must have the parameter.  Otherwise, any constructor
   * may have the parameter.
   */
  private Matcher<ClassTree> constructorHasAssistedParams = new Matcher<ClassTree>() {
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      MultiMatcher<MethodTree, VariableTree> paramWithAssistedInjectMatcher =
          methodHasParameters(ANY, hasAnnotation(ASSISTED_ANNOTATION_STRING, VariableTree.class));
      MultiMatcher<ClassTree, MethodTree> constructorWithInjectMatcher =
          constructor(ANY, hasAnnotation(INJECT_ANNOTATION_STRING, MethodTree.class));
      if (constructorWithInjectMatcher.matches(classTree, state)) {
        // Check constructor with @Inject annotation for parameter with @AssistedInject annotation.
        return paramWithAssistedInjectMatcher.matches(
            constructorWithInjectMatcher.getMatchingNode(), state);
      }

      return constructor(ANY, paramWithAssistedInjectMatcher)
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
    AnnotationTree annotationWithScopeAnnotation = classAnnotationMatcher.getMatchingNode();
    if (annotationWithScopeAnnotation == null) {
      throw new IllegalStateException("Expected to find an annotation that was annotated " +
          "with @ScopeAnnotation");
    }

    return new Description(
        annotationWithScopeAnnotation,
        diagnosticMessage,
        new SuggestedFix().delete(annotationWithScopeAnnotation));
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
