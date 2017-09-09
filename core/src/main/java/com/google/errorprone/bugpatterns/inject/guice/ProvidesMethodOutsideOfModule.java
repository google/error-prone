/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_PROVIDES_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.INSIDE_GUICE_MODULE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
  name = "ProvidesMethodOutsideOfModule",
  summary = "@Provides methods need to be declared in a Module to have any effect.",
  explanation =
      "Guice `@Provides` methods annotate methods that are used as a means of declaring"
          + " bindings. However, this is only helpful inside of a module. Methods outside of these"
          + " modules are not used for binding declaration.",
  category = GUICE,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ProvidesMethodOutsideOfModule extends BugChecker implements AnnotationTreeMatcher {

  private static final Matcher<AnnotationTree> PROVIDES_ANNOTATION_ON_METHOD_OUTSIDE_OF_MODULE =
      allOf(isType(GUICE_PROVIDES_ANNOTATION), not(INSIDE_GUICE_MODULE));

  @Override
  public Description matchAnnotation(AnnotationTree annotation, VisitorState state) {
    if (PROVIDES_ANNOTATION_ON_METHOD_OUTSIDE_OF_MODULE.matches(annotation, state)) {
      return describeMatch(annotation, SuggestedFix.delete(annotation));
    }
    return Description.NO_MATCH;
  }
}
