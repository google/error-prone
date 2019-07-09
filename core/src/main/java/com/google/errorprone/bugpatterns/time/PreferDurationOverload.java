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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** This check suggests the use of {@code java.time.Duration}-based APIs, when available. */
@BugPattern(
    name = "PreferDurationOverload",
    summary =
        "Prefer using java.time.Duration-based APIs when available. Note that this checker does"
            + " not and cannot guarantee that the overloads have equivalent semantics, but that is"
            + " generally the case with overloaded methods.",
    severity = WARNING,
    explanation =
        "APIs that accept a java.time.Duration should be preferred, when available. JodaTime is"
            + " now considered a legacy library, and APIs that require a <long, TimeUnit> pair"
            + " suffer from a number of problems: 1) they may require plumbing 2 parameters"
            + " through various layers of your application; 2) overflows are possible when doing"
            + " any duration math; 3) they lack semantic meaning; 4) decomposing a duration into a"
            + " <long, TimeUnit> is dangerous because of unit mismatch and/or excessive"
            + " truncation.",
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class PreferDurationOverload extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String JAVA_DURATION = "java.time.Duration";
  private static final String JODA_DURATION = "org.joda.time.Duration";

  private static final ImmutableMap<TimeUnit, String> TIMEUNIT_TO_DURATION_FACTORY =
      new ImmutableMap.Builder<TimeUnit, String>()
          .put(NANOSECONDS, "%s.ofNanos(%s)")
          .put(MICROSECONDS, "%s.of(%s, %s)")
          .put(MILLISECONDS, "%s.ofMillis(%s)")
          .put(SECONDS, "%s.ofSeconds(%s)")
          .put(MINUTES, "%s.ofMinutes(%s)")
          .put(HOURS, "%s.ofHours(%s)")
          .put(DAYS, "%s.ofDays(%s)")
          .build();

  private static final Matcher<ExpressionTree> IGNORED_APIS =
      anyOf(
          );

  private static final ImmutableMap<Matcher<ExpressionTree>, TimeUnit>
      JODA_DURATION_FACTORY_MATCHERS =
          new ImmutableMap.Builder<Matcher<ExpressionTree>, TimeUnit>()
              .put(constructor().forClass(JODA_DURATION).withParameters("long"), MILLISECONDS)
              .put(staticMethod().onClass(JODA_DURATION).named("millis"), MILLISECONDS)
              .put(staticMethod().onClass(JODA_DURATION).named("standardSeconds"), SECONDS)
              .put(staticMethod().onClass(JODA_DURATION).named("standardMinutes"), MINUTES)
              .put(staticMethod().onClass(JODA_DURATION).named("standardHours"), HOURS)
              .put(staticMethod().onClass(JODA_DURATION).named("standardDays"), DAYS)
              .build();

  // TODO(kak): Add support for constructors that accept a <long, TimeUnit> or JodaTime Duration

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // we return no match for a set of explicitly ignored APIs
    if (IGNORED_APIS.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> arguments = tree.getArguments();

    // TODO(glorioso): Add support for methods with > 2 parameters. E.g.,
    // foo(String, long, TimeUnit, Frobber) -> foo(String, Duration, Frobber)

    if (isNumericMethodCall(tree, state)) {
      if (hasValidJavaTimeDurationOverload(tree, state)) {
        // we don't know what units to use, but we can still warn the user!
        return buildDescription(tree)
            .setMessage(
                String.format(
                    "If the numeric primitive (%s) represents a Duration, please call"
                        + " %s(Duration) instead.",
                    state.getSourceForNode(arguments.get(0)),
                    state.getSourceForNode(tree.getMethodSelect())))
            .build();
      }
    }

    if (isLongTimeUnitMethodCall(tree, state)) {
      Optional<TimeUnit> optionalTimeUnit = DurationToLongTimeUnit.getTimeUnit(arguments.get(1));
      if (optionalTimeUnit.isPresent()) {
        if (hasValidJavaTimeDurationOverload(tree, state)) {
          String durationFactory = TIMEUNIT_TO_DURATION_FACTORY.get(optionalTimeUnit.get());
          if (durationFactory != null) {
            SuggestedFix.Builder fix = SuggestedFix.builder();
            String qualifiedDuration = SuggestedFixes.qualifyType(state, fix, JAVA_DURATION);
            String value = state.getSourceForNode(arguments.get(0));
            String replacement;

            // TODO(kak): Add support for:
            //   foo(javaDuration.getSeconds(), SECONDS);
            //   foo(javaDuration.toMillis(), MILLISECONDS);

            if (optionalTimeUnit.get() == MICROSECONDS) {
              String qualifiedChronoUnit =
                  SuggestedFixes.qualifyType(state, fix, "java.time.temporal.ChronoUnit");
              replacement =
                  String.format(
                      durationFactory, qualifiedDuration, value, qualifiedChronoUnit + ".MICROS");
            } else {
              replacement = String.format(durationFactory, qualifiedDuration, value);
            }

            fix.replace(
                ((JCTree) arguments.get(0)).getStartPosition(),
                state.getEndPosition(arguments.get(1)),
                replacement);
            return describeMatch(tree, fix.build());
          }
        }
      }
    }

    if (isJodaDurationMethodCall(tree, state)) {
      ExpressionTree arg0 = arguments.get(0);
      if (hasValidJavaTimeDurationOverload(tree, state)) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        // TODO(kak): Maybe only emit a match if Duration doesn't have to be fully qualified?
        String qualifiedDuration = SuggestedFixes.qualifyType(state, fix, JAVA_DURATION);

        // TODO(kak): Add support for org.joda.time.Duration.ZERO -> java.time.Duration.ZERO

        // If the Joda Duration is being constructed inline, then unwrap it.
        for (Entry<Matcher<ExpressionTree>, TimeUnit> entry :
            JODA_DURATION_FACTORY_MATCHERS.entrySet()) {
          if (entry.getKey().matches(arg0, state)) {
            if (arg0 instanceof MethodInvocationTree) {
              MethodInvocationTree jodaDurationCreation = (MethodInvocationTree) arg0;
              String value = state.getSourceForNode(jodaDurationCreation.getArguments().get(0));

              String durationFactory = TIMEUNIT_TO_DURATION_FACTORY.get(entry.getValue());
              if (durationFactory != null) {
                String replacement = String.format(durationFactory, qualifiedDuration, value);

                fix.replace(arg0, replacement);
                return describeMatch(tree, fix.build());
              }
            }
          }
        }

        // We could suggest using JavaTimeConversions.toJavaDuration(jodaDuration), but that
        // requires an additional dependency and isn't open-sourced.
        fix.replace(
            arguments.get(0),
            String.format(
                "%s.ofMillis(%s.getMillis())",
                qualifiedDuration, state.getSourceForNode(arguments.get(0))));
        return describeMatch(tree, fix.build());
      }
    }

    return Description.NO_MATCH;
  }

  private static boolean isNumericMethodCall(MethodInvocationTree tree, VisitorState state) {
    List<VarSymbol> params = getSymbol(tree).getParameters();
    if (params.size() == 1) {
      Type type0 = params.get(0).asType();
      return isSameType(type0, state.getSymtab().intType, state)
          || isSameType(type0, state.getSymtab().longType, state)
          || isSameType(type0, state.getSymtab().doubleType, state);
    }
    return false;
  }

  private static boolean isJodaDurationMethodCall(MethodInvocationTree tree, VisitorState state) {
    Type jodaDurationType = state.getTypeFromString("org.joda.time.ReadableDuration");
    List<VarSymbol> params = getSymbol(tree).getParameters();
    if (params.size() == 1) {
      return isSubtype(params.get(0).asType(), jodaDurationType, state);
    }
    return false;
  }

  private static boolean isLongTimeUnitMethodCall(MethodInvocationTree tree, VisitorState state) {
    Type longType = state.getSymtab().longType;
    Type timeUnitType = state.getTypeFromString("java.util.concurrent.TimeUnit");
    List<VarSymbol> params = getSymbol(tree).getParameters();
    if (params.size() == 2) {
      return isSameType(params.get(0).asType(), longType, state)
          && isSameType(params.get(1).asType(), timeUnitType, state);
    }
    return false;
  }

  private static boolean hasValidJavaTimeDurationOverload(
      MethodInvocationTree tree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(tree);
    if (methodSymbol == null) {
      return false;
    }
    Type durationType = state.getTypeFromString(JAVA_DURATION);
    Set<MethodSymbol> durationOverloads =
        ASTHelpers.findMatchingMethods(
            methodSymbol.name,
            input ->
                !input.equals(methodSymbol)
                    // TODO(kak): Do we want to check return types too?
                    && input.isStatic() == methodSymbol.isStatic()
                    && input.getParameters().size() == 1
                    && isSameType(input.getParameters().get(0).asType(), durationType, state),
            ASTHelpers.enclosingClass(methodSymbol).asType(),
            state.getTypes());
    if (durationOverloads.isEmpty()) {
      return false;
    }

    // If we found an overload, make sure we're not currently *inside* that overload, to avoid
    // creating an infinite loop.  There *should* only be one, but just in case, we'll check each
    // overload against the outer method.
    MethodTree t = state.findEnclosing(MethodTree.class);
    if (t == null) {
      return true;
    }
    return durationOverloads.stream().noneMatch(getSymbol(t)::equals);
  }
}
