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

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import edu.umd.cs.findbugs.formatStringChecker.ExtraFormatArgumentsException;
import edu.umd.cs.findbugs.formatStringChecker.Formatter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.GregorianCalendar;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;

/** Utilities for validating format strings. */
public class FormatStringValidation {

  /** Description of an incorrect format method call. */
  @AutoValue
  public abstract static class ValidationResult {
    /** The exception thrown by {@code String.format} or {@code Formatter.check}. */
    @Nullable
    public abstract Exception exception();

    /** A human-readable diagnostic message. */
    public abstract String message();

    public static ValidationResult create(@Nullable Exception exception, String message) {
      return new AutoValue_FormatStringValidation_ValidationResult(exception, message);
    }
  }

  static Stream<String> constValues(Tree tree) {
    if (tree instanceof ConditionalExpressionTree) {
      ConditionalExpressionTree cond = (ConditionalExpressionTree) tree;
      String t = ASTHelpers.constValue(cond.getTrueExpression(), String.class);
      String f = ASTHelpers.constValue(cond.getFalseExpression(), String.class);
      if (t == null || f == null) {
        return null;
      }
      return Stream.of(t, f);
    }
    String r = ASTHelpers.constValue(tree, String.class);
    return r != null ? Stream.of(r) : null;
  }

  @Nullable
  public static ValidationResult validate(
      @Nullable MethodSymbol formatMethodSymbol,
      Collection<? extends ExpressionTree> arguments,
      final VisitorState state) {

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

    Iterable<Object> instances =
        Iterables.transform(
            args,
            new Function<ExpressionTree, Object>() {
              @Override
              public Object apply(ExpressionTree input) {
                try {
                  return getInstance(input, state);
                } catch (Throwable t) {
                  // ignore symbol completion failures
                  return null;
                }
              }
            });

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
          return Byte.valueOf((byte) 1);
        case SHORT:
          return Short.valueOf((short) 2);
        case INT:
          return Integer.valueOf(3);
        case LONG:
          return Long.valueOf(4);
        case CHAR:
          return Character.valueOf('c');
        case FLOAT:
          return Float.valueOf(5.0f);
        case DOUBLE:
          return Double.valueOf(6.0d);
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
      return String.valueOf("string");
    }
    if (isSubtype(types, type, state.getTypeFromString(BigDecimal.class.getName()))) {
      return BigDecimal.valueOf(42.0d);
    }
    if (isSubtype(types, type, state.getTypeFromString(BigInteger.class.getName()))) {
      return BigInteger.valueOf(43L);
    }
    if (isSubtype(types, type, state.getTypeFromString(Date.class.getName()))) {
      return new Date();
    }
    if (isSubtype(types, type, state.getTypeFromString(Calendar.class.getName()))) {
      return new GregorianCalendar();
    }
    if (isSubtype(types, type, state.getTypeFromString(TemporalAccessor.class.getName()))) {
      return LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    }
    return new Object();
  }

  private static boolean isSubtype(Types types, Type t, Type s) {
    return s != null && types.isSubtype(t, s);
  }

  private static ValidationResult validate(String formatString, Iterable<Object> arguments) {
    try {
      String unused = String.format(formatString, Iterables.toArray(arguments, Object.class));
    } catch (DuplicateFormatFlagsException e) {
      return ValidationResult.create(e, String.format("duplicate format flags: %s", e.getFlags()));
    } catch (FormatFlagsConversionMismatchException e) {
      return ValidationResult.create(
          e,
          String.format(
              "format specifier '%%%s' is not compatible with the given flag(s): %s",
              e.getConversion(), e.getFlags()));
    } catch (IllegalFormatCodePointException e) {
      return ValidationResult.create(
          e, String.format("invalid Unicode code point: %x", e.getCodePoint()));
    } catch (IllegalFormatConversionException e) {
      return ValidationResult.create(
          e,
          String.format(
              "illegal format conversion: '%s' cannot be formatted using '%%%s'",
              e.getArgumentClass().getName(), e.getConversion()));
    } catch (IllegalFormatFlagsException e) {
      return ValidationResult.create(e, String.format("illegal format flags: %s", e.getFlags()));
    } catch (IllegalFormatPrecisionException e) {
      return ValidationResult.create(
          e, String.format("illegal format precision: %d", e.getPrecision()));
    } catch (IllegalFormatWidthException e) {
      return ValidationResult.create(e, String.format("illegal format width: %s", e.getWidth()));
    } catch (MissingFormatArgumentException e) {
      return ValidationResult.create(
          e, String.format("missing argument for format specifier '%s'", e.getFormatSpecifier()));
    } catch (MissingFormatWidthException e) {
      return ValidationResult.create(
          e, String.format("missing format width: %s", e.getFormatSpecifier()));
    } catch (UnknownFormatConversionException e) {
      return ValidationResult.create(e, unknownFormatConversion(e.getConversion()));
    } catch (UnknownFormatFlagsException e) {
      // TODO(cushon): I don't think the implementation ever throws this.
      return ValidationResult.create(e, String.format("unknown format flag(s): %s", e.getFlags()));
    }

    try {
      // arguments are specified as type descriptors, and all we care about checking is the arity
      String[] argDescriptors =
          Collections.nCopies(Iterables.size(arguments), "Ljava/lang/Object;")
              .toArray(new String[0]);
      Formatter.check(formatString, argDescriptors);
    } catch (ExtraFormatArgumentsException e) {
      return ValidationResult.create(
          e, String.format("extra format arguments: used %d, provided %d", e.used, e.provided));
    } catch (Exception ignored) {
      // everything else is validated by String.format above
    }
    return null;
  }

  private static String unknownFormatConversion(String conversion) {
    if (conversion.equals("l")) {
      return "%l is not a valid format specifier; use %d for all integral types and %f for all "
          + "floating point types";
    }
    return String.format("unknown format conversion: '%s'", conversion);
  }
}
