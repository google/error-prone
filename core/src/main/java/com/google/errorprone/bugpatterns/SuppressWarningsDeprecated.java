/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.stringLiteral;
import static java.util.stream.Collectors.toList;

import com.google.auto.common.AnnotationMirrors;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;

/**
 * Find uses of SuppressWarnings with "deprecated".
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(
    name = "SuppressWarningsDeprecated",
    summary = "Suppressing \"deprecated\" is probably a typo for \"deprecation\"",
    severity = ERROR)
public class SuppressWarningsDeprecated extends BugChecker implements AnnotationTreeMatcher {

  private static final Matcher<AnnotationTree> matcher =
      allOf(
          isType("java.lang.SuppressWarnings"),
          hasArgumentWithValue("value", stringLiteral("deprecated")));

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (!matcher.matches(annotationTree, state)) {
      return Description.NO_MATCH;
    }

    AnnotationMirror mirror = ASTHelpers.getAnnotationMirror(annotationTree);
    List<String> values =
        MoreAnnotations.asStrings(AnnotationMirrors.getAnnotationValue(mirror, "value"))
            .map(v -> v.equals("deprecated") ? "deprecation" : v)
            .map(state::getConstantExpression)
            .collect(toList());

    return describeMatch(
        annotationTree,
        SuggestedFixes.updateAnnotationArgumentValues(annotationTree, "value", values, state)
            .build());
  }
}
