/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.util.Arrays.asList;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.GregorianCalendar;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;

/** Utilities for validating format strings. */
public final class FormatStringValidation {

  /** Description of an incorrect format method call. */
  @AutoValue
  public abstract static class ValidationResult {
    /** A human-readable diagnostic message. */
    public abstract String message();

    public static ValidationResult create(String message) {
      return new AutoValue_FormatStringValidation_ValidationResult(message);
    }
  }

  static Stream<String> constValues(Tree tree) {
    List<Tree> flat = new ArrayList<>();
    new SimpleTreeVisitor<Void, Void>() {
      @Override
      public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
        visit(tree.getTrueExpression(), null);
        visit(tree.getFalseExpression(), null);
        return null;
      }

      @Override
      protected Void defaultAction(Tree tree, Void unused) {
        flat.add(tree);
        return null;
      }
    }.visit(tree, null);
    return flat.stream().map(t -> ASTHelpers.constValue(t, String.class)).filter(x -> x != null);
  }

  @Nullable
  public static ValidationResult validate(
      @Nullable MethodSymbol formatMethodSymbol,
      Collection<? extends ExpressionTree> arguments,
      VisitorState state) {
    Preconditions.checkArgument(
        !arguments.isEmpty(),
        "A format method should have one or more arguments, but method (%s) has zero arguments.",
        formatMethodSymbol);

    Deque<ExpressionTree> args = new ArrayDeque<>(arguments);

    Stream<String> formatStrings = constValues(args.removeFirst());
    if (formatStrings == null) {
      return null;
    }

    // If the only argument is an Object[], it's an explicit varargs call.
    // Bail out, since we don't know what the actual argument types are.
    if (args.size() == 1 && (formatMethodSymbol == null || formatMethodSymbol.isVarArgs())) {
      Type type = ASTHelpers.getType(Iterables.getOnlyElement(args));
      if (type instanceof Type.ArrayType
          && ASTHelpers.isSameType(
              ((Type.ArrayType) type).elemtype, state.getSymtab().objectType, state)) {
        return null;
      }
    }

    Object[] instances =
        args.stream()
            .map(
                (ExpressionTree input) -> {
                  try {
                    return getInstance(input, state);
                  } catch (RuntimeException t) {
                    // ignore symbol completion failures
                    return null;
                  }
                })
            .toArray();

    return formatStrings
        .map(formatString -> validate(formatString, instances))
        .filter(x -> x != null)
        .findFirst()
        .orElse(null);
  }

  /**
   * Return an instance of the given type if it receives special handling by {@code String.format}.
   * For example, an intance of {@link Integer} will be returned for an input of type {@code int} or
   * {@link Integer}.
   */
  @Nullable
  private static Object getInstance(Tree tree, VisitorState state) {
    Object value = ASTHelpers.constValue(tree);
    if (value != null) {
      return value;
    }
    Type type = ASTHelpers.getType(tree);
    return getInstance(type, state);
  }

  @Nullable
  private static Object getInstance(Type type, VisitorState state) {
    Types types = state.getTypes();
    if (type.getKind() == TypeKind.NULL) {
      return null;
    }
    // normalize boxed primitives
    Type unboxedType = types.unboxedTypeOrType(types.erasure(type));
    if (unboxedType.isPrimitive()) {
      type = unboxedType;
      switch (type.getKind()) {
        case BOOLEAN:
          return false;
        case BYTE:
          return (byte) 1;
        case SHORT:
          return (short) 2;
        case INT:
          return 3;
        case LONG:
          return 4L;
        case CHAR:
          return 'c';
        case FLOAT:
          return 5.0f;
        case DOUBLE:
          return 6.0d;
        case VOID:
        case NONE:
        case NULL:
        case ERROR:
          return null;
        case ARRAY:
          return new Object[0];
        default:
          throw new AssertionError(type.getKind());
      }
    }
    if (isSubtype(types, type, state.getSymtab().stringType)) {
      return "string";
    }
    if (isSubtype(types, type, state.getTypeFromString(BigDecimal.class.getName()))) {
      return BigDecimal.valueOf(42.0d);
    }
    if (isSubtype(types, type, state.getTypeFromString(BigInteger.class.getName()))) {
      return BigInteger.valueOf(43L);
    }
    if (isSameType(type, state.getTypeFromString(Number.class.getName()), state)) {
      // String.format only supports well-known subtypes of Number, but custom subtypes of Number
      // are rarer than using it as a union of Byte/Short/Integer/Long/BigInteger, so we allow
      // Number to avoid false positives.
      return 0;
    }
    if (isSubtype(types, type, state.getTypeFromString(Date.class.getName()))) {
      return new Date();
    }
    if (isSubtype(types, type, state.getTypeFromString(Calendar.class.getName()))) {
      return new GregorianCalendar();
    }
    if (isSubtype(types, type, state.getTypeFromString(Instant.class.getName()))) {
      return Instant.now();
    }
    if (isSubtype(types, type, state.getTypeFromString(TemporalAccessor.class.getName()))) {
      return Instant.now().atZone(ZoneId.systemDefault());
    }
    Type lazyArg = COM_GOOGLE_COMMON_FLOGGER_LAZYARG.get(state);
    if (lazyArg != null) {
      Type asLazyArg = types.asSuper(type, lazyArg.tsym);
      if (asLazyArg != null && !asLazyArg.getTypeArguments().isEmpty()) {
        return getInstance(getOnlyElement(asLazyArg.getTypeArguments()), state);
      }
    }
    return new Object();
  }

  private static boolean isSubtype(Types types, Type t, Type s) {
    return s != null && types.isSubtype(t, s);
  }

  private static ValidationResult validate(String formatString, Object[] arguments) {
    try {
      String unused = String.format(formatString, arguments);
    } catch (DuplicateFormatFlagsException e) {
      return ValidationResult.create(String.format("duplicate format flags: %s", e.getFlags()));
    } catch (FormatFlagsConversionMismatchException e) {
      return ValidationResult.create(
          String.format(
              "format specifier '%%%s' is not compatible with the given flag(s): %s",
              e.getConversion(), e.getFlags()));
    } catch (IllegalFormatCodePointException e) {
      return ValidationResult.create(
          String.format("invalid Unicode code point: %x", e.getCodePoint()));
    } catch (IllegalFormatConversionException e) {
      return ValidationResult.create(
          String.format(
              "illegal format conversion: '%s' cannot be formatted using '%%%s'",
              e.getArgumentClass().getName(), e.getConversion()));
    } catch (IllegalFormatFlagsException e) {
      return ValidationResult.create(String.format("illegal format flags: %s", e.getFlags()));
    } catch (IllegalFormatPrecisionException e) {
      return ValidationResult.create(
          String.format("illegal format precision: %d", e.getPrecision()));
    } catch (IllegalFormatWidthException e) {
      return ValidationResult.create(String.format("illegal format width: %s", e.getWidth()));
    } catch (MissingFormatArgumentException e) {
      return ValidationResult.create(
          String.format("missing argument for format specifier '%s'", e.getFormatSpecifier()));
    } catch (MissingFormatWidthException e) {
      return ValidationResult.create(
          String.format("missing format width: %s", e.getFormatSpecifier()));
    } catch (UnknownFormatConversionException e) {
      return ValidationResult.create(unknownFormatConversion(e.getConversion()));
    } catch (UnknownFormatFlagsException e) {
      // TODO(cushon): I don't think the implementation ever throws this.
      return ValidationResult.create(String.format("unknown format flag(s): %s", e.getFlags()));
    } catch (IllegalFormatException e) {
      // Fall back for other invalid format strings, e.g. IllegalFormatArgumentIndexException that
      // was added in JDK 16
      return ValidationResult.create(e.getMessage());
    }
    return extraFormatArguments(formatString, asList(arguments));
  }

  @Nullable
  private static ValidationResult extraFormatArguments(
      String formatString, List<Object> arguments) {
    int used =
        IntStream.rangeClosed(0, arguments.size())
            .filter(i -> doesItFormat(formatString, arguments.subList(0, i)))
            .findFirst()
            .orElse(0);
    if (used == arguments.size()) {
      return null;
    }
    return ValidationResult.create(
        String.format("extra format arguments: used %d, provided %d", used, arguments.size()));
  }

  private static boolean doesItFormat(String formatString, List<Object> arguments) {
    try {
      String unused = String.format(formatString, arguments.toArray());
      return true;
    } catch (IllegalFormatException e) {
      return false;
    }
  }

  private static String unknownFormatConversion(String conversion) {
    if (conversion.equals("l")) {
      return "%l is not a valid format specifier; use %d to format integral types as a decimal "
          + "integer, and %f, %g or %e to format floating point types (depending on your "
          + "formatting needs)";
    }
    return String.format("unknown format conversion: '%s'", conversion);
  }

  private FormatStringValidation() {}

  private static final Supplier<Type> COM_GOOGLE_COMMON_FLOGGER_LAZYARG =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.common.flogger.LazyArg"));
}
