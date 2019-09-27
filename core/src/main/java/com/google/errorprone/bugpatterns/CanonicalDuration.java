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
import static java.util.stream.Collectors.toList;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private static final ImmutableMap<TemporalUnit, Long> BANLIST =
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

    List<MethodInvocationTree> allInvocationsInParentExpression =
        getAllInvocationsInParentExpression(state);
    if (allInvocationsInParentExpression.isEmpty()) {
      return NO_MATCH;
    }

    List<Number> constValues =
        allInvocationsInParentExpression.stream()
            .map(t -> getOnlyElement(t.getArguments()))
            .map(arg -> (arg instanceof LiteralTree) ? arg : null)
            .map(arg -> constValue(arg, Number.class))
            .collect(toList());

    if (constValues.stream().anyMatch(Objects::isNull)) {
      return NO_MATCH;
    }

    if (constValues.stream().mapToLong(Number::longValue).allMatch(v -> v == 0L)) {
      return handleAllZeros(state, api, allInvocationsInParentExpression);
    }

    MethodSymbol sym = getSymbol(tree);
    if (!METHOD_NAME_TO_UNIT.containsKey(sym.getSimpleName().toString())) {
      return NO_MATCH;
    }
    TemporalUnit unit = METHOD_NAME_TO_UNIT.get(sym.getSimpleName().toString());
    Long banListValue = BANLIST.get(unit);
    if (banListValue != null
        && constValues.stream()
            .anyMatch(value -> Objects.equals(banListValue, value.longValue()))) {
      return NO_MATCH;
    }

    List<Duration> durations =
        constValues.stream().map(value -> Duration.of(value.longValue(), unit)).collect(toList());
    // Iterate over all possible units from largest to smallest (days to nanos) until we find the
    // largest unit that can be used to exactly express the duration.
    for (Map.Entry<ChronoUnit, Converter<Duration, Long>> entry : CONVERTERS.entrySet()) {
      ChronoUnit nextUnit = entry.getKey();
      if (unit.equals(nextUnit)) {
        // We reached the original unit, no simplification is possible.
        break;
      }
      Converter<Duration, Long> converter = entry.getValue();

      List<Duration> roundTripped =
          durations.stream()
              .map(converter::convert)
              .map(converter.reverse()::convert)
              .collect(toList());

      if (roundTripped.equals(durations)) {
        // We reached a larger than original unit that precisely expresses the duration, rewrite to
        // use it instead.
        for (int i = 0; i < allInvocationsInParentExpression.size(); i++) {
          MethodInvocationTree m = allInvocationsInParentExpression.get(i);
          long nextValue = converter.convert(durations.get(i));
          String name = FACTORIES.get(api, nextUnit);
          String replacement =
              String.format("%s(%d%s)", name, nextValue, nextValue == ((int) nextValue) ? "" : "L");
          ExpressionTree receiver = getReceiver(m);
          if (receiver == null) { // static import of the method
            SuggestedFix fix =
                SuggestedFix.builder()
                    .addStaticImport(api.getDurationFullyQualifiedName() + "." + name)
                    .replace(m, replacement)
                    .build();
            state.reportMatch(describeMatch(m, fix));
          } else {
            state.reportMatch(
                describeMatch(
                    m,
                    SuggestedFix.replace(
                        state.getEndPosition(receiver),
                        state.getEndPosition(m),
                        "." + replacement)));
          }
        }
        return Description.NO_MATCH;
      }
    }
    return NO_MATCH;
  }

  private Description handleAllZeros(
      VisitorState state, Api api, List<MethodInvocationTree> allInvocationsInParentExpression) {
    switch (api) {
      case JODA:
        for (MethodInvocationTree tree : allInvocationsInParentExpression) {
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
          state.reportMatch(
              buildDescription(tree)
                  .setMessage(
                      "Duration can be expressed more clearly without units, as Duration.ZERO")
                  .addFix(fix)
                  .build());
        }
        return NO_MATCH;
      case JAVA:
        // don't rewrite e.g. `ofMillis(0)` to `ofDays(0)`
        return NO_MATCH;
    }
    throw new AssertionError(api);
  }

  private static List<MethodInvocationTree> getAllInvocationsInParentExpression(
      VisitorState state) {
    // Walk up the tree path until the parent tree is no longer an expression.
    TreePath expressionPath = state.getPath();
    while (true) {
      TreePath parentPath = expressionPath.getParentPath();
      if (parentPath == null) {
        break;
      }
      if (!(expressionPath.getParentPath().getLeaf() instanceof ExpressionTree)) {
        break;
      }
      expressionPath = parentPath;
    }

    // This check gets run on all invocations of the duration methods, but we propose fixes for
    // all things in the same expression. As such, we detect if we are the first invocation of the
    // method, and if not, we don't check any further, because all trees will be checked when the
    // first invocation is checked.
    AtomicBoolean notFirst = new AtomicBoolean();

    // Scan the tree for invocations of the same method.
    MethodInvocationTree tree = (MethodInvocationTree) state.getPath().getLeaf();
    MethodSymbol methodSymbol = getSymbol(tree);
    List<MethodInvocationTree> sameMethodInvocations = new ArrayList<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void scan(Tree node, Void aVoid) {
        if (notFirst.get()) {
          return null;
        }
        return super.scan(node, aVoid);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
        if (Objects.equals(methodSymbol, ASTHelpers.getSymbol(node))) {
          if (sameMethodInvocations.isEmpty()) {
            if (!Objects.equals(node, tree)) {
              notFirst.set(true);
              return null;
            }
          }

          sameMethodInvocations.add(node);
          return super.visitMethodInvocation(node, aVoid);
        }
        return super.visitMethodInvocation(node, aVoid);
      }
    }.scan(expressionPath.getLeaf(), null);

    if (notFirst.get()) {
      sameMethodInvocations.clear();
    }
    return sameMethodInvocations;
  }
}
