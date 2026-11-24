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
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.nullLiteral;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.google.errorprone.matchers.field.FieldMatchers.staticField;
import static com.google.errorprone.predicates.TypePredicates.isPrimitive;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static com.google.errorprone.util.AnnotationNames.COMPILE_TIME_CONSTANT_ANNOTATION;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.matchers.method.ParameterPredicates;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/**
 * A matcher for compile-time-constant expressions.
 *
 * <p>For the purposes of this matcher, a compile-time constant expression is one of the following:
 *
 * <ol>
 *   <li>Any expression for which the Java compiler can determine a constant value at compile time.
 *   <li>The expression consisting of the literal {@code null}.
 *   <li>An enum constant.
 *   <li>A class literal (e.g. {@code String.class}).
 *   <li>A field of a class or record, which is declared {@code final} and is either:
 *       <ul>
 *         <li>Explicitly annotated with {@link CompileTimeConstant}.
 *         <li>A well-known compatible field, e.g. from the Java standard library. (See {@link
 *             #COMPATIBLE_FIELDS} below for the complete list.)
 *       </ul>
 *   <li>A formal parameter of a method or constructor, which is effectively {@code final} (either
 *       inferred or explicitly declared) and is annotated with {@link CompileTimeConstant}.
 *   <li>A {@link String} which is formed from the concatenation of other compile-time-constant
 *       {@link String}s.
 *   <li>A switch expression where all case bodies are compile-time constants.
 *   <li>A conditional expression (a ? b : c) where both branches are compile-time constants.
 *   <li>A call to a well-known (e.g. from a Java standard library class) constructor, static
 *       method, or instance method, where all arguments (including the receiver, if applicable) are
 *       compile-time constants and the resulting value is immutable. (See {@link
 *       #COMPATIBLE_METHODS} and {@link #COMPATIBLE_CONSTRUCTORS} below for the complete list.)
 * </ol>
 */
public class CompileTimeConstantExpressionMatcher implements Matcher<ExpressionTree> {

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

  /**
   * A matcher for {@link ExpressionTree}s for which the java compiler can compute a constant value
   * (except a literal {@code null}).
   */
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
        if (!COMPATIBLE_METHODS.matches(tree, state)) {
          return false;
        }

        // If this is an instance method, check the receiver.
        ExpressionTree receiver = getReceiver(tree);
        if (!isStatic(getSymbol(tree)) && (receiver == null || !receiver.accept(this, null))) {
          return false;
        }

