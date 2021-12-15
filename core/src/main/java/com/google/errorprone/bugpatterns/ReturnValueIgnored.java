/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.nothing;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isExactTypeAny;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/** @author alexeagle@google.com (Alex Eagle) */
@BugPattern(
    name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored", "CheckReturnValue"},
    summary = "Return value of this method must be used",
    severity = ERROR)
public class ReturnValueIgnored extends AbstractReturnValueIgnored {
  /**
   * A set of types which this checker should examine method calls on.
   *
   * <p>There are also some high-priority return value ignored checks in SpotBugs for various
   * threading constructs which do not return the same type as the receiver. This check does not
   * deal with them, since the fix is less straightforward. See a list of the SpotBugs checks here:
   * https://github.com/spotbugs/spotbugs/blob/master/spotbugs/src/main/java/edu/umd/cs/findbugs/ba/CheckReturnAnnotationDatabase.java
   */
  private static final ImmutableSet<String> TYPES_TO_CHECK =
      ImmutableSet.of("java.math.BigInteger", "java.math.BigDecimal", "java.nio.file.Path");

  /**
   * Matches method invocations in which the method being called is on an instance of a type in the
   * TYPES_TO_CHECK set and returns the same type (e.g. String.trim() returns a String).
   */
  private static final Matcher<ExpressionTree> RETURNS_SAME_TYPE =
      allOf(
          (t, s) -> TYPES_TO_CHECK.contains(ASTHelpers.getReceiverType(t).toString()),
          (t, s) -> isSameType(ASTHelpers.getReceiverType(t), ASTHelpers.getReturnType(t), s));

  /**
   * This matcher allows the following methods in {@code java.time}:
   *
   * <ul>
   *   <li>any methods named {@code parse}
   *   <li>any static method named {@code of}
   *   <li>any static method named {@code from}
   *   <li>any instance method starting with {@code append} on {@link
   *       java.time.format.DateTimeFormatterBuilder}
   *   <li>{@link java.time.temporal.ChronoField#checkValidIntValue}
   *   <li>{@link java.time.temporal.ChronoField#checkValidValue}
   *   <li>{@link java.time.temporal.ValueRange#checkValidValue}
   * </ul>
   */
  private static final Matcher<ExpressionTree> ALLOWED_JAVA_TIME_METHODS =
      anyOf(
          staticMethod().anyClass().named("parse"),
          instanceMethod().anyClass().named("parse"),
          staticMethod().anyClass().named("of"),
          staticMethod().anyClass().named("from"),
          staticMethod().onClass("java.time.ZoneId").named("ofOffset"),
          instanceMethod()
              .onExactClass("java.time.format.DateTimeFormatterBuilder")
              .withNameMatching(Pattern.compile("^(append|parse|pad|optional).*")),
          instanceMethod()
              .onExactClass("java.time.temporal.ChronoField")
              .named("checkValidIntValue"),
          instanceMethod().onExactClass("java.time.temporal.ChronoField").named("checkValidValue"),
          instanceMethod().onExactClass("java.time.temporal.ValueRange").named("checkValidValue"));

  /**
   * {@link java.time} types are immutable. The only methods we allow ignoring the return value on
   * are the {@code parse}-style APIs since folks often use it for validation.
   */
  private static boolean javaTimeTypes(ExpressionTree tree, VisitorState state) {
    if (packageStartsWith("java.time").matches(tree, state)) {
      return false;
    }
    Symbol symbol = ASTHelpers.getSymbol(tree);
    return symbol instanceof MethodSymbol
        && symbol.owner.packge().getQualifiedName().toString().startsWith("java.time")
        && symbol.getModifiers().contains(Modifier.PUBLIC)
        && !ALLOWED_JAVA_TIME_METHODS.matches(tree, state);
  }

  /**
   * Methods in {@link java.util.function} are pure, and their return values should not be
   * discarded.
   */
  private static boolean functionalMethod(ExpressionTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    return symbol instanceof MethodSymbol
        && symbol.owner.packge().getQualifiedName().contentEquals("java.util.function");
  }

  /**
   * The return value of stream methods should always be checked (except for forEach and
   * forEachOrdered, which are void-returning and won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> STREAM_METHODS =
      instanceMethod().onDescendantOf("java.util.stream.BaseStream");

  /**
   * The return values of {@link java.util.Arrays} methods should always be checked (except for
   * void-returning ones, which won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> ARRAYS_METHODS =
      staticMethod().onClass("java.util.Arrays");

  /**
   * The return values of {@link java.lang.String} methods should always be checked (except for
   * void-returning ones, which won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> STRING_METHODS =
      anyMethod().onClass("java.lang.String");

  private static final ImmutableSet<String> PRIMITIVE_TYPES =
      ImmutableSet.of(
          "java.lang.Boolean",
          "java.lang.Byte",
          "java.lang.Character",
          "java.lang.Double",
          "java.lang.Float",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Short");

  /** All methods on the primitive wrapper types. */
  private static final Matcher<ExpressionTree> PRIMITIVE_NON_PARSING_METHODS =
      anyMethod().onClass(isExactTypeAny(PRIMITIVE_TYPES));

