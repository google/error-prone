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
import static com.google.errorprone.matchers.Matchers.anyOf;
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
 * This checker matches iff *both* of the following conditions are true:
 * 1) The class is assisted:
 *   a) If there is a constructor that is annotated with @Inject and that constructor has at least
 *      one parameter that is annotated with @Assisted.
 *   b) If there is no @Inject constructor and at least one constructor is annotated with
 *      @AssistedInject.
 * 2) There is an annotation on the class, and the annotation is itself annotated with
 *    @ScopeAnnotation.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "GuiceAssistedInjectScoping",
    summary = "Scope annotation on implementation class of AssistedInject factory is not allowed",
    explanation =
        "Classes that AssistedInject factories create may not be annotated with scope " +
        "annotations, such as @Singleton.  This will cause a Guice error at runtime.",
    category = GUICE, severity = ERROR, maturity = MATURE)
public class GuiceAssistedInjectScoping extends DescribingMatcher<ClassTree> {

  private static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  private static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";
  private static final String ASSISTED_ANNOTATION = "com.google.inject.assistedinject.Assisted";
  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";
  private static final String ASSISTED_INJECT_ANNOTATION =
      "com.google.inject.assistedinject.AssistedInject";

  /**
   * Matches classes that have an annotation that itself is annotated with @ScopeAnnotation.
   */
  @SuppressWarnings("unchecked")
  private MultiMatcher<ClassTree, AnnotationTree> classAnnotationMatcher =
     annotations(ANY, anyOf(hasAnnotation(GUICE_SCOPE_ANNOTATION, AnnotationTree.class),
         hasAnnotation(JAVAX_SCOPE_ANNOTATION)));

  /**
   * Matches if:
   * 1) If there is a constructor that is annotated with @Inject and that constructor has at least
   *    one parameter that is annotated with @Assisted.
   * 2) If there is no @Inject constructor and at least one constructor is annotated with
   *    @AssistedInject.
   */
  private Matcher<ClassTree> assistedMatcher = new Matcher<ClassTree>() {
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      MultiMatcher<ClassTree, MethodTree> constructorWithInjectMatcher =
          constructor(ANY, anyOf(hasAnnotation(GUICE_INJECT_ANNOTATION, MethodTree.class),
              hasAnnotation(JAVAX_INJECT_ANNOTATION)));

      if (constructorWithInjectMatcher.matches(classTree, state)) {
        // Check constructor with @Inject annotation for parameter with @Assisted annotation.
        return methodHasParameters(ANY,
            hasAnnotation(ASSISTED_ANNOTATION, VariableTree.class))
            .matches(constructorWithInjectMatcher.getMatchingNode(), state);
      }

      return constructor(ANY, hasAnnotation(ASSISTED_INJECT_ANNOTATION, MethodTree.class))
          .matches(classTree, state);
    }
  };

  @Override
  @SuppressWarnings("unchecked")
  public final boolean matches(ClassTree classTree, VisitorState state) {
    return Matchers.allOf(classAnnotationMatcher, assistedMatcher)
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
