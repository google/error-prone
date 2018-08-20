/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Objects;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "CanonicalDuration",
    summary = "Duration can be expressed more clearly with different units",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public class CanonicalDuration extends BugChecker implements MethodInvocationTreeMatcher {

  enum Api {
    JAVA("java.time.Duration"),
    JODA("org.joda.time.Duration");

    private final String durationFullyQualifiedName;

    private Api(String durationFullyQualifiedName) {
      this.durationFullyQualifiedName = durationFullyQualifiedName;
    }

    String getDurationFullyQualifiedName() {
      return durationFullyQualifiedName;
    }
  }

  private static final Matcher<ExpressionTree> JAVA_TIME_MATCHER =
      staticMethod().onClass(Api.JAVA.getDurationFullyQualifiedName());

  private static final Matcher<ExpressionTree> JODA_MATCHER =
      staticMethod().onClass(Api.JODA.getDurationFullyQualifiedName());

  private static final ImmutableTable<Api, ChronoUnit, String> FACTORIES =
      ImmutableTable.<Api, ChronoUnit, String>builder()
          .put(Api.JAVA, ChronoUnit.DAYS, "ofDays")
          .put(Api.JAVA, ChronoUnit.HOURS, "ofHours")
          .put(Api.JAVA, ChronoUnit.MINUTES, "ofMinutes")
          .put(Api.JAVA, ChronoUnit.SECONDS, "ofSeconds")
          .put(Api.JAVA, ChronoUnit.MILLIS, "ofMillis")
          .put(Api.JAVA, ChronoUnit.NANOS, "ofNanos")
          .put(Api.JODA, ChronoUnit.DAYS, "standardDays")
          .put(Api.JODA, ChronoUnit.HOURS, "standardHours")
          .put(Api.JODA, ChronoUnit.MINUTES, "standardMinutes")
          .put(Api.JODA, ChronoUnit.SECONDS, "standardSeconds")
          .build();

  private static final ImmutableMap<String, TemporalUnit> METHOD_NAME_TO_UNIT =
      FACTORIES.rowMap().values().stream()
          .flatMap(x -> x.entrySet().stream())
          .collect(toImmutableMap(x -> x.getValue(), x -> x.getKey()));

  private static final ImmutableMap<ChronoUnit, Converter<Duration, Long>> CONVERTERS =
      ImmutableMap.<ChronoUnit, Converter<Duration, Long>>builder()
          .put(ChronoUnit.DAYS, Converter.from(Duration::toDays, Duration::ofDays))
          .put(ChronoUnit.HOURS, Converter.from(Duration::toHours, Duration::ofHours))
          .put(ChronoUnit.MINUTES, Converter.from(Duration::toMinutes, Duration::ofMinutes))
          .put(ChronoUnit.SECONDS, Converter.from(Duration::getSeconds, Duration::ofSeconds))
          .put(ChronoUnit.MILLIS, Converter.from(Duration::toMillis, Duration::ofMillis))
          .put(ChronoUnit.NANOS, Converter.from(Duration::toNanos, Duration::ofNanos))
          .build();

  // Represent a single day/hour/minute as hours/minutes/seconds is sometimes used to allow a block
  // of durations to have consistent units.
  private static final ImmutableMap<TemporalUnit, Long> BLACKLIST =
      ImmutableMap.of(
          ChronoUnit.HOURS, 24L,
          ChronoUnit.MINUTES, 60L,
          ChronoUnit.SECONDS, 60L);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Api api;
    if (JAVA_TIME_MATCHER.matches(tree, state)) {
      api = Api.JAVA;
    } else if (JODA_MATCHER.matches(tree, state)) {
      api = Api.JODA;
    } else {
      return NO_MATCH;
    }
    if (tree.getArguments().size() != 1) {
      // TODO(cushon): ofSeconds w/ nano adjustment?
      return NO_MATCH;
    }
    Tree arg = getOnlyElement(tree.getArguments());
    if (!(arg instanceof LiteralTree)) {
      // don't inline constants
      return NO_MATCH;
    }
    Number value = constValue(arg, Number.class);
    if (value == null) {
      return NO_MATCH;
    }
    if (value.intValue() == 0) {
      switch (api) {
        case JODA:
          ExpressionTree receiver = getReceiver(tree);
          SuggestedFix fix;
          if (receiver == null) { // static import of the method
            fix =
                SuggestedFix.builder()
                    .addImport(api.getDurationFullyQualifiedName())
                    .replace(tree, "Duration.ZERO")
                    .build();
          } else {
            fix =
                SuggestedFix.replace(
                    state.getEndPosition(getReceiver(tree)), state.getEndPosition(tree), ".ZERO");
          }
          return buildDescription(tree)
              .setMessage("Duration can be expressed more clearly without units, as Duration.ZERO")
              .addFix(fix)
              .build();
        case JAVA:
          // don't rewrite e.g. `ofMillis(0)` to `ofDays(0)`
          return NO_MATCH;
      }
      throw new AssertionError(api);
    }
    MethodSymbol sym = getSymbol(tree);
    if (!METHOD_NAME_TO_UNIT.containsKey(sym.getSimpleName().toString())) {
      return NO_MATCH;
    }
    TemporalUnit unit = METHOD_NAME_TO_UNIT.get(sym.getSimpleName().toString());
    if (Objects.equals(BLACKLIST.get(unit), value.longValue())) {
      return NO_MATCH;
    }
    Duration duration = Duration.of(value.longValue(), unit);
    // Iterate over all possible units from largest to smallest (days to nanos) until we find the
    // largest unit that can be used to exactly express the duration.
    for (Map.Entry<ChronoUnit, Converter<Duration, Long>> entry : CONVERTERS.entrySet()) {
      ChronoUnit nextUnit = entry.getKey();
      if (unit.equals(nextUnit)) {
        // We reached the original unit, no simplification is possible.
        break;
      }
      Converter<Duration, Long> converter = entry.getValue();
      long nextValue = converter.convert(duration);
      if (converter.reverse().convert(nextValue).equals(duration)) {
        // We reached a larger than original unit that precisely expresses the duration, rewrite to
        // use it instead.
        String name = FACTORIES.get(api, nextUnit);
        String replacement =
            String.format("%s(%d%s)", name, nextValue, nextValue == ((int) nextValue) ? "" : "L");
        ExpressionTree receiver = getReceiver(tree);
        if (receiver == null) { // static import of the method
          SuggestedFix fix =
              SuggestedFix.builder()
                  .addStaticImport(api.getDurationFullyQualifiedName() + "." + name)
                  .replace(tree, replacement)
                  .build();
          return describeMatch(tree, fix);
        } else {
          return describeMatch(
              tree,
              SuggestedFix.replace(
                  state.getEndPosition(receiver), state.getEndPosition(tree), "." + replacement));
        }
      }
    }
    return NO_MATCH;
  }
}