  /**
   * Parsing-style methods on the primitive wrapper types (e.g., {@link
   * java.lang.Integer#decode(String)}).
   */
  // TODO(kak): Instead of special casing the parsing style methods, we could consider looking for a
  // surrounding try/catch block (which folks use to validate input data).
  private static final Matcher<ExpressionTree> PRIMITIVE_PARSING_METHODS =
      anyOf(
          staticMethod().onClass("java.lang.Character").namedAnyOf("toChars", "codePointCount"),
          staticMethod().onClassAny(PRIMITIVE_TYPES).named("decode"),
          staticMethod()
              .onClassAny(PRIMITIVE_TYPES)
              .withNameMatching(Pattern.compile("^parse[A-z]*")),
          staticMethod()
              .onClassAny(PRIMITIVE_TYPES)
              .named("valueOf")
              .withParameters("java.lang.String"),
          staticMethod()
              .onClassAny(PRIMITIVE_TYPES)
              .named("valueOf")
              .withParameters("java.lang.String", "int"));

  // TODO(kak): we may want to change this to an opt-out list per class (e.g., check _all_ of the
  // methods on `java.util.Collection`, except this set. That would be much more future-proof.
  // However, we need to make sure we're only checking the APIs defined on the interface, and not
  // all methods on the descendant type.

