/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isExactTypeAnyOf;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ClassName;
import com.sun.source.tree.ExpressionTree;

/** Matchers and class name constants for regular expression APIs. */
public final class RegexMatchers {

  public static final ClassName JDK_PATTERN_CLASS = ClassName.of("java.util.regex.Pattern");
  public static final ClassName JDK_MATCHER_CLASS = ClassName.of("java.util.regex.Matcher");
  public static final ClassName RE2J_PATTERN_CLASS = ClassName.of("com.google.re2j.Pattern");
  public static final ClassName RE2J_MATCHER_CLASS = ClassName.of("com.google.re2j.Matcher");
  public static final ClassName SAFERE_PATTERN_CLASS = ClassName.of("org.safere.Pattern");
  public static final ClassName SAFERE_MATCHER_CLASS = ClassName.of("org.safere.Matcher");
  public static final ClassName SAFERE_CROSSCHECK_MATCHER_CLASS =
      ClassName.of("org.safere.crosscheck.Matcher");

  private static final ClassName STRING_CLASS = ClassName.of("java.lang.String");
  private static final ClassName SPLITTER_CLASS = ClassName.of("com.google.common.base.Splitter");
  private static final ClassName PREDICATES_CLASS =
      ClassName.of("com.google.common.base.Predicates");

  /** Immutable {@code Pattern} classes that do not hold closeable native resources. */
  public static final ImmutableSet<ClassName> NON_CLOSEABLE_PATTERN_CLASSES =
      ImmutableSet.<ClassName>builder()
          .add(JDK_PATTERN_CLASS)
          .add(RE2J_PATTERN_CLASS)
          .add(SAFERE_PATTERN_CLASS)
          .build();

  /** All supported regular expression {@code Pattern} classes. */
  public static final ImmutableSet<ClassName> ALL_PATTERN_CLASSES =
      ImmutableSet.<ClassName>builder().addAll(NON_CLOSEABLE_PATTERN_CLASSES).build();

  /** All supported regular expression {@code Matcher} classes. */
  public static final ImmutableSet<ClassName> ALL_MATCHER_CLASSES =
      ImmutableSet.<ClassName>builder()
          .add(JDK_MATCHER_CLASS)
          .add(RE2J_MATCHER_CLASS)
          .add(SAFERE_MATCHER_CLASS)
          .add(SAFERE_CROSSCHECK_MATCHER_CLASS)
          .build();

  /** Matches static {@code compile} (and {@code compileUnsafe}) calls on {@code Pattern} types. */
  public static final Matcher<ExpressionTree> PATTERN_COMPILE_METHODS =
      anyOf(staticMethod().onClass(isExactTypeAnyOf(ALL_PATTERN_CLASSES)).named("compile"));

  /** Matches static {@code matches} (and {@code matchesUnsafe}) calls on {@code Pattern} types. */
  public static final Matcher<ExpressionTree> PATTERN_MATCHES_METHODS =
      anyOf(staticMethod().onClass(isExactTypeAnyOf(ALL_PATTERN_CLASSES)).named("matches"));

  /** Matches static {@code quote} calls on {@code Pattern} types. */
  public static final Matcher<ExpressionTree> PATTERN_QUOTE_METHODS =
      staticMethod().onClass(isExactTypeAnyOf(ALL_PATTERN_CLASSES)).named("quote");

  /** Matches instance {@code matcher} calls on {@code Pattern} types. */
  public static final Matcher<ExpressionTree> PATTERN_MATCHER_METHODS =
      instanceMethod().onClass(isExactTypeAnyOf(ALL_PATTERN_CLASSES)).named("matcher");

  /** Matches {@link String#matches(String)}. */
  public static final Matcher<ExpressionTree> STRING_MATCHES_METHOD =
      instanceMethod()
          .onExactClass(STRING_CLASS)
          .named("matches")
          .withParametersOfType(STRING_CLASS);

  /** Matches {@link String#replaceFirst(String, String)} and {@link String#replaceAll}. */
  public static final Matcher<ExpressionTree> STRING_REPLACE_METHODS =
      instanceMethod()
          .onExactClass(STRING_CLASS)
          .namedAnyOf("replaceFirst", "replaceAll")
          .withParametersOfType(STRING_CLASS, STRING_CLASS);

  /** Matches {@link String#split(String)} and {@link String#split(String, int)}. */
  public static final Matcher<ExpressionTree> STRING_SPLIT_METHOD =
      instanceMethod().onExactClass(STRING_CLASS).named("split");

  /** Matches {@link String#splitWithDelimiters(String, int)}. */
  public static final Matcher<ExpressionTree> STRING_SPLIT_WITH_DELIMITERS_METHOD =
      instanceMethod().onExactClass(STRING_CLASS).named("splitWithDelimiters");

  /** Matches {@code String.split} and {@code String.splitWithDelimiters}. */
  public static final Matcher<ExpressionTree> STRING_SPLIT_METHODS =
      anyOf(STRING_SPLIT_METHOD, STRING_SPLIT_WITH_DELIMITERS_METHOD);

  /** Matches {@code com.google.common.base.Splitter.onPattern(String)}. */
  public static final Matcher<ExpressionTree> SPLITTER_ON_PATTERN_METHOD =
      staticMethod().onClass(SPLITTER_CLASS).named("onPattern");

  /** Matches {@code com.google.common.base.Predicates.containsPattern(String)}. */
  public static final Matcher<ExpressionTree> PREDICATES_CONTAINS_PATTERN_METHOD =
      staticMethod().onClass(PREDICATES_CLASS).named("containsPattern");

  private static final CharMatcher SURROGATE =
      CharMatcher.inRange(Character.MIN_HIGH_SURROGATE, Character.MAX_LOW_SURROGATE);

  private static final CharMatcher SPLIT_FAST_PATH_SINGLE_CHAR =
      CharMatcher.anyOf(".$|()[{^?*+\\").or(SURROGATE).negate();

  private static final CharMatcher SPLIT_FAST_PATH_ESCAPED_CHAR =
      CharMatcher.inRange('0', '9')
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('A', 'Z'))
          .or(SURROGATE)
          .negate();

  /**
   * Returns true if {@code regex} takes the non-regex fast path in {@link String#split(String,
   * int)} and {@link String#splitWithDelimiters(String, int)} without compiling a {@link
   * java.util.regex.Pattern}.
   */
  public static boolean isStringSplitFastPath(String regex) {
    return switch (regex.length()) {
      case 1 -> SPLIT_FAST_PATH_SINGLE_CHAR.matches(regex.charAt(0));
      case 2 -> regex.charAt(0) == '\\' && SPLIT_FAST_PATH_ESCAPED_CHAR.matches(regex.charAt(1));
      default -> false;
    };
  }

  private RegexMatchers() {}
}
