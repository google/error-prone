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
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_QUALIFIER_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "OverlappingQualifierAndScopeAnnotation",
  summary =
      "Annotations cannot be both Scope annotations and Qualifier annotations: this causes "
          + "confusion when trying to use them.",
  category = INJECT,
  severity = ERROR
)
public class OverlappingQualifierAndScopeAnnotation extends BugChecker implements ClassTreeMatcher {
  private static final Matcher<ClassTree> ANNOTATION_WITH_BOTH_TYPES =
      allOf(
          kindIs(ANNOTATION_TYPE),
          anyOf(hasAnnotation(GUICE_BINDING_ANNOTATION), hasAnnotation(JAVAX_QUALIFIER_ANNOTATION)),
          anyOf(hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION)));

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    return ANNOTATION_WITH_BOTH_TYPES.matches(classTree, state)
        ? describeMatch(classTree)
        : Description.NO_MATCH;
  }
}
