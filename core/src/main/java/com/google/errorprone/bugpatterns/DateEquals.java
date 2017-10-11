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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.predicates.TypePredicates;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/** @author thomas.schwery@sai-erp.net (Thomas Schwery) */
@BugPattern(
  name = "DateEquals",
  summary = "Equals used to compare dates",
  explanation =
      "Date comparison using equals can cause equality mismatch when comparing java.util.Date"
              + "with its descendants, even though both objects represents the same date."
              + "It is thus preferable to use the instance compareTo to check for"
              + "period equality.",
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class DateEquals extends BugChecker implements MethodInvocationTreeMatcher {
  /** Matches when the equals instance method is used to compare two Date objects. */
  private static final Matcher<MethodInvocationTree> instanceEqualsMatcher =
      Matchers.allOf(
          instanceMethod().onClass(TypePredicates.isDescendantOf("java.util.Date")).named("equals"),
          argument(0, Matchers.<ExpressionTree>isSubtypeOf("java.util.Date")));

  /**
   * Matches when the Guava com.google.common.base.Objects#equal or the JDK7
   * java.util.Objects#equals method is used to compare two Date objects.
   */
  private static final Matcher<MethodInvocationTree> staticEqualsMatcher =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Objects").named("equal"),
              staticMethod().onClass("java.util.Objects").named("equals")),
          argument(0, Matchers.<ExpressionTree>isSubtypeOf("java.util.Date")),
          argument(1, Matchers.<ExpressionTree>isSubtypeOf("java.util.Date")));

  /**
   * Suggests replacing with compareTo.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    String arg1;
    String arg2;
    if (instanceEqualsMatcher.matches(t, state)) {
      arg1 = ((JCFieldAccess) t.getMethodSelect()).getExpression().toString();
      arg2 = t.getArguments().get(0).toString();
    } else if (staticEqualsMatcher.matches(t, state)) {
      arg1 = t.getArguments().get(0).toString();
      arg2 = t.getArguments().get(1).toString();
    } else {
      return NO_MATCH;
    }

    Fix fix =
        SuggestedFix.builder()
            .replace(t, "" + arg1 + ".compareTo(" + arg2 + ") == 0")
            .build();
    return describeMatch(t, fix);
  }
}
