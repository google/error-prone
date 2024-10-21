/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.time.NearbyCallers.containsCallToSameReceiverNearby;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** Error prone checking for {@code Duration.getSeconds()} without {@code Duration.getNano()}. */
@BugPattern(
    summary = "Prefer duration.toSeconds() over duration.getSeconds()",
    explanation =
        "duration.getSeconds() is a decomposition API which should always be used alongside"
            + " duration.getNano(). duration.toSeconds() is a conversion API, and the preferred way"
            + " to convert to seconds.",
    severity = WARNING)
public final class JavaDurationGetSecondsToToSeconds extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_SECONDS =
      instanceMethod().onExactClass("java.time.Duration").named("getSeconds");
  private static final Matcher<ExpressionTree> GET_NANO =
      allOf(
          instanceMethod().onExactClass("java.time.Duration").named("getNano"),
          Matchers.not(Matchers.packageStartsWith("java.")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (GET_SECONDS.matches(tree, state)) {
      if (!containsCallToSameReceiverNearby(tree, GET_NANO, state, /* checkProtoChains= */ false)) {
        return describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "toSeconds", state));
      }
    }
    return Description.NO_MATCH;
  }
}
