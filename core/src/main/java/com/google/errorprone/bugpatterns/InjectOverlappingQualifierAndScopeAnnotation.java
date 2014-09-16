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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "OverlappingQualifierAndScopeAnnotation",
    summary = "Annotations cannot be both Qualifiers/BindingAnnotations and Scopes",
    explanation = "Qualifiers and Scoping annotations have different semantic meanings and a "
        + "single annotation should not be both a qualifier and a scoping annotation",
    category = INJECT, severity = ERROR, maturity = EXPERIMENTAL)
public class InjectOverlappingQualifierAndScopeAnnotation extends BugChecker implements AnnotationTreeMatcher {

  private static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  private static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";
  private static final String GUICE_BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  private static final String JAVAX_QUALIFER_ANNOTATION = "javax.inject.Qualifier";

  /**
   * Matches types(including annotation types) that are annotated with  @ScopeAnnotation(Guice) or
   * {@code @Scope}(javax).
   */
  private static final Matcher<ClassTree> HAS_SCOPE_ANNOTATION_MATCHER =
      anyOf(hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION));

  /**
   * Matches types(including annotation types) that are annotated with @Qualifier or @BindingAnnotation
   */
  private static final Matcher<ClassTree> HAS_QUALIFIER_ANNOTATION_MATCHER =
      anyOf(hasAnnotation(GUICE_BINDING_ANNOTATION), hasAnnotation(JAVAX_QUALIFER_ANNOTATION));
  /**
   * Matches the following four annotations: 
   * (1) @javax.inject.Qualifier
   * (2) @com.google.inject.BindingAnnotation
   * (3) @javax.inject.Scope, 
   * (4) @com.google.inject.ScopeAnnotation
   * 
   * It matches the annotations themselves, NOT anotations annotated with them.
   */
  private static final Matcher<AnnotationTree> QUALIFIER_OR_SCOPE_MATCHER = new Matcher<AnnotationTree>() {
    @Override public boolean matches(AnnotationTree annotationTree, VisitorState state) {
      Symbol annotationSymbol = ASTHelpers.getSymbol(annotationTree);
      return (annotationSymbol.equals(state.getSymbolFromString(JAVAX_QUALIFER_ANNOTATION))
          || annotationSymbol.equals(state.getSymbolFromString(GUICE_BINDING_ANNOTATION))
          || annotationSymbol.equals(state.getSymbolFromString(JAVAX_SCOPE_ANNOTATION))
          || annotationSymbol.equals(state.getSymbolFromString(GUICE_SCOPE_ANNOTATION)));
    }
  };
      
  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (QUALIFIER_OR_SCOPE_MATCHER.matches(annotationTree, state)) {
      ClassTree annotationType = getAnnotationTypeFromMetaAnnotation(state);
      if (HAS_QUALIFIER_ANNOTATION_MATCHER.matches(annotationType, state)
          && HAS_SCOPE_ANNOTATION_MATCHER.matches(annotationType, state)) {
        return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
      }
    }
    return Description.NO_MATCH;
  }

  private static ClassTree getAnnotationTypeFromMetaAnnotation(VisitorState state) {
    return (ClassTree) state.getPath().getParentPath().getParentPath().getLeaf();
  }
}
