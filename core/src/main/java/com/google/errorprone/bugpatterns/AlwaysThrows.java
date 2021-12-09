/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.protobuf.ByteString;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "AlwaysThrows",
    summary = "Detects calls that will fail at runtime",
    severity = ERROR)
public class AlwaysThrows extends BugChecker implements MethodInvocationTreeMatcher {

  @SuppressWarnings("UnnecessarilyFullyQualified")
  private static final ImmutableMap<String, Consumer<CharSequence>> VALIDATORS =
      ImmutableMap.<String, Consumer<CharSequence>>builder()
          .put("java.time.Duration", java.time.Duration::parse)
          .put("java.time.Instant", java.time.Instant::parse)
          .put("java.time.LocalDate", java.time.LocalDate::parse)
          .put("java.time.LocalDateTime", java.time.LocalDateTime::parse)
          .put("java.time.LocalTime", java.time.LocalTime::parse)
          .put("java.time.MonthDay", java.time.MonthDay::parse)
          .put("java.time.OffsetDateTime", java.time.OffsetDateTime::parse)
          .put("java.time.OffsetTime", java.time.OffsetTime::parse)
          .put("java.time.Period", java.time.Period::parse)
          .put("java.time.Year", java.time.Year::parse)
          .put("java.time.YearMonth", java.time.YearMonth::parse)
          .put("java.time.ZonedDateTime", java.time.ZonedDateTime::parse)
          .buildOrThrow();

  enum Apis {
    PARSE_TIME(
        MethodMatchers.staticMethod()
            .onClassAny(VALIDATORS.keySet())
            .named("parse")
            .withParameters("java.lang.CharSequence")) {
      @Override
      void validate(MethodInvocationTree tree, String argument, VisitorState state)
          throws Throwable {
        MethodSymbol sym = ASTHelpers.getSymbol(tree);
        VALIDATORS.get(sym.owner.getQualifiedName().toString()).accept(argument);
      }
    },
    BYTE_STRING(
        MethodMatchers.staticMethod()
            .onClass("com.google.protobuf.ByteString")
            .named("fromHex")
            .withParameters("java.lang.String")) {
      @Override
      void validate(MethodInvocationTree tree, String argument, VisitorState state)
          throws Throwable {
        try {
          ByteString.class.getMethod("fromHex", String.class).invoke(null, argument);
        } catch (NoSuchMethodException | IllegalAccessException e) {
          return;
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }
    };

    Apis(Matcher<ExpressionTree> matcher) {
      this.matcher = matcher;
    }

    @SuppressWarnings("ImmutableEnumChecker") // is immutable
    private final Matcher<ExpressionTree> matcher;

    abstract void validate(MethodInvocationTree tree, String argument, VisitorState state)
        throws Throwable;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Apis api =
        stream(Apis.values()).filter(m -> m.matcher.matches(tree, state)).findAny().orElse(null);
    if (api == null) {
      return NO_MATCH;
    }
    String argument =
        ASTHelpers.constValue(Iterables.getOnlyElement(tree.getArguments()), String.class);
    if (argument == null) {
      return NO_MATCH;
    }
    try {
      api.validate(tree, argument, state);
    } catch (Throwable t) {
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "This call will fail at runtime with a %s: %s",
                  t.getClass().getSimpleName(), t.getMessage()))
          .build();
    }
    return NO_MATCH;
  }
}
