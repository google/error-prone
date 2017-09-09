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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_QUALIFIER_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import java.util.List;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "InjectMoreThanOneQualifier",
  summary = "Using more than one qualifier annotation on the same element is not allowed.",
  explanation = "An element can be qualified by at most one qualifier.",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class MoreThanOneQualifier extends BugChecker implements AnnotationTreeMatcher {

  private static final Matcher<AnnotationTree> QUALIFIER_ANNOTATION_MATCHER =
      Matchers.anyOf(
          hasAnnotation(GUICE_BINDING_ANNOTATION), hasAnnotation(JAVAX_QUALIFIER_ANNOTATION));

  @Override
  public Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    int numberOfQualifiers = 0;
    if (QUALIFIER_ANNOTATION_MATCHER.matches(annotationTree, state)) {
      for (AnnotationTree t : getSiblingAnnotations(state)) {
        if (QUALIFIER_ANNOTATION_MATCHER.matches(t, state)) {
          numberOfQualifiers++;
        }
      }
    }
    if (numberOfQualifiers > 1) {
      return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
    }
    return Description.NO_MATCH;
  }

  private List<? extends AnnotationTree> getSiblingAnnotations(VisitorState state) {
    return ((ModifiersTree) state.getPath().getParentPath().getLeaf()).getAnnotations();
  }
}
