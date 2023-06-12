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
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * This checker warns about calls to {@code duration.getNano()} without a corresponding "nearby"
 * call to {@code duration.getSeconds()}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    summary =
        "duration.getNano() only accesses the underlying nanosecond adjustment from the whole "
            + "second.",
    explanation =
        "If you call duration.getNano(), you must also call duration.getSeconds() in 'nearby' code."
            + " If you are trying to convert this duration to nanoseconds, you probably meant to"
            + " use duration.toNanos() instead.",
    severity = WARNING)
public final class JavaDurationGetSecondsGetNano extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_SECONDS =
      instanceMethod().onExactClass("java.time.Duration").named("getSeconds");
  private static final Matcher<ExpressionTree> GET_NANO =
      allOf(
          instanceMethod().onExactClass("java.time.Duration").named("getNano"),
          Matchers.not(Matchers.packageStartsWith("java.")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (GET_NANO.matches(tree, state)) {
      if (!containsCallToSameReceiverNearby(
          tree, GET_SECONDS, state, /* checkProtoChains= */ false)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
