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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
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
        "APIs that require a <long, TimeUnit> pair suffer from a number of problems: 1) they may"
            + " require plumbing 2 parameters through various layers of your application; 2)"
            + " overflows are possible when doing any duration math; 3) they lack semantic"
            + " meaning; 4) decomposing a duration into a <long, TimeUnit> is dangerous because of"
            + " unit mismatch and/or excessive truncation.",
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class PreferDurationOverload extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String DURATION = "java.time.Duration";

  private static final ImmutableMap<TimeUnit, String> TIMEUNIT_TO_DURATION_FACTORY =
      new ImmutableMap.Builder<TimeUnit, String>()
          .put(TimeUnit.NANOSECONDS, "%s.ofNanos(%s)")
          // TODO(kak): Do we want to handle MICROSECONDS? We'd need to either use
          // Duration.of(%s, MICROSECONDS) or com.google.common.time.Durations.ofMicros(%s)
          .put(TimeUnit.MILLISECONDS, "%s.ofMillis(%s)")
          .put(TimeUnit.SECONDS, "%s.ofSeconds(%s)")
          .put(TimeUnit.MINUTES, "%s.ofMinutes(%s)")
          .put(TimeUnit.HOURS, "%s.ofHours(%s)")
          .put(TimeUnit.DAYS, "%s.ofDays(%s)")
          .build();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // TODO(kak): Add support for methods with > 2 parameters. E.g.,
    // foo(String, long, TimeUnit, Frobber) -> foo(String, Duration, Frobber)
    if (isLongTimeUnitMethod(tree, state)) {
      List<? extends ExpressionTree> arguments = tree.getArguments();
      Optional<TimeUnit> optionalTimeUnit = DurationToLongTimeUnit.getTimeUnit(arguments.get(1));
      if (optionalTimeUnit.isPresent()) {
        if (durationOverloadExists(tree, state)) {
          String durationFactory = TIMEUNIT_TO_DURATION_FACTORY.get(optionalTimeUnit.get());
          if (durationFactory != null) {
            SuggestedFix.Builder fix = SuggestedFix.builder();
            String qualifiedType = SuggestedFixes.qualifyType(state, fix, DURATION);
            fix.replace(
                ((JCTree) arguments.get(0)).getStartPosition(),
                state.getEndPosition(arguments.get(1)),
                String.format(
                    durationFactory, qualifiedType, state.getSourceForNode(arguments.get(0))));
            return describeMatch(tree, fix.build());
          }
        }
      }
    }
    // TODO(kak): Add support for Joda Durations. I.e., if (isJodaDurationMethod(tree, state)) { }
    return Description.NO_MATCH;
  }

  private static boolean isLongTimeUnitMethod(MethodInvocationTree tree, VisitorState state) {
    Type longType = state.getSymtab().longType;
    Type timeUnitType = state.getTypeFromString("java.util.concurrent.TimeUnit");
    List<VarSymbol> params = getSymbol(tree).getParameters();
    if (params.size() == 2) {
      return isSameType(params.get(0).asType(), longType, state)
          && isSameType(params.get(1).asType(), timeUnitType, state);
    }
    return false;
  }

  private static boolean durationOverloadExists(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(tree);
    Type durationType = state.getTypeFromString(DURATION);
    return !ASTHelpers.findMatchingMethods(
            methodSymbol.name,
            input ->
                !input.equals(methodSymbol)
                    // TODO(kak): Do we want to check return types too?
                    && input.isStatic() == methodSymbol.isStatic()
                    && input.getParameters().size() == 1
                    && isSameType(input.getParameters().get(0).asType(), durationType, state),
            ASTHelpers.enclosingClass(methodSymbol).asType(),
            state.getTypes())
        .isEmpty();
  }
}
