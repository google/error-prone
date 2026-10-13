/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.nullLiteral;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.google.errorprone.matchers.field.FieldMatchers.staticField;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/// A matcher for compile-time-constant expressions.
///
/// For the purposes of this matcher, a compile-time constant expression is one of the following:
///
/// 1. Any expression for which the Java compiler can determine a constant value at compile time.
/// 1. The expression consisting of the literal `null`.
/// 1. An enum constant.
/// 1. A class literal (e.g. `String.class`).
/// 1. A field of a class or record, which is declared `final` and is either:
///    - Explicitly annotated with [CompileTimeConstant].
///    - A well-known compatible field, e.g. from the Java standard library. (See
///      [#COMPATIBLE_FIELDS] below for the complete list.)
/// 1. A formal parameter of a method or constructor, which is effectively `final` (either inferred
///    or explicitly declared) and is annotated with [CompileTimeConstant].
/// 1. A [String] which is formed from the concatenation of other compile-time-constant [String]s.
/// 1. A switch expression where all case bodies are compile-time constants.
/// 1. A conditional expression (a ? b : c) where both branches are compile-time constants.
/// 1. The result of an invocation of one of the following immutable collection factory methods,
///    where all arguments are compile-time constants:
///    - `ImmutableList.of(...)`
///    - `ImmutableSet.of(...)`
///    - `ImmutableMap.of(...)`
///    - `ImmutableMultiset.of(...)`
///    - `ImmutableSortedSet.of(...)`
///    - `ImmutableSortedMap.of(...)`
///    - `List.of(...)`
///    - `Set.of(...)`
///    - `Map.of(...)`
public class CompileTimeConstantExpressionMatcher implements Matcher<ExpressionTree> {

  private static final ClassName COMPILE_TIME_CONSTANT_ANNOTATION =
      ClassName.of(CompileTimeConstant.class.getName());

  private static final Matcher<ExpressionTree> INSTANCE =
      anyOf(
          // TODO(xtof): Consider utilising mdempsky's closed-over-addition matcher
          // (perhaps extended for other arithmetic operations).
          new ExpressionWithConstValueMatcher(),
          nullLiteral(),
          // Allows passing a null literal to a method with a @CompileTimeConstant parameter
          // when A) there's an overload of the method that takes a supertype of the parameter's
          // type and B) the overload with the @CompileTimeConstant parameter specifically needs to
          // be the overload that is called.
          toType(TypeCastTree.class, typeCast(anything(), nullLiteral())));

