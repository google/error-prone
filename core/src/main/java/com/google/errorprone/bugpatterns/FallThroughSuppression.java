/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;

import java.util.List;

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.OFF;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author pepstein@google.com (Peter Epstein)
 */
@BugPattern(name = "FallthroughSuppression",
    summary = "Fallthrough warning suppression has no effect if warning is suppressed",
    explanation =
        "Remove all arguments to @SuppressWarnings annotations that suppress the Java " +
        "compiler's fallthrough warning. If there are no more arguments in a " +
        "@SuppressWarnings annotation, remove the whole annotation.\n\n" +
        "Note: This checker was specific to a refactoring we performed and should not be " +
        "used as a general error or warning.",
    category = ONE_OFF, severity = OFF, maturity = EXPERIMENTAL)
public class FallThroughSuppression extends AbstractSuppressWarningsMatcher {

  @SuppressWarnings({"varargs", "unchecked"})
  private static final Matcher<AnnotationTree> matcher = allOf(
      isType("java.lang.SuppressWarnings"),
      hasArgumentWithValue("value", stringLiteral("fallthrough")));

  @Override
  public final boolean matches(AnnotationTree annotationTree, VisitorState state) {
    return matcher.matches(annotationTree, state);
  }
  
  @Override
  protected void processSuppressWarningsValues(List<String> values) {
    values.remove("fallthrough");
  }
  
  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<AnnotationTree> annotationMatcher = new FallThroughSuppression();

    @Override
    public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
      evaluateMatch(annotationTree, visitorState, annotationMatcher);
      return super.visitAnnotation(annotationTree, visitorState);
    }
  }
}
