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
import static com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ModifiersTree;

/**
 * This checker matches if a class has more than one annotation that is a scope annotation(that is,
 * the annotation is either annotated with Guice's @ScopeAnnotation or Javax's @Scope).
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */

@BugPattern(name = "InjectMoreThanOneScopeAnnotationOnClass",
    summary = "A class can be annotated with at most one scope annotation", 
    explanation = "Annotating a class with more than one scope annotation is "
        + "invalid according to the JSR-330 specification. ", category = INJECT, severity = ERROR,
    maturity = EXPERIMENTAL)
public class InjectMoreThanOneScopeAnnotationOnClass extends BugChecker
    implements AnnotationTreeMatcher {

  private static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  private static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";

  /**
   * Matches annotations that are themselves annotated with with @ScopeAnnotation(Guice) or
   * @Scope(Javax).
   */
  private Matcher<AnnotationTree> scopeAnnotationMatcher = Matchers.<AnnotationTree>anyOf(
      hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION));

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    int numberOfScopeAnnotations = 0;
    // check if this annotation is on a class and is a scope annotation
    if (scopeAnnotationMatcher.matches(annotationTree, state)
        && state.getPath().getParentPath().getParentPath().getLeaf() instanceof ClassTree) {
      for (AnnotationTree annotation :
          ((ModifiersTree) state.getPath().getParentPath().getLeaf()).getAnnotations()) {
        if (scopeAnnotationMatcher.matches(annotation, state)) {
          numberOfScopeAnnotations++;
        }
      }
    }
    if (numberOfScopeAnnotations > 1) {
      return describeMatch(
        annotationTree, SuggestedFix.delete(annotationTree));
    }
    return Description.NO_MATCH;
  }
}
