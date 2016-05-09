/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Calendar;
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
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.regex.Pattern;

import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FormatString",
  summary = "Invalid printf-style format string",
  category = JDK,
  maturity = EXPERIMENTAL,
  severity = ERROR
)
public class FormatString extends BugChecker implements MethodInvocationTreeMatcher {

  // TODO(cushon): add support for additional printf methods, maybe with an annotation
  private static final Matcher<ExpressionTree> FORMAT_METHOD =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.io.PrintStream")
              .withNameMatching(Pattern.compile("format|printf")),
          instanceMethod()
              .onDescendantOf("java.io.PrintWriter")
              .withNameMatching(Pattern.compile("format|printf")),
          instanceMethod().onDescendantOf("java.util.Formatter").named("format"),
          staticMethod().onClass("java.lang.String").named("format"),
          staticMethod()
              .onClass("java.io.Console")
              .withNameMatching(Pattern.compile("format|printf|readline|readPassword")));

  /**
   * Return an instance of the given type if it receives special handling by {@code String.format}.
   * For example, an intance of {@link Integer} will be returned for an input of type {@code int}
   * or {@link Integer}.
   */
  private static Object getInstance(Tree tree, VisitorState state) {
    Object value = ASTHelpers.constValue(tree);
    if (value != null) {
      return value;
    }
    Type type = ASTHelpers.getType(tree);
    Types types = state.getTypes();
    if (type.getKind() == TypeKind.NULL) {
      return null;
    }
    // normalize boxed primitives
    type = types.unboxedTypeOrType(types.erasure(type));
    if (type.isPrimitive()) {
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
          break;
      }
    }
    if (types.isSubtype(type, state.getSymtab().stringType)) {
      return String.valueOf("string");
    }
    if (types.isSubtype(type, state.getTypeFromString(BigDecimal.class.getName()))) {
      return BigDecimal.valueOf(42.0d);
    }
    if (types.isSubtype(type, state.getTypeFromString(BigInteger.class.getName()))) {
      return BigInteger.valueOf(43L);
    }
    if (types.isSubtype(type, state.getTypeFromString(Date.class.getName()))) {
      return new Date();
    }
    if (types.isSubtype(type, state.getTypeFromString(Calendar.class.getName()))) {
      return new GregorianCalendar();
    }
    return new Object();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!FORMAT_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    Deque<ExpressionTree> args = new ArrayDeque<ExpressionTree>(tree.getArguments());

    // skip the first argument of printf(Locale,String,Object...)
    if (ASTHelpers.isSameType(
        ASTHelpers.getType(args.peekFirst()),
        state.getTypeFromString(Locale.class.getName()),
        state)) {
      args.removeFirst();
    }

    String formatString = ASTHelpers.constValue(args.pollFirst(), String.class);
    if (formatString == null) {
      return Description.NO_MATCH;
    }

    // If the only argument is an Object[], it's an explicit varargs call.
    // Bail out, since we don't know what the actual argument types are.
    if (args.size() == 1) {
      Type type = ASTHelpers.getType(Iterables.getOnlyElement(args));
      if (type instanceof ArrayType
          && ASTHelpers.isSameType(
              ((ArrayType) type).elemtype, state.getSymtab().objectType, state)) {
        return Description.NO_MATCH;
      }
    }

    Iterable<Object> instances =
        Iterables.transform(
            args,
            new Function<ExpressionTree, Object>() {
              @Override
              public Object apply(ExpressionTree input) {
                return getInstance(input, state);
              }
            });

    String error = validate(formatString, instances);
    if (error == null) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).setMessage(error).build();
  }

  private String validate(String formatString, Iterable<Object> args) {
    try {
      String unused = String.format(formatString, Iterables.toArray(args, Object.class));
    } catch (DuplicateFormatFlagsException e) {
      return String.format("duplicate format flags: %s", e.getFlags());
    } catch (FormatFlagsConversionMismatchException e) {
      return String.format(
          "format specifier '%%%s' is not compatible with the given flag(s): %s",
          e.getConversion(),
          e.getFlags());
    } catch (IllegalFormatCodePointException e) {
      return String.format("invalid Unicode code point: %x", e.getCodePoint());
    } catch (IllegalFormatConversionException e) {
      return String.format(
          "illegal format conversion: '%s' cannot be formatted using '%%%s'",
          e.getArgumentClass(),
          e.getConversion());
    } catch (IllegalFormatFlagsException e) {
      return String.format("illegal format flags: %s", e.getFlags());
    } catch (IllegalFormatPrecisionException e) {
      return String.format("illegal format precision: %d", e.getPrecision());
    } catch (IllegalFormatWidthException e) {
      return String.format("illegal format width: %s", e.getWidth());
    } catch (MissingFormatArgumentException e) {
      return String.format("missing argument for format specifier '%s'", e.getFormatSpecifier());
    } catch (MissingFormatWidthException e) {
      return String.format("missing format width: %s", e.getFormatSpecifier());
    } catch (UnknownFormatConversionException e) {
      return unknownFormatConversion(e.getConversion());
    } catch (UnknownFormatFlagsException e) {
      // TODO(cushon): I don't think the implementation ever throws this.
      return String.format("unknown format flag(s): %s", e.getFlags());
    }
    return null;
  }

  private String unknownFormatConversion(String conversion) {
    if (conversion.equals("l")) {
      return "%l is not a valid format specifier; use %d for all integral types and %f for all "
          + "floating point types";
    }
    return String.format("unknown format conversion: '%s'", conversion);
  }
}