        // Check all arguments.
        return tree.getArguments().stream().allMatch(a -> a.accept(this, null));
      }

      @Override
      public Boolean visitNewClass(NewClassTree tree, Void unused) {
        if (tree.getClassBody() != null || !COMPATIBLE_CONSTRUCTORS.matches(tree, state)) {
          return false;
        }
        ExpressionTree enclosing = tree.getEnclosingExpression();
        if (enclosing != null && !enclosing.accept(this, null)) {
          return false;
        }
        return tree.getArguments().stream().allMatch(a -> a.accept(this, null));
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

  /**
   * Matches method invocations that are known compatible with {@link CompileTimeConstant}, but are
   * "upstream" of this check and therefore cannot be annotated themselves.
   *
   * <p>To be {@link CompileTimeConstant}-compatible, a method must not depend on any runtime state,
   * (global tables, system properties, etc.) and its return value must be a compile-time constant
   * (including being immutable) whenever all of the arguments (including the method receiver, if
   * applicable) are compile-time constants.
   *
   * <p>Note that instance methods can only be {@link CompileTimeConstant}-compatible if there's no
   * way to override them; overriding implementations could depend on runtime state and/or return
   * non-constant values.
   */
  private static final Matcher<ExpressionTree> COMPATIBLE_METHODS =
      anyOf(
          // java.lang boxed primitives and numbers
          staticMethod()
              .onClass("java.lang.Integer")
              .namedAnyOf(
                  "bitCount",
                  "compare",
                  "compareUnsigned",
                  "compress",
                  "decode",
                  "divideUnsigned",
                  "expand",
                  "hashCode",
                  "highestOneBit",
                  "lowestOneBit",
                  "max",
                  "min",
                  "numberOfLeadingZeros",
                  "numberOfTrailingZeros",
                  "parseInt",
                  "parseUnsignedInt",
                  "remainderUnsigned",
                  "reverse",
                  "reverseBytes",
                  "rotateLeft",
                  "rotateRight",
                  "signum",
                  "sum",
                  "toBinaryString",
                  "toHexString",
                  "toOctalString",
                  "toString",
                  "toUnsignedLong",
                  "toUnsignedString",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Long")
              .namedAnyOf(
                  "bitCount",
                  "compare",
                  "compareUnsigned",
                  "compress",
                  "decode",
                  "divideUnsigned",
                  "expand",
                  "hashCode",
                  "highestOneBit",
                  "lowestOneBit",
                  "max",
                  "min",
                  "numberOfLeadingZeros",
                  "numberOfTrailingZeros",
                  "parseLong",
                  "parseUnsignedLong",
                  "remainderUnsigned",
                  "reverse",
                  "reverseBytes",
                  "rotateLeft",
                  "rotateRight",
                  "signum",
                  "sum",
                  "toBinaryString",
                  "toHexString",
                  "toOctalString",
                  "toString",
                  "toUnsignedString",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Short")
              .namedAnyOf(
                  "compare",
                  "compareUnsigned",
                  "decode",
                  "hashCode",
                  "parseShort",
                  "reverseBytes",
                  "toString",
                  "toUnsignedInt",
                  "toUnsignedLong",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Byte")
              .namedAnyOf(
                  "compare",
                  "compareUnsigned",
                  "decode",
                  "hashCode",
                  "parseByte",
                  "toString",
                  "toUnsignedInt",
                  "toUnsignedLong",
                  "valueOf"),
          // TODO(b/576770029): Allow the following char[] overloads:
          //   Character.codePointAt(char[], int)
          //   Character.codePointAt(char[], int, int)
          //   Character.codePointBefore(char[], int)
          //   Character.codePointBefore(char[], int, int)
          //   Character.codePointCount(char[], int, int)
          //   Character.offsetByCodePoints(char[], int, int, int, int)
          staticMethod()
              .onClass("java.lang.Character")
              .namedAnyOf("codePointAt", "codePointBefore")
              .withParameters("java.lang.CharSequence", "int"),
          staticMethod()
              .onClass("java.lang.Character")
              .namedAnyOf("codePointCount", "offsetByCodePoints")
              .withParameters("java.lang.CharSequence", "int", "int"),
          staticMethod()
              .onClass("java.lang.Character")
              .namedAnyOf(
                  "charCount",
                  "codePointOf",
                  "compare",
                  "digit",
                  "forDigit",
                  "getDirectionality",
                  "getName",
                  "getNumericValue",
                  "getType",
                  "hashCode",
                  "highSurrogate",
                  "isAlphabetic",
                  "isBmpCodePoint",
                  "isDefined",
                  "isDigit",
                  "isEmoji",
                  "isEmojiComponent",
                  "isEmojiModifier",
                  "isEmojiModifierBase",
                  "isEmojiPresentation",
                  "isExtendedPictographic",
                  "isHighSurrogate",
                  "isISOControl",
                  "isIdentifierIgnorable",
                  "isIdeographic",
                  "isJavaIdentifierPart",
                  "isJavaIdentifierStart",
                  "isJavaLetter",
                  "isJavaLetterOrDigit",
                  "isLetter",
                  "isLetterOrDigit",
                  "isLowSurrogate",
                  "isLowerCase",
                  "isMirrored",
                  "isSpace",
                  "isSpaceChar",
                  "isSupplementaryCodePoint",
                  "isSurrogate",
                  "isSurrogatePair",
                  "isTitleCase",
                  "isUnicodeIdentifierPart",
                  "isUnicodeIdentifierStart",
                  "isUpperCase",
                  "isValidCodePoint",
                  "isWhitespace",
                  "lowSurrogate",
                  "reverseBytes",
                  "toCodePoint",
                  "toLowerCase",
                  "toString",
                  "toTitleCase",
                  "toUpperCase",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Boolean")
              .namedAnyOf(
                  "compare",
                  "hashCode",
                  "logicalAnd",
                  "logicalOr",
                  "logicalXor",
                  "parseBoolean",
                  "toString",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Float")
              .namedAnyOf(
                  "compare",
                  "float16ToFloat",
                  "floatToFloat16",
                  "floatToIntBits",
                  "floatToRawIntBits",
                  "hashCode",
                  "intBitsToFloat",
                  "isFinite",
                  "isInfinite",
                  "isNaN",
                  "max",
                  "min",
                  "parseFloat",
                  "sum",
                  "toHexString",
                  "toString",
                  "valueOf"),
          staticMethod()
              .onClass("java.lang.Double")
              .namedAnyOf(
                  "compare",
                  "doubleToLongBits",
                  "doubleToRawLongBits",
                  "hashCode",
                  "isFinite",
                  "isInfinite",
                  "isNaN",
                  "longBitsToDouble",
                  "max",
                  "min",
                  "parseDouble",
                  "sum",
                  "toHexString",
                  "toString",
                  "valueOf"),
          instanceMethod()
              .onExactClassAny(
                  "java.lang.Integer",
                  "java.lang.Long",
                  "java.lang.Short",
                  "java.lang.Byte",
                  "java.lang.Character",
                  "java.lang.Boolean",
                  "java.lang.Float",
                  "java.lang.Double")
              .namedAnyOf(
                  "booleanValue",
                  "byteValue",
                  "charValue",
                  "compareTo",
                  "describeConstable",
                  "doubleValue",
                  "equals",
                  "floatValue",
                  "hashCode",
                  "intValue",
                  "isInfinite",
                  "isNaN",
                  "longValue",
                  "shortValue",
                  "toString"),

          // java.lang.String
          staticMethod().onClass("java.lang.String").named("join"),
          // TODO(b/576770029): Allow the following methods:
          //   String.valueOf(char[])
          //   String.valueOf(char[], int, int)
          //   String.copyValueOf(char[])
          //   String.copyValueOf(char[], int, int)
          staticMethod()
              .onClass("java.lang.String")
              .named("valueOf")
              .withParametersMatching(ParameterPredicates.of(isPrimitive())),
          instanceMethod()
              .onExactClass("java.lang.String")
              .namedAnyOf(
                  "charAt",
                  "codePointAt",
                  "codePointBefore",
                  "codePointCount",
                  "compareTo",
                  "compareToIgnoreCase",
                  "concat",
                  "contains",
                  "contentEquals",
                  "describeConstable",
                  "endsWith",
                  "equals",
                  "equalsIgnoreCase",
                  "hashCode",
                  "indent",
                  "indexOf",
                  "isBlank",
                  "isEmpty",
                  "lastIndexOf",
                  "length",
                  "matches",
                  "offsetByCodePoints",
                  "regionMatches",
                  "repeat",
                  "replace",
                  "replaceAll",
                  "replaceFirst",
                  "startsWith",
                  "strip",
                  "stripIndent",
                  "stripLeading",
                  "stripTrailing",
                  "subSequence",
                  "substring",
                  "toString",
                  "translateEscapes",
                  "trim"),
          instanceMethod()
              .onExactClass("java.lang.String")
              .namedAnyOf("toLowerCase", "toUpperCase")
              .withParameters("java.util.Locale"),

          // java.lang.Math and java.lang.StrictMath
          staticMethod()
              .onClassAny("java.lang.Math", "java.lang.StrictMath")
              .namedAnyOf(
                  "IEEEremainder",
                  "abs",
                  "absExact",
                  "acos",
                  "acosh",
                  "addExact",
                  "asin",
                  "asinh",
                  "atan",
                  "atan2",
                  "atanh",
                  "cbrt",
                  "ceil",
                  "ceilDiv",
                  "ceilDivExact",
                  "ceilMod",
                  "clamp",
                  "copySign",
                  "cos",
                  "cosh",
                  "decrementExact",
                  "divideExact",
                  "exp",
                  "expm1",
                  "floor",
                  "floorDiv",
                  "floorDivExact",
                  "floorMod",
                  "fma",
                  "getExponent",
                  "hypot",
                  "incrementExact",
                  "log",
                  "log10",
                  "log1p",
                  "max",
                  "min",
                  "multiplyExact",
                  "multiplyFull",
                  "multiplyHigh",
                  "negateExact",
                  "nextAfter",
                  "nextDown",
                  "nextUp",
                  "pow",
                  "powExact",
                  "rint",
                  "round",
                  "scalb",
                  "signum",
                  "sin",
                  "sinh",
                  "sqrt",
                  "subtractExact",
                  "tan",
                  "tanh",
                  "toDegrees",
                  "toIntExact",
                  "toRadians",
                  "ulp",
                  "unsignedMultiplyExact",
                  "unsignedMultiplyHigh",
                  "unsignedPowExact"),

          // java.lang.Enum
          staticMethod()
              .onClass("java.lang.Enum")
              .named("valueOf")
              .withParameters("java.lang.Class", "java.lang.String"),
          staticMethod()
              .onDescendantOf("java.lang.Enum")
              .named("valueOf")
              .withParameters("java.lang.String"),
          // Omit "toString" because it could be overridden.
          instanceMethod()
              .onDescendantOf("java.lang.Enum")
              .named("compareTo")
              .withParameters("java.lang.Enum"),
          instanceMethod()
              .onDescendantOf("java.lang.Enum")
              .named("equals")
              .withParameters("java.lang.Object"),
          instanceMethod()
              .onDescendantOf("java.lang.Enum")
              .namedAnyOf("describeConstable", "getDeclaringClass", "hashCode", "name", "ordinal")
              .withNoParameters(),

          // java.math
          // Omit instance methods on BigInteger and BigDecimal because neither the classes nor
          // their methods are final.
          staticMethod()
              .onClassAny("java.math.BigInteger", "java.math.BigDecimal")
              .named("valueOf"),
          instanceMethod()
              .onExactClass("java.math.MathContext")
              .namedAnyOf("equals", "getPrecision", "getRoundingMode", "hashCode", "toString"),

          // java.time
          staticMethod().onClass("java.time.Clock").namedAnyOf("fixed", "offset", "tick"),
          staticMethod()
              .onClassAny("java.time.DayOfWeek", "java.time.Month")
              .namedAnyOf("from", "of", "valueOf"),
          instanceMethod()
              .onExactClassAny("java.time.DayOfWeek", "java.time.Month")
              .namedAnyOf(
                  "firstDayOfYear",
                  "firstMonthOfQuarter",
                  "get",
                  "getDisplayName",
                  "getLong",
                  "getValue",
                  "isSupported",
                  "length",
                  "maxLength",
                  "minLength",
                  "minus",
                  "plus"),
          staticMethod()
              .onClass("java.time.Duration")
              .namedAnyOf(
                  "between",
                  "from",
                  "of",
                  "ofDays",
                  "ofHours",
                  "ofMillis",
                  "ofMinutes",
                  "ofNanos",
                  "ofSeconds",
                  "parse"),
          instanceMethod()
              .onExactClass("java.time.Duration")
              .namedAnyOf(
                  "abs",
                  "compareTo",
                  "dividedBy",
                  "equals",
                  "get",
                  "getNano",
                  "getSeconds",
                  "getUnits",
                  "hashCode",
                  "isNegative",
                  "isPositive",
                  "isZero",
                  "minus",
                  "minusDays",
                  "minusHours",
                  "minusMillis",
                  "minusMinutes",
                  "minusNanos",
                  "minusSeconds",
                  "multipliedBy",
                  "negated",
                  "plus",
                  "plusDays",
                  "plusHours",
                  "plusMillis",
                  "plusMinutes",
                  "plusNanos",
                  "plusSeconds",
                  "toDays",
                  "toDaysPart",
                  "toHours",
                  "toHoursPart",
                  "toMillis",
                  "toMillisPart",
                  "toMinutes",
                  "toMinutesPart",
                  "toNanos",
                  "toNanosPart",
                  "toSeconds",
                  "toSecondsPart",
                  "toString",
                  "truncatedTo",
                  "withNanos",
                  "withSeconds"),
          staticMethod()
              .onClass("java.time.Instant")
              .namedAnyOf("from", "ofEpochMilli", "ofEpochSecond", "parse"),
          instanceMethod()
              .onExactClass("java.time.Instant")
              .namedAnyOf(
                  "atOffset",
                  "atZone",
                  "compareTo",
                  "equals",
                  "get",
                  "getEpochSecond",
                  "getLong",
                  "getNano",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isSupported",
                  "minus",
                  "minusMillis",
                  "minusNanos",
                  "minusSeconds",
                  "plus",
                  "plusMillis",
                  "plusNanos",
                  "plusSaturating",
                  "plusSeconds",
                  "toEpochMilli",
                  "toString",
                  "truncatedTo",
                  "until",
                  "with"),
          staticMethod()
              .onClass("java.time.LocalDate")
              .namedAnyOf("from", "of", "ofEpochDay", "ofInstant", "ofYearDay", "parse"),
          instanceMethod()
              .onExactClass("java.time.LocalDate")
              .namedAnyOf(
                  "atStartOfDay",
                  "atTime",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getDayOfMonth",
                  "getDayOfWeek",
                  "getDayOfYear",
                  "getEra",
                  "getLong",
                  "getMonth",
                  "getMonthValue",
                  "getYear",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isEqual",
                  "isLeapYear",
                  "isSupported",
                  "lengthOfMonth",
                  "lengthOfYear",
                  "minus",
                  "minusDays",
                  "minusMonths",
                  "minusWeeks",
                  "minusYears",
                  "plus",
                  "plusDays",
                  "plusMonths",
                  "plusWeeks",
                  "plusYears",
                  "toEpochDay",
                  "toEpochSecond",
                  "toString",
                  "until",
                  "with",
                  "withDayOfMonth",
                  "withDayOfYear",
                  "withMonth",
                  "withYear"),
          staticMethod()
              .onClass("java.time.LocalDateTime")
              .namedAnyOf("from", "of", "ofEpochSecond", "ofInstant", "parse"),
          instanceMethod()
              .onExactClass("java.time.LocalDateTime")
              .namedAnyOf(
                  "atOffset",
                  "atZone",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getDayOfMonth",
                  "getDayOfWeek",
                  "getDayOfYear",
                  "getHour",
                  "getLong",
                  "getMinute",
                  "getMonth",
                  "getMonthValue",
                  "getNano",
                  "getSecond",
                  "getYear",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isEqual",
                  "isSupported",
                  "minus",
                  "minusDays",
                  "minusHours",
                  "minusMinutes",
                  "minusMonths",
                  "minusNanos",
                  "minusSeconds",
                  "minusWeeks",
                  "minusYears",
                  "plus",
                  "plusDays",
                  "plusHours",
                  "plusMinutes",
                  "plusMonths",
                  "plusNanos",
                  "plusSeconds",
                  "plusWeeks",
                  "plusYears",
                  "toEpochSecond",
                  "toInstant",
                  "toLocalDate",
                  "toLocalTime",
                  "toString",
                  "truncatedTo",
                  "until",
                  "with",
                  "withDayOfMonth",
                  "withDayOfYear",
                  "withHour",
                  "withMinute",
                  "withMonth",
                  "withNano",
                  "withSecond",
                  "withYear"),
          staticMethod()
              .onClass("java.time.LocalTime")
              .namedAnyOf("from", "of", "ofInstant", "ofNanoOfDay", "ofSecondOfDay", "parse"),
          instanceMethod()
              .onExactClass("java.time.LocalTime")
              .namedAnyOf(
                  "atDate",
                  "atOffset",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getHour",
                  "getLong",
                  "getMinute",
                  "getNano",
                  "getSecond",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isSupported",
                  "minus",
                  "minusHours",
                  "minusMinutes",
                  "minusNanos",
                  "minusSeconds",
                  "plus",
                  "plusHours",
                  "plusMinutes",
                  "plusNanos",
                  "plusSeconds",
                  "toEpochSecond",
                  "toNanoOfDay",
                  "toSecondOfDay",
                  "toString",
                  "truncatedTo",
                  "until",
                  "with",
                  "withHour",
                  "withMinute",
                  "withNano",
                  "withSecond"),
          staticMethod().onClass("java.time.MonthDay").namedAnyOf("from", "of", "parse"),
          instanceMethod()
              .onExactClass("java.time.MonthDay")
              .namedAnyOf(
                  "atYear",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getDayOfMonth",
                  "getLong",
                  "getMonth",
                  "getMonthValue",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isSupported",
                  "isValidYear",
                  "toString",
                  "with",
                  "withDayOfMonth",
                  "withMonth"),
          staticMethod()
              .onClass("java.time.OffsetDateTime")
              .namedAnyOf("from", "of", "ofInstant", "parse"),
          instanceMethod()
              .onExactClass("java.time.OffsetDateTime")
              .namedAnyOf(
                  "atZoneSameInstant",
                  "atZoneSimilarLocal",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getDayOfMonth",
                  "getDayOfWeek",
                  "getDayOfYear",
                  "getHour",
                  "getLong",
                  "getMinute",
                  "getMonth",
                  "getMonthValue",
                  "getNano",
                  "getOffset",
                  "getSecond",
                  "getYear",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isEqual",
                  "isSupported",
                  "minus",
                  "minusDays",
                  "minusHours",
                  "minusMinutes",
                  "minusMonths",
                  "minusNanos",
                  "minusSeconds",
                  "minusWeeks",
                  "minusYears",
                  "plus",
                  "plusDays",
                  "plusHours",
                  "plusMinutes",
                  "plusMonths",
                  "plusNanos",
                  "plusSeconds",
                  "plusWeeks",
                  "plusYears",
                  "toEpochSecond",
                  "toInstant",
                  "toLocalDate",
                  "toLocalDateTime",
                  "toLocalTime",
                  "toOffsetTime",
                  "toString",
                  "toZonedDateTime",
                  "truncatedTo",
                  "until",
                  "with",
                  "withDayOfMonth",
                  "withDayOfYear",
                  "withHour",
                  "withMinute",
                  "withMonth",
                  "withNano",
                  "withOffsetSameInstant",
                  "withOffsetSameLocal",
                  "withSecond",
                  "withYear"),
          staticMethod()
              .onClass("java.time.OffsetTime")
              .namedAnyOf("from", "of", "ofInstant", "parse"),
          instanceMethod()
              .onExactClass("java.time.OffsetTime")
              .namedAnyOf(
                  "atDate",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getHour",
                  "getLong",
                  "getMinute",
                  "getNano",
                  "getOffset",
                  "getSecond",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isEqual",
                  "isSupported",
                  "minus",
                  "minusHours",
                  "minusMinutes",
                  "minusNanos",
                  "minusSeconds",
                  "plus",
                  "plusHours",
                  "plusMinutes",
                  "plusNanos",
                  "plusSeconds",
                  "toEpochSecond",
                  "toLocalTime",
                  "toString",
                  "truncatedTo",
                  "until",
                  "with",
                  "withHour",
                  "withMinute",
                  "withNano",
                  "withOffsetSameInstant",
                  "withOffsetSameLocal",
                  "withSecond"),
          staticMethod()
              .onClass("java.time.Period")
              .namedAnyOf(
                  "between", "from", "of", "ofDays", "ofMonths", "ofWeeks", "ofYears", "parse"),
          instanceMethod()
              .onExactClass("java.time.Period")
              .namedAnyOf(
                  "equals",
                  "get",
                  "getDays",
                  "getMonths",
                  "getUnits",
                  "getYears",
                  "hashCode",
                  "isNegative",
                  "isZero",
                  "minus",
                  "minusDays",
                  "minusMonths",
                  "minusYears",
                  "multipliedBy",
                  "negated",
                  "normalized",
                  "plus",
                  "plusDays",
                  "plusMonths",
                  "plusYears",
                  "toString",
                  "toTotalMonths",
                  "withDays",
                  "withMonths",
                  "withYears"),
          staticMethod().onClass("java.time.Year").namedAnyOf("from", "isLeap", "of", "parse"),
          instanceMethod()
              .onExactClass("java.time.Year")
              .namedAnyOf(
                  "atDay",
                  "atMonth",
                  "atMonthDay",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getLong",
                  "getValue",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isLeap",
                  "isSupported",
                  "isValidMonthDay",
                  "length",
                  "minus",
                  "minusYears",
                  "plus",
                  "plusYears",
                  "toString",
                  "until",
                  "with"),
          staticMethod().onClass("java.time.YearMonth").namedAnyOf("from", "of", "parse"),
          instanceMethod()
              .onExactClass("java.time.YearMonth")
              .namedAnyOf(
                  "atDay",
                  "atEndOfMonth",
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getLong",
                  "getMonth",
                  "getMonthValue",
                  "getYear",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isLeapYear",
                  "isSupported",
                  "isValidDay",
                  "lengthOfMonth",
                  "lengthOfYear",
                  "minus",
                  "minusMonths",
                  "minusYears",
                  "plus",
                  "plusMonths",
                  "plusYears",
                  "toString",
                  "until",
                  "with",
                  "withMonth",
                  "withYear"),
          staticMethod()
              .onClass("java.time.ZoneOffset")
              .namedAnyOf(
                  "from",
                  "of",
                  "ofHours",
                  "ofHoursMinutes",
                  "ofHoursMinutesSeconds",
                  "ofTotalSeconds"),
          instanceMethod()
              .onExactClass("java.time.ZoneOffset")
              .namedAnyOf(
                  "compareTo",
                  "equals",
                  "get",
                  "getId",
                  "getLong",
                  "getTotalSeconds",
                  "hashCode",
                  "isSupported",
                  "normalized",
                  "toString"),
          staticMethod()
              .onClass("java.time.ZonedDateTime")
              .namedAnyOf("from", "of", "ofInstant", "ofLocal", "ofStrict"),
          instanceMethod()
              .onExactClass("java.time.ZonedDateTime")
              .namedAnyOf(
                  "compareTo",
                  "equals",
                  "format",
                  "get",
                  "getDayOfMonth",
                  "getDayOfWeek",
                  "getDayOfYear",
                  "getHour",
                  "getLong",
                  "getMinute",
                  "getMonth",
                  "getMonthValue",
                  "getNano",
                  "getOffset",
                  "getSecond",
                  "getYear",
                  "getZone",
                  "hashCode",
                  "isAfter",
                  "isBefore",
                  "isEqual",
                  "isSupported",
                  "minus",
                  "minusDays",
                  "minusHours",
                  "minusMinutes",
                  "minusMonths",
                  "minusNanos",
                  "minusSeconds",
                  "minusWeeks",
                  "minusYears",
                  "plus",
                  "plusDays",
                  "plusHours",
                  "plusMinutes",
                  "plusMonths",
                  "plusNanos",
                  "plusSeconds",
                  "plusWeeks",
                  "plusYears",
                  "toEpochSecond",
                  "toInstant",
                  "toLocalDate",
                  "toLocalDateTime",
                  "toLocalTime",
                  "toOffsetDateTime",
                  "toString",
                  "truncatedTo",
                  "until",
                  "with",
                  "withDayOfMonth",
                  "withDayOfYear",
                  "withEarlierOffsetAtOverlap",
                  "withFixedOffsetZone",
                  "withHour",
                  "withLaterOffsetAtOverlap",
                  "withMinute",
                  "withMonth",
                  "withNano",
                  "withSecond",
                  "withYear",
                  "withZoneSameInstant",
                  "withZoneSameLocal"),

          // java.util collections, optionals, and utilities
          staticMethod().onClass("java.util.List").namedAnyOf("copyOf", "of"),
          staticMethod().onClass("java.util.Set").namedAnyOf("copyOf", "of"),
          staticMethod().onClass("java.util.Map").namedAnyOf("copyOf", "entry", "of", "ofEntries"),
          // Omit unmodifiable* methods because it's redundant; any CTC collection is immutable.
          staticMethod()
              .onClass("java.util.Collections")
              .namedAnyOf(
                  "emptyEnumeration",
                  "emptyIterator",
                  "emptyList",
                  "emptyListIterator",
                  "emptyMap",
                  "emptyNavigableMap",
                  "emptyNavigableSet",
                  "emptySet",
                  "emptySortedMap",
                  "emptySortedSet",
                  "nCopies",
                  "reverseOrder",
                  "singleton",
                  "singletonList",
                  "singletonMap"),
          staticMethod()
              .onClassAny(
                  "java.util.Optional",
                  "java.util.OptionalInt",
                  "java.util.OptionalLong",
                  "java.util.OptionalDouble")
              .namedAnyOf("empty", "of", "ofNullable"),
          instanceMethod()
              .onExactClassAny(
                  "java.util.Optional",
                  "java.util.OptionalInt",
                  "java.util.OptionalLong",
                  "java.util.OptionalDouble")
              .namedAnyOf(
                  "equals",
                  "get",
                  "getAsDouble",
                  "getAsInt",
                  "getAsLong",
                  "hashCode",
                  "isEmpty",
                  "isPresent",
                  "orElse",
                  "orElseThrow",
                  "toString"),
          staticMethod()
              .onClass("java.util.Objects")
              .namedAnyOf(
                  "checkFromIndexSize",
                  "checkFromToIndex",
                  "checkIndex",
                  "isNull",
                  "nonNull",
                  "requireNonNull",
                  "requireNonNullElse"),
          staticMethod()
              .onClass("java.util.Comparator")
              .namedAnyOf("naturalOrder", "nullsFirst", "nullsLast", "reverseOrder"),
          staticMethod()
              .onClass("java.util.Locale")
              .namedAnyOf("caseFoldLanguageTag", "forLanguageTag", "of"),
          instanceMethod()
              .onExactClass("java.util.Locale")
              .namedAnyOf(
                  "equals",
                  "getCountry",
                  "getExtension",
                  "getLanguage",
                  "getScript",
                  "getUnicodeLocaleType",
                  "getVariant",
                  "hasExtensions",
                  "hashCode",
                  "stripExtensions",
                  "toLanguageTag",
                  "toString"),
          staticMethod().onClass("java.util.UUID").named("fromString"),
          instanceMethod()
              .onExactClass("java.util.UUID")
              .namedAnyOf(
                  "clockSequence",
                  "compareTo",
                  "equals",
                  "getLeastSignificantBits",
                  "getMostSignificantBits",
                  "hashCode",
                  "node",
                  "timestamp",
                  "toString",
                  "variant",
                  "version"),
          staticMethod()
              .onClass("java.util.regex.Pattern")
              .namedAnyOf("compile", "matches", "quote"),
          instanceMethod()
              .onExactClass("java.util.regex.Pattern")
              .namedAnyOf("flags", "pattern", "toString"),

          // java.nio.charset.Charset - only include final methods.
          instanceMethod()
              .onDescendantOf("java.nio.charset.Charset")
              .named("compareTo")
              .withParameters("java.nio.charset.Charset"),
          instanceMethod()
              .onDescendantOf("java.nio.charset.Charset")
              .named("equals")
              .withParameters("java.lang.Object"),
          instanceMethod()
              .onDescendantOf("java.nio.charset.Charset")
              .namedAnyOf("hashCode", "isRegistered", "name", "toString")
              .withNoParameters(),

          // Guava Immutable collections
          //
          // Remove once/if Guava annotates to tell us the result is a compile-time constant iff the
          // arguments are compile-time constants.
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.ImmutableList",
                  "com.google.common.collect.ImmutableSet",
                  "com.google.common.collect.ImmutableMap",
                  "com.google.common.collect.ImmutableMultiset",
                  "com.google.common.collect.ImmutableSortedSet",
                  "com.google.common.collect.ImmutableSortedMap")
              .named("of"));

  /**
   * Matches constructors that are known to produce compile-time constants if their arguments are
   * all compile-time constants, but are defined "upstream" of this check and therefore cannot be
   * annotated {@code @CompileTimeConstant} themselves.
   *
   * <p>The class must be immutable to be included here. (It can seem valid to include a mutable
   * class {@code T}'s constructor if it doesn't depend on any runtime state, but that causes
   * constness issues (like b/576770029) unless the lint can forbid operations mutate the object.)
   */
  private static final Matcher<ExpressionTree> COMPATIBLE_CONSTRUCTORS =
      anyOf(
          constructor().forClass("java.lang.Integer"),
          constructor().forClass("java.lang.Long"),
          constructor().forClass("java.lang.Short"),
          constructor().forClass("java.lang.Byte"),
          constructor().forClass("java.lang.Character"),
          constructor().forClass("java.lang.Boolean"),
          constructor().forClass("java.lang.Float"),
          constructor().forClass("java.lang.Double"),

          // TODO(b/576770029): Allow the byte[], char[], and int[] String constructors.
          // String(byte[]) and String(byte[], int, int)) are disallowed because they use the
          // default charset, which is not a compile-time constant.
          constructor().forClass("java.lang.String").withNoParameters(),
          constructor().forClass("java.lang.String").withParameters("java.lang.String"),

          // TODO(b/576770029): Allow the byte[] BigInteger constructors and char[] BigDecimal
          // constructors.
          constructor().forClass("java.math.BigInteger").withParameters("java.lang.String"),
          constructor().forClass("java.math.BigInteger").withParameters("java.lang.String", "int"),
          constructor().forClass("java.math.BigDecimal").withParameters("double"),
          constructor()
              .forClass("java.math.BigDecimal")
              .withParameters("double", "java.math.MathContext"),
          constructor().forClass("java.math.BigDecimal").withParameters("int"),
          constructor()
              .forClass("java.math.BigDecimal")
              .withParameters("int", "java.math.MathContext"),
          constructor().forClass("java.math.BigDecimal").withParameters("long"),
          constructor()
              .forClass("java.math.BigDecimal")
              .withParameters("long", "java.math.MathContext"),
          constructor().forClass("java.math.BigDecimal").withParameters("java.lang.String"),
          constructor()
              .forClass("java.math.BigDecimal")
              .withParameters("java.lang.String", "java.math.MathContext"),
          constructor().forClass("java.math.MathContext"),
          constructor().forClass("java.util.Locale"),
          constructor().forClass("java.util.UUID"));

  /**
   * Matches static final fields that are known to be compile-time constants, but are "upstream" of
   * this check and therefore cannot be annotated {@code @CompileTimeConstant} themselves.
   *
   * <p>Note that {@code static final} fields of primitive or {@link String} type initialized with a
   * constant expression (JLS §4.12.4 "constant variables", such as {@link Integer#MAX_VALUE} or
   * {@link Math#PI}) already have their constant value computed by {@code javac} and are matched
   * via {@link ASTHelpers#constValue}, so only non-primitive, non-{@link String} constant fields
   * need to be listed here.
   */
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
