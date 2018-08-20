/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.booleanLiteral;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;

/** A checker for injected constructors with @Inject(optional=true) or binding annotations. */
@BugPattern(
    name = "InjectedConstructorAnnotations",
    summary = "Injected constructors cannot be optional nor have binding annotations",
    category = INJECT,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class InjectedConstructorAnnotations extends BugChecker implements MethodTreeMatcher {

  // A matcher of @Inject{optional=true}
  private static final Matcher<AnnotationTree> OPTIONAL_INJECTION_MATCHER =
      allOf(
          isType(GUICE_INJECT_ANNOTATION), hasArgumentWithValue("optional", booleanLiteral(true)));

  // A matcher of binding annotations
  private static final Matcher<AnnotationTree> BINDING_ANNOTATION_MATCHER =
      new Matcher<AnnotationTree>() {
        @Override
        public boolean matches(AnnotationTree annotationTree, VisitorState state) {
          return symbolHasAnnotation(GUICE_BINDING_ANNOTATION)
              .matches(annotationTree.getAnnotationType(), state);
        }
      };

  /**
   * Matches injected constructors annotated with @Inject(optional=true) or binding annotations.
   * Suggests fixes to remove the argument {@code optional=true} or binding annotations.
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    SuggestedFix.Builder fix = null;
    if (isInjectedConstructor(methodTree, state)) {
      for (AnnotationTree annotationTree : methodTree.getModifiers().getAnnotations()) {
        if (OPTIONAL_INJECTION_MATCHER.matches(annotationTree, state)) {
          // Replace the annotation with "@Inject"
          if (fix == null) {
            fix = SuggestedFix.builder();
          }
          fix = fix.replace(annotationTree, "@Inject");
        } else if (BINDING_ANNOTATION_MATCHER.matches(annotationTree, state)) {
          // Remove the binding annotation
          if (fix == null) {
            fix = SuggestedFix.builder();
          }
          fix = fix.delete(annotationTree);
        }
      }
    }
    if (fix == null) {
      return Description.NO_MATCH;
    } else {
      return describeMatch(methodTree, fix.build());
    }
  }

  private boolean isInjectedConstructor(MethodTree methodTree, VisitorState state) {
    return allOf(methodIsConstructor(), hasAnnotation(GUICE_INJECT_ANNOTATION))
        .matches(methodTree, state);
  }
}
