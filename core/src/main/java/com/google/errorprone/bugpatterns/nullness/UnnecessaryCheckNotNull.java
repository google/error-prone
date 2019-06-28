/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/**
 * Checks for unnecessarily performing null checks on expression which create a new class/array.
 *
 * @author awturner@google.com (Andy Turner)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "UnnecessaryCheckNotNull",
    summary =
        "By specification, creating instances with 'new' cannot return a null value, so invoking"
            + " null check methods is redundant",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class UnnecessaryCheckNotNull extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> CHECK_NOT_NULL_MATCHER =
      allOf(
          Matchers.<MethodInvocationTree>anyOf(
              staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
              staticMethod().onClass("com.google.common.base.Verify").named("verifyNotNull"),
              staticMethod().onClass("java.util.Objects").named("requireNonNull")),
          argument(
              0,
              Matchers.<ExpressionTree>kindAnyOf(ImmutableSet.of(Kind.NEW_CLASS, Kind.NEW_ARRAY))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CHECK_NOT_NULL_MATCHER.matches(tree, state) || tree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    Fix fix = SuggestedFix.replace(tree, state.getSourceForNode(tree.getArguments().get(0)));
    return describeMatch(tree, fix);
  }
}
