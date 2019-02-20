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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

/** Check for calls to Joda-Time's {@code foo.toFoo()} and {@code new Foo(foo)}. */
@BugPattern(
    name = "JodaToSelf",
    summary =
        "Use of Joda-Time's DateTime.toDateTime(), Duration.toDuration(), Instant.toInstant(), "
            + "Interval.toInterval(), and Period.toPeriod() are not allowed.",
    explanation =
        "Joda-Time's DateTime.toDateTime(), Duration.toDuration(), Instant.toInstant(), "
            + "Interval.toInterval(), and Period.toPeriod() are always unnecessary, since they "
            + "simply 'return this'. There is no reason to ever call them.",
    severity = ERROR,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JodaToSelf extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final ImmutableSet<String> TYPE_NAMES =
      ImmutableSet.of("DateTime", "Duration", "Instant", "Interval", "Period");

  private static final ImmutableSet<String> TYPES_WITHOUT_METHOD =
      ImmutableSet.of("LocalDate", "LocalDateTime", "LocalTime");

  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.allOf(
          Matchers.anyOf(
              TYPE_NAMES
                  .stream()
                  .map(
                      typeName ->
                          Matchers.instanceMethod()
                              .onExactClass("org.joda.time." + typeName)
                              .named("to" + typeName)
                              .withParameters())
                  .collect(toImmutableList())),
          // Allow usage by JodaTime itself
          Matchers.not(Matchers.packageStartsWith("org.joda.time")));

  private static final Matcher<ExpressionTree> CONSTRUCTOR_MATCHER =
      Matchers.allOf(
          Matchers.anyOf(
              Streams.concat(TYPE_NAMES.stream(), TYPES_WITHOUT_METHOD.stream())
                  .map(
                      typeName ->
                          Matchers.constructor()
                              .forClass("org.joda.time." + typeName)
                              .withParameters("java.lang.Object"))
                  .collect(toImmutableList())),
          // Allow usage by JodaTime itself
          Matchers.not(Matchers.packageStartsWith("org.joda.time")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format("Use of %s is a no-op and is not allowed.", ASTHelpers.getSymbol(tree)))
        .addFix(
            SuggestedFix.replace(
                state.getEndPosition(ASTHelpers.getReceiver(tree)), state.getEndPosition(tree), ""))
        .build();
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!CONSTRUCTOR_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree argument = getOnlyElement(tree.getArguments());
    if (!ASTHelpers.isSameType(
        ASTHelpers.getType(argument), ASTHelpers.getType(tree.getIdentifier()), state)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format("Use of %s is a no-op and is not allowed.", ASTHelpers.getSymbol(tree)))
        .addFix(SuggestedFix.replace(tree, state.getSourceForNode(argument)))
        .build();
  }
}
