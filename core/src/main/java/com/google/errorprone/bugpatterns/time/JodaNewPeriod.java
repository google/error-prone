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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorClassMatcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;

/** Match possibly incorrect use of Period to obtain a number of (e.g.) days between two dates. */
@BugPattern(
    name = "JodaNewPeriod",
    summary =
        "This may have surprising semantics, e.g. new Period(LocalDate.parse(\"1970-01-01\"), "
            + "LocalDate.parse(\"1970-02-02\")).getDays() == 1, not 32.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class JodaNewPeriod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final ConstructorClassMatcher PERIOD_CONSTRUCTOR =
      constructor().forClass("org.joda.time.Period");

  private static final String READABLE_PARTIAL = "org.joda.time.ReadablePartial";

  private static final String READABLE_INSTANT = "org.joda.time.ReadableInstant";

  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(
          instanceMethod()
              .onDescendantOf("org.joda.time.Period")
              .namedAnyOf(
                  "getMonths", "getWeeks", "getDays", "getHours", "getMinutes", "getSeconds"),
          receiverOfInvocation(
              anyOf(
                  PERIOD_CONSTRUCTOR.withParameters("long", "long"),
                  PERIOD_CONSTRUCTOR.withParameters(READABLE_PARTIAL, READABLE_PARTIAL),
                  PERIOD_CONSTRUCTOR.withParameters(READABLE_INSTANT, READABLE_INSTANT))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state)
        ? describeMatch(tree, generateFix(tree, state))
        : Description.NO_MATCH;
  }

  private static SuggestedFix generateFix(MethodInvocationTree tree, VisitorState state) {
    NewClassTree receiver = (NewClassTree) getReceiver(tree);
    List<? extends ExpressionTree> arguments = receiver.getArguments();
    MethodSymbol methodSymbol = getSymbol(receiver);
    String unit =
        ((MemberSelectTree) tree.getMethodSelect()).getIdentifier().toString().replace("get", "");
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();

    // new Period(long, long).getUnits() -> new Duration(long, long).getStandardUnits();
    if (isSameType(
        state.getTypes().unboxedTypeOrType(methodSymbol.params().get(0).type),
        state.getSymtab().longType,
        state)) {
      String duration = SuggestedFixes.qualifyType(state, fixBuilder, "org.joda.time.Duration");
      return fixBuilder
          .replace(
              tree,
              String.format(
                  "new %s(%s, %s).getStandard%s()",
                  duration,
                  state.getSourceForNode(arguments.get(0)),
                  state.getSourceForNode(arguments.get(1)),
                  unit))
          .build();
    }

    // new Period(ReadableFoo, ReadableFoo).getUnits() -> Units.unitsBetween(RF, RF).getUnits();
    String unitImport = SuggestedFixes.qualifyType(state, fixBuilder, "org.joda.time." + unit);
    return fixBuilder
        .replace(
            tree,
            String.format(
                "%s.%sBetween(%s, %s).get%s()",
                unitImport,
                unit.toLowerCase(),
                state.getSourceForNode(arguments.get(0)),
                state.getSourceForNode(arguments.get(1)),
                unit))
        .build();
  }
}