  public static Matcher<ExpressionTree> instance() {
    return INSTANCE;
  }

  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    return INSTANCE.matches(t, state);
  }

  private static final Matcher<ExpressionTree> IMMUTABLE_FACTORY =
      anyOf(
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.ImmutableList",
                  "com.google.common.collect.ImmutableSet",
                  "com.google.common.collect.ImmutableMap",
                  "com.google.common.collect.ImmutableMultiset",
                  "com.google.common.collect.ImmutableSortedSet",
                  "com.google.common.collect.ImmutableSortedMap",
                  "java.util.List",
                  "java.util.Set",
                  "java.util.Map")
              .named("of"));

  /// A matcher for [ExpressionTree]s for which the java compiler can compute a constant value
  /// (except a literal `null`).
  private static final class ExpressionWithConstValueMatcher implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree tree, VisitorState state) {
      return tree.accept(new TreeVisitor(state), null);
    }

    private static final class TreeVisitor extends SimpleTreeVisitor<Boolean, Void> {
      final VisitorState state;

      private TreeVisitor(VisitorState state) {
        this.state = state;
      }

      @Override
      public Boolean visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
        return tree.getTrueExpression().accept(this, null)
            && tree.getFalseExpression().accept(this, null);
      }

      @Override
      public Boolean visitSwitchExpression(SwitchExpressionTree tree, Void unused) {
        // This is intentionally quite restrictive: only accept bare values and throws.
        return tree.getCases().stream()
            .allMatch(c ->
                c.getCaseKind().equals(CaseKind.RULE)
                    && (c.getBody().accept(this, null) || c.getBody() instanceof ThrowTree));
      }

      @Override
      public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        return IMMUTABLE_FACTORY.matches(tree, state)
            && tree.getArguments().stream().allMatch(a -> a.accept(this, null));
      }

      @Override
      public Boolean visitBinary(BinaryTree tree, Void unused) {
        return defaultAction(tree, null)
            || (tree.getKind().equals(Kind.PLUS)
                && tree.getLeftOperand().accept(this, null)
                && tree.getRightOperand().accept(this, null));
      }

      @Override
      public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
        return tree.getExpression().accept(this, null);
      }

      @Override
      protected Boolean defaultAction(Tree node, Void unused) {
        if (constValue(node) != null) {
          return true;
        }
        if (!(getSymbol(node) instanceof VarSymbol varSymbol)) {
          return false;
        }

        // Class literals are always compile-time constants. (Not allowed to call .class on a
        // runtime Class<?> value.)
        if (varSymbol.getSimpleName().contentEquals("class")) {
          return true;
        }

        return switch (varSymbol.getKind()) {
          case ENUM_CONSTANT -> true;
          case PARAMETER, FIELD ->
              isConsideredFinal(varSymbol)
                  && (hasCompileTimeConstantAnnotation(state, varSymbol)
                      || isWellKnownCompatibleField(state, node));
          default -> false;
        };
      }
    }
  }

  public static boolean hasCompileTimeConstantAnnotation(VisitorState state, Symbol symbol) {
    Symbol annotation = COMPILE_TIME_CONSTANT_ANNOTATION.symbol(state);
    // If we can't look up the annotation in the current VisitorState, then presumably it couldn't
    // be present on a Symbol we're inspecting.
    return annotation != null && symbol.attribute(annotation) != null;
  }

  public static boolean isWellKnownCompatibleField(VisitorState state, Tree node) {
    return node instanceof ExpressionTree expressionTree
        && COMPATIBLE_FIELDS.matches(expressionTree, state);
  }

  /// Matches static final fields that are known to be compile-time constants, but are "upstream" of
  /// this check and therefore cannot be annotated `@CompileTimeConstant` themselves.
  ///
  /// Note that `static final` fields of primitive or [String] type initialized with a constant
  /// expression (JLS §4.12.4 "constant variables", such as [Integer#MAX_VALUE] or [Math#PI])
  /// already have their constant value computed by `javac` and are matched via
  /// [ASTHelpers#constValue], so only non-primitive, non-[String] constant fields need to be listed
  /// here.
  private static final Matcher<ExpressionTree> COMPATIBLE_FIELDS =
      anyOf(
          // java.lang
          staticField().onClass("java.lang.Boolean").namedAnyOf("FALSE", "TRUE", "TYPE"),
          staticField().onClass("java.lang.Byte").named("TYPE"),
          staticField().onClass("java.lang.Character").named("TYPE"),
          staticField().onClass("java.lang.Double").named("TYPE"),
          staticField().onClass("java.lang.Float").named("TYPE"),
          staticField().onClass("java.lang.Integer").named("TYPE"),
          staticField().onClass("java.lang.Long").named("TYPE"),
          staticField().onClass("java.lang.Short").named("TYPE"),
          staticField().onClass("java.lang.Void").named("TYPE"),
          staticField().onClass("java.lang.String").named("CASE_INSENSITIVE_ORDER"),

          // java.math
          staticField().onClass("java.math.BigDecimal").namedAnyOf("ONE", "TEN", "TWO", "ZERO"),
          staticField().onClass("java.math.BigInteger").namedAnyOf("ONE", "TEN", "TWO", "ZERO"),
          staticField()
              .onClass("java.math.MathContext")
              .namedAnyOf("DECIMAL128", "DECIMAL32", "DECIMAL64", "UNLIMITED"),

          // java.time
          staticField().onClass("java.time.Duration").named("ZERO"),
          staticField().onClass("java.time.Instant").namedAnyOf("EPOCH", "MAX", "MIN"),
          staticField().onClass("java.time.LocalDate").namedAnyOf("EPOCH", "MAX", "MIN"),
          staticField().onClass("java.time.LocalDateTime").namedAnyOf("MAX", "MIN"),
          staticField().onClass("java.time.LocalTime").namedAnyOf("MAX", "MIDNIGHT", "MIN", "NOON"),
          staticField().onClass("java.time.OffsetDateTime").namedAnyOf("MAX", "MIN"),
          staticField().onClass("java.time.OffsetTime").namedAnyOf("MAX", "MIN"),
          staticField().onClass("java.time.Period").named("ZERO"),
          staticField().onClass("java.time.ZoneOffset").namedAnyOf("MAX", "MIN", "UTC"),
          staticField()
              .onClass("java.time.format.DateTimeFormatter")
              .namedAnyOf(
                  "BASIC_ISO_DATE",
                  "ISO_DATE",
                  "ISO_DATE_TIME",
                  "ISO_INSTANT",
                  "ISO_LOCAL_DATE",
                  "ISO_LOCAL_DATE_TIME",
                  "ISO_LOCAL_TIME",
                  "ISO_OFFSET_DATE",
                  "ISO_OFFSET_DATE_TIME",
                  "ISO_OFFSET_TIME",
                  "ISO_ORDINAL_DATE",
                  "ISO_TIME",
                  "ISO_WEEK_DATE",
                  "ISO_ZONED_DATE_TIME",
                  "RFC_1123_DATE_TIME"),
          staticField()
              .onClass("java.time.temporal.IsoFields")
              .namedAnyOf(
                  "DAY_OF_QUARTER",
                  "QUARTER_OF_YEAR",
                  "QUARTER_YEARS",
                  "WEEK_BASED_YEAR",
                  "WEEK_BASED_YEARS",
                  "WEEK_OF_WEEK_BASED_YEAR"),
          staticField()
              .onClass("java.time.temporal.JulianFields")
              .namedAnyOf("JULIAN_DAY", "MODIFIED_JULIAN_DAY", "RATA_DIE"),
          staticField().onClass("java.time.temporal.WeekFields").namedAnyOf("ISO", "SUNDAY_START"),

          // java.util
          staticField()
              .onClass("java.util.Collections")
              .namedAnyOf("EMPTY_LIST", "EMPTY_MAP", "EMPTY_SET"),
          staticField()
              .onClass("java.util.Locale")
              .namedAnyOf(
                  "CANADA",
                  "CANADA_FRENCH",
                  "CHINA",
                  "CHINESE",
                  "ENGLISH",
                  "FRANCE",
                  "FRENCH",
                  "GERMAN",
                  "GERMANY",
                  "ITALIAN",
                  "ITALY",
                  "JAPAN",
                  "JAPANESE",
                  "KOREA",
                  "KOREAN",
                  "PRC",
                  "ROOT",
                  "SIMPLIFIED_CHINESE",
                  "TAIWAN",
                  "TRADITIONAL_CHINESE",
                  "UK",
                  "US"),

          // java.nio.charset
          staticField()
              .onClass("java.nio.charset.StandardCharsets")
              .namedAnyOf(
                  "ISO_8859_1",
                  "US_ASCII",
                  "UTF_16",
                  "UTF_16BE",
                  "UTF_16LE",
                  "UTF_32",
                  "UTF_32BE",
                  "UTF_32LE",
                  "UTF_8"));
}
