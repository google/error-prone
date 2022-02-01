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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Name;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.HijrahEra;
import java.time.chrono.IsoEra;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.MinguoEra;
import java.time.chrono.ThaiBuddhistDate;
import java.time.chrono.ThaiBuddhistEra;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;

/**
 * Bans calls to {@code TemporalAccessor.get(ChronoField)} where the implementation is guaranteed to
 * throw an {@code UnsupportedTemporalTypeException}.
 */
@BugPattern(
    summary = "TemporalAccessor.get() only works for certain values of ChronoField.",
    explanation =
        "TemporalAccessor.get(ChronoField) only works for certain values of ChronoField. E.g., "
            + "DayOfWeek only supports DAY_OF_WEEK. All other values are guaranteed to throw an "
            + "UnsupportedTemporalTypeException.",
    severity = ERROR)
public final class TemporalAccessorGetChronoField extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final ZoneId ARBITRARY_ZONE = ZoneId.of("America/Los_Angeles");

  private static final ImmutableList<TemporalAccessor> TEMPORAL_ACCESSOR_INSTANCES =
      ImmutableList.of(
          DayOfWeek.MONDAY,
          HijrahDate.now(ARBITRARY_ZONE),
          HijrahEra.AH,
          Instant.now(),
          IsoEra.CE,
          JapaneseDate.now(ARBITRARY_ZONE),
          JapaneseEra.SHOWA,
          LocalDate.now(ARBITRARY_ZONE),
          LocalDateTime.now(ARBITRARY_ZONE),
          LocalTime.now(ARBITRARY_ZONE),
          MinguoDate.now(ARBITRARY_ZONE),
          MinguoEra.ROC,
          Month.MAY,
          MonthDay.now(ARBITRARY_ZONE),
          OffsetDateTime.now(ARBITRARY_ZONE),
          OffsetTime.now(ARBITRARY_ZONE),
          ThaiBuddhistDate.now(ARBITRARY_ZONE),
          ThaiBuddhistEra.BE,
          Year.now(ARBITRARY_ZONE),
          YearMonth.now(ARBITRARY_ZONE),
          ZonedDateTime.now(ARBITRARY_ZONE),
          ZoneOffset.ofHours(8));

  private static final ImmutableListMultimap<String, ChronoField> UNSUPPORTED = buildUnsupported();

  private static ImmutableListMultimap<String, ChronoField> buildUnsupported() {
    ImmutableListMultimap.Builder<String, ChronoField> builder = ImmutableListMultimap.builder();
    for (TemporalAccessor temporalAccessor : TEMPORAL_ACCESSOR_INSTANCES) {
      for (ChronoField chronoField : ChronoField.values()) {
        if (!temporalAccessor.isSupported(chronoField)) {
          builder.put(temporalAccessor.getClass().getCanonicalName(), chronoField);
        }
      }
    }
    return builder.build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    Name methodName = sym.name;
    if (!(methodName.contentEquals("get") || methodName.contentEquals("getLong"))) {
      return Description.NO_MATCH;
    }

    List<VarSymbol> params = sym.params();
    if (params.size() != 1) {
      return Description.NO_MATCH;
    }
    Name argType = params.get(0).type.tsym.getQualifiedName();
    if (!argType.contentEquals("java.time.temporal.TemporalField")) {
      return Description.NO_MATCH;
    }

    String declaringType = sym.owner.getQualifiedName().toString();
    ImmutableList<ChronoField> invalidChronoFields = UNSUPPORTED.get(declaringType);
    if (invalidChronoFields != null && isDefinitelyInvalidChronoField(tree, invalidChronoFields)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private static boolean isDefinitelyInvalidChronoField(
      MethodInvocationTree tree, Iterable<ChronoField> invalidChronoFields) {
    return getEnumName(Iterables.getOnlyElement(tree.getArguments()))
        .map(
            /* TODO(amalloy): We could pre-index chronoFields instead of iterating over it.
             * However, it's fairly rare to get to this point, so it's not a big deal if
             * it's relatively slow. */
            constant ->
                Streams.stream(invalidChronoFields).map(Enum::name).anyMatch(constant::equals))
        .orElse(false);
  }

  private static Optional<String> getEnumName(ExpressionTree chronoField) {
    if (chronoField instanceof IdentifierTree) { // e.g., SECOND_OF_DAY
      return Optional.of(((IdentifierTree) chronoField).getName().toString());
    }
    if (chronoField instanceof MemberSelectTree) { // e.g., ChronoField.SECOND_OF_DAY
      return Optional.of(((MemberSelectTree) chronoField).getIdentifier().toString());
    }
    return Optional.empty();
  }
}
