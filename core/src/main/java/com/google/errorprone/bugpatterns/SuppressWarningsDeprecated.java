/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.stringLiteral;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import java.util.List;

/**
 * Find uses of SuppressWarnings with "deprecated".
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(
  name = "SuppressWarningsDeprecated",
  summary = "Suppressing \"deprecated\" is probably a typo for \"deprecation\"",
  explanation =
      "To suppress warnings to deprecated methods, you should add the annotation\n"
          + "`@SuppressWarnings(\"deprecation\")`\n"
          + "and not\n"
          + "`@SuppressWarnings(\"deprecated\")`",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class SuppressWarningsDeprecated extends AbstractSuppressWarningsMatcher {

  @SuppressWarnings({"varargs", "unchecked"})
  private static final Matcher<AnnotationTree> matcher =
      allOf(
          isType("java.lang.SuppressWarnings"),
          hasArgumentWithValue("value", stringLiteral("deprecated")));

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (matcher.matches(annotationTree, state)) {
      return describeMatch(annotationTree, getSuggestedFix(annotationTree));
    }
    return Description.NO_MATCH;
  }

  @Override
  protected void processSuppressWarningsValues(List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      if (values.get(i).equals("deprecated")) {
        values.set(i, "deprecation");
      }
    }
  }
}
