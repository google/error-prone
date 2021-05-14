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
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
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
    if (symbol instanceof MethodSymbol) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      if (methodSymbol.owner.packge().getQualifiedName().toString().startsWith("java.time")
          && methodSymbol.getModifiers().contains(Modifier.PUBLIC)) {
        if (ALLOWED_JAVA_TIME_METHODS.matches(tree, state)) {
          return false;
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Methods in {@link java.util.function} are pure, and their returnvalues should not be discarded.
   */
  private static boolean functionalMethod(ExpressionTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    return symbol instanceof MethodSymbol
        && ((MethodSymbol) symbol)
            .owner
            .packge()
            .getQualifiedName()
            .contentEquals("java.util.function");
  }

  /**
   * The return value of stream methods should always be checked (except for forEach and
   * forEachOrdered, which are void-returning and won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> STREAM_METHOD =
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
          instanceMethod().onDescendantOf("java.util.Collection").named("isEmpty").withParameters(),
          instanceMethod().onDescendantOf("java.util.Collection").named("size").withParameters(),
          instanceMethod().onDescendantOf("java.util.Collection").named("stream").withParameters(),
          instanceMethod().onDescendantOf("java.util.Collection").named("toArray").withParameters(),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .named("toArray")
              .withParameters("java.util.function.IntFunction"));

  /** APIs to check on the {@link java.util.Map} interface. */
  private static final Matcher<ExpressionTree> MAP_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .namedAnyOf("containsKey", "containsValue")
              .withParameters("java.lang.Object"));

  /** APIs to check on the {@link java.lang.Iterable} interface. */
  private static final Matcher<ExpressionTree> ITERABLE_METHODS =
      anyOf(
          instanceMethod().onDescendantOf("java.lang.Iterable").named("iterator").withParameters(),
          instanceMethod()
              .onDescendantOf("java.lang.Iterable")
              .named("spliterator")
              .withParameters());

  /** APIs to check on the {@link java.util.Iterator} interface. */
  private static final Matcher<ExpressionTree> ITERATOR_METHODS =
      instanceMethod().onDescendantOf("java.util.Iterator").named("hasNext").withParameters();

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

  private static final String PROTO_MESSAGE = "com.google.protobuf.MessageLite";

  /**
   * The return values of {@code ProtoMessage.newBuilder()}, {@code protoBuilder.build()} and {@code
   * protoBuilder.buildPartial()} should always be checked.
   */
  private static final Matcher<ExpressionTree> PROTO_METHODS =
      anyOf(
          staticMethod().onClass(isDescendantOf(PROTO_MESSAGE)).named("newBuilder"),
          instanceMethod()
              .onDescendantOf(PROTO_MESSAGE + ".Builder")
              .namedAnyOf("build", "buildPartial"));

  private static final Matcher<? super ExpressionTree> SPECIALIZED_MATCHER =
      anyOf(
          RETURNS_SAME_TYPE,
          ReturnValueIgnored::functionalMethod,
          STREAM_METHOD,
          STRING_METHODS,
          PROTO_METHODS,
          PRIMITIVE_METHODS,
          ARRAYS_METHODS,
          OPTIONAL_METHODS,
          TIME_UNIT_METHODS,
          ReturnValueIgnored::javaTimeTypes,
          COLLECTION_METHODS,
          MAP_METHODS,
          ITERABLE_METHODS,
          ITERATOR_METHODS);

  private final Matcher<? super ExpressionTree> matcher;

  public ReturnValueIgnored(ErrorProneFlags flags) {
    boolean checkOptional = flags.getBoolean("ReturnValueIgnored:MoreOptional").orElse(true);
    this.matcher =
        checkOptional ? anyOf(SPECIALIZED_MATCHER, MORE_OPTIONAL_METHODS) : SPECIALIZED_MATCHER;
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
