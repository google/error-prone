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
import static com.google.errorprone.bugpatterns.time.JavaDurationGetSecondsGetNano.containsGetSecondsCallInNearbyCode;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * This checker warns about accessing the underlying nanosecond-adjustment field of a duration
 * without a "nearby" access of the underlying seconds field.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "ProtoDurationGetSecondsGetNano",
    summary = "getNanos() only accesses the underlying nanosecond-adjustment of the duration.",
    explanation =
        "If you call duration.getNanos(), you must also call duration.getSeconds() in 'nearby' "
            + "code. If you are trying to convert this duration to nanoseconds, "
            + "you probably meant to use Durations.toNanos(duration) instead.",
    severity = WARNING)
public final class ProtoDurationGetSecondsGetNano extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_SECONDS =
      instanceMethod().onExactClass("com.google.protobuf.Duration").named("getSeconds");
  private static final Matcher<ExpressionTree> GET_NANOS =
      instanceMethod().onExactClass("com.google.protobuf.Duration").named("getNanos");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (GET_NANOS.matches(tree, state)) {
      if (!containsGetSecondsCallInNearbyCode(
          tree, state, GET_SECONDS, /*checkProtoChains=*/ true)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
