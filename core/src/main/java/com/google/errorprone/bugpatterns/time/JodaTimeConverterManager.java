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

import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** Ban usage of Joda's {@code ConverterManager}. */
@BugPattern(
    name = "JodaTimeConverterManager",
    summary =
        "Joda-Time's ConverterManager makes the semantics of DateTime/Instant/etc construction"
            + " subject to global static state. If you need to define your own converters, use"
            + " a helper.",
    severity = WARNING,
    providesFix = NO_FIX)
public final class JodaTimeConverterManager extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          staticMethod().onClass("org.joda.time.convert.ConverterManager").named("getInstance"),
          // Allow usage by JodaTime itself.
          not(Matchers.packageStartsWith("org.joda.time")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