  /** APIs to check on the {@link java.util.Collection} interface. */
  private static final Matcher<ExpressionTree> COLLECTION_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("contains")
              .withParameters("java.lang.Object"),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("containsAll")
              .withParameters("java.util.Collection"),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("isEmpty")
              .withNoParameters(),
          instanceMethod().onDescendantOf("java.util.Collection").named("size").withNoParameters(),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("stream")
              .withNoParameters(),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("toArray")
              .withNoParameters(),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("toArray")
              .withParameters("java.util.function.IntFunction"));

  /** APIs to check on the {@link java.util.Map} interface. */
  // TODO(b/188207175): consider adding Map.get() and Map.getOrDefault()
  private static final Matcher<ExpressionTree> MAP_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .namedAnyOf("containsKey", "containsValue")
              .withParameters("java.lang.Object"),
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .namedAnyOf("isEmpty", "size", "entrySet", "keySet", "values"),
          staticMethod().onClass("java.util.Map").namedAnyOf("of", "copyOf", "entry", "ofEntries"));

  /** APIs to check on the {@link java.util.Map.Entry} interface. */
  private static final Matcher<ExpressionTree> MAP_ENTRY_METHODS =
      anyOf(
          staticMethod().onClass("java.util.Map.Entry"),
          instanceMethod().onDescendantOf("java.util.Map.Entry").namedAnyOf("getKey", "getValue"));

  /** APIs to check on the {@link java.lang.Iterable} interface. */
  private static final Matcher<ExpressionTree> ITERABLE_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.lang.Iterable")
              .named("iterator")
              .withNoParameters(),
          instanceMethod()
              .onDescendantOf("java.lang.Iterable")
              .named("spliterator")
              .withNoParameters());

  /** APIs to check on the {@link java.util.Iterator} interface. */
  private static final Matcher<ExpressionTree> ITERATOR_METHODS =
      instanceMethod().onDescendantOf("java.util.Iterator").named("hasNext").withNoParameters();

  private static final Matcher<ExpressionTree> COLLECTOR_METHODS =
      anyOf(
          anyMethod().onClass("java.util.stream.Collector"),
          anyMethod().onClass("java.util.stream.Collectors"));

  /**
   * The return values of primitive types (e.g., {@link java.lang.Integer}) should always be checked
   * (except for parsing-type methods and void-returning methods, which won't be checked by
   * AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> PRIMITIVE_METHODS =
      allOf(not(PRIMITIVE_PARSING_METHODS), PRIMITIVE_NON_PARSING_METHODS);

  /**
   * The return values of {@link java.util.Optional} static methods and some instance methods should
   * always be checked.
   */
  private static final Matcher<ExpressionTree> OPTIONAL_METHODS =
      anyOf(
          staticMethod().onClass("java.util.Optional"),
          instanceMethod().onExactClass("java.util.Optional").namedAnyOf("isEmpty", "isPresent"));

  /**
   * The return values of {@link java.util.Optional} methods should always be checked (except for
   * void-returning ones, which won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> MORE_OPTIONAL_METHODS =
      anyMethod().onClass("java.util.Optional");

  /**
   * The return values of {@link java.util.concurrent.TimeUnit} methods should always be checked.
   */
  private static final Matcher<ExpressionTree> TIME_UNIT_METHODS =
      anyMethod().onClass("java.util.concurrent.TimeUnit");

  /** APIs to check on {@code JodaTime} types. */
  // TODO(kak): there's a ton more we could do here
  private static final Matcher<ExpressionTree> JODA_TIME_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("org.joda.time.ReadableInstant")
              .named("getMillis")
              .withNoParameters(),
          instanceMethod()
              .onDescendantOf("org.joda.time.ReadableDuration")
              .named("getMillis")
              .withNoParameters());

  private static final String PROTO_MESSAGE = "com.google.protobuf.MessageLite";

  /**
   * The return values of {@code ProtoMessage.newBuilder()}, {@code protoBuilder.build()} and {@code
   * protoBuilder.buildPartial()} should always be checked.
   */
  private static final Matcher<ExpressionTree> PROTO_METHODS =
      anyOf(
          staticMethod().onDescendantOf(PROTO_MESSAGE).named("newBuilder"),
          instanceMethod()
              .onDescendantOf(PROTO_MESSAGE + ".Builder")
              .namedAnyOf("build", "buildPartial"));

  private static final Matcher<ExpressionTree> CLASS_METHODS =
      allOf(
          anyMethod().onClass("java.lang.Class"),
          not(staticMethod().onClass("java.lang.Class").named("forName")),
          not(instanceMethod().onExactClass("java.lang.Class").named("getMethod")));

  private static final Matcher<ExpressionTree> OBJECT_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.lang.Object")
              .namedAnyOf("getClass", "hashCode", "clone", "toString")
              .withNoParameters(),
          instanceMethod()
              .onDescendantOf("java.lang.Object")
              .namedAnyOf("equals")
              .withParameters("java.lang.Object"));

  private static final Matcher<ExpressionTree> CHAR_SEQUENCE_METHODS =
      anyMethod().onClass("java.lang.CharSequence");

  private static final Matcher<ExpressionTree> ENUM_METHODS = anyMethod().onClass("java.lang.Enum");

  private static final Matcher<ExpressionTree> THROWABLE_METHODS =
      instanceMethod()
          .onDescendantOf("java.lang.Throwable")
          .namedAnyOf(
              "getCause", "getLocalizedMessage", "getMessage", "getStackTrace", "getSuppressed");

  private static final Matcher<ExpressionTree> OBJECTS_METHODS =
      allOf(
          staticMethod().onClass("java.util.Objects"),
          not(
              staticMethod()
                  .onClass("java.util.Objects")
                  .namedAnyOf(
                      "checkFromIndexSize",
                      "checkFromToIndex",
                      "checkIndex",
                      "requireNonNull",
                      "requireNonNullElse",
                      "requireNonNullElseGet")));

  private static final Matcher<? super ExpressionTree> SPECIALIZED_MATCHER =
      anyOf(
          // keep-sorted start
          ARRAYS_METHODS,
          CHAR_SEQUENCE_METHODS,
          COLLECTION_METHODS,
          COLLECTOR_METHODS,
          ENUM_METHODS,
          ITERABLE_METHODS,
          ITERATOR_METHODS,
          JODA_TIME_METHODS,
          MAP_ENTRY_METHODS,
          MAP_METHODS,
          OBJECTS_METHODS,
          OPTIONAL_METHODS,
          PRIMITIVE_METHODS,
          PROTO_METHODS,
          RETURNS_SAME_TYPE,
          ReturnValueIgnored::functionalMethod,
          ReturnValueIgnored::javaTimeTypes,
          STREAM_METHODS,
          STRING_METHODS,
          THROWABLE_METHODS,
          TIME_UNIT_METHODS
          // keep-sorted end
          );

  private final Matcher<? super ExpressionTree> matcher;

  public ReturnValueIgnored(ErrorProneFlags flags) {
    this.matcher =
        anyOf(
            SPECIALIZED_MATCHER,
            getMatcher(flags, "ReturnValueIgnored:MoreOptional", MORE_OPTIONAL_METHODS),
            getMatcher(flags, "ReturnValueIgnored:ClassMethods", CLASS_METHODS),
            getMatcher(flags, "ReturnValueIgnored:ObjectMethods", OBJECT_METHODS));
  }

  private static Matcher<? super ExpressionTree> getMatcher(
      ErrorProneFlags flags, String flagName, Matcher<ExpressionTree> matcher) {
    return flags.getBoolean(flagName).orElse(true) ? matcher : nothing();
  }

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return matcher;
  }

  @Override
  protected String getMessage(Name name) {
    return String.format("Return value of '%s' must be used", name);
  }
}
