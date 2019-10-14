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
package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.time.NearbyCallers.containsCallToSameReceiverNearby;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.LocalDateTime;

/**
 * This checker warns about calls to {@link LocalDateTime#getNano} without a corresponding "nearby"
 * call to {@link LocalDateTime#getSecond}.
 */
@BugPattern(
    name = "JavaLocalDateTimeGetNano",
    summary =
        "localDateTime.getNano() only accesss the nanos-of-second field."
            + " It's rare to only use getNano() without a nearby getSecond() call.",
    severity = WARNING)
public final class JavaLocalDateTimeGetNano extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_SECOND =
      instanceMethod().onExactClass("java.time.LocalDateTime").named("getSecond");
  private static final Matcher<ExpressionTree> GET_NANO =
      allOf(
          instanceMethod().onExactClass("java.time.LocalDateTime").named("getNano"),
          not(packageStartsWith("java.")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (GET_NANO.matches(tree, state)) {
      if (!containsCallToSameReceiverNearby(
          tree, GET_SECOND, state, /* checkProtoChains= */ false)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
