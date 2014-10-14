/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import edu.umd.cs.findbugs.formatStringChecker.ExtraFormatArgumentsException;
import edu.umd.cs.findbugs.formatStringChecker.Formatter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.lang.model.type.TypeKind;

/**
 * @author rburny@google.com (Radoslaw Burny)
 */
@BugPattern(name = "MalformedFormatString",
    summary = "Printf-like format string does not match its arguments",
    explanation = "Format strings for printf family of functions contain format specifiers"
        + " (placeholders) which must match amount and type of arguments that follow them. If there"
        + " are more arguments then specifiers, redundant ones are silently ignored. If there are"
        + " less, or their types don't match, runtime exception is thrown.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class MalformedFormatString extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String EXTRA_ARGUMENTS_MESSAGE =
      "Too many arguments for printf-like format string: expected %d, got %d";

  private static final Matcher<ExpressionTree> isPrintfLike = anyOf(
    isDescendantOfMethod("java.io.PrintStream", "format(java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintStream", "printf(java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintWriter", "format(java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintWriter", "printf(java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.util.Formatter", "format(java.lang.String,java.lang.Object...)"),
    staticMethod("java.lang.String", "format(java.lang.String,java.lang.Object...)")
    );
  private static final Matcher<ExpressionTree> isPrintfLikeWithLocale = anyOf(
    isDescendantOfMethod("java.io.PrintStream",
      "format(java.util.Locale,java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintStream",
        "printf(java.util.Locale,java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintWriter",
        "printf(java.util.Locale,java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.io.PrintWriter",
      "format(java.util.Locale,java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.util.Formatter",
        "format(java.util.Locale,java.lang.String,java.lang.Object...)"),
    staticMethod("java.lang.String",
        "format(java.util.Locale,java.lang.String,java.lang.Object...)")
    );
  private static final ImmutableMap<TypeKind, String> BOXED_TYPE_NAMES;

  static {
    EnumMap<TypeKind, String> boxedTypeNames = new EnumMap<>(TypeKind.class);
    boxedTypeNames.put(TypeKind.BYTE, Byte.class.getName());
    boxedTypeNames.put(TypeKind.SHORT, Short.class.getName());
    boxedTypeNames.put(TypeKind.INT, Integer.class.getName());
    boxedTypeNames.put(TypeKind.LONG, Long.class.getName());
    boxedTypeNames.put(TypeKind.FLOAT, Float.class.getName());
    boxedTypeNames.put(TypeKind.DOUBLE, Double.class.getName());
    boxedTypeNames.put(TypeKind.BOOLEAN, Boolean.class.getName());
    boxedTypeNames.put(TypeKind.CHAR, Character.class.getName());
    boxedTypeNames.put(TypeKind.NULL, Object.class.getName());
    // Apparently, matcher is run even if typing phase failed. Hence, we replace missing/erroneous
    // types with Object to prevent further failures.
    boxedTypeNames.put(TypeKind.ERROR, Object.class.getName());
    BOXED_TYPE_NAMES = Maps.immutableEnumMap(boxedTypeNames);
  }

  // get type name in format accepted by Formatter.check
  private static String getFormatterType(Type type) {
    String boxedTypeName = BOXED_TYPE_NAMES.get(type.getKind());
    String typeName = (boxedTypeName != null ? boxedTypeName : type.toString());
    return ("L" + typeName.replace('.', '/') + ";");
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    int formatIndex;
    if (isPrintfLike.matches(tree, state)) {
      formatIndex = 0;
    } else if (isPrintfLikeWithLocale.matches(tree, state)) {
      formatIndex = 1;
    } else {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> allArgs = tree.getArguments();
    ExpressionTree formatExpression = allArgs.get(formatIndex);
    List<? extends ExpressionTree> formatArgs = allArgs.subList(formatIndex + 1, allArgs.size());
    // If there's a sole argument of array type, this can be a non-varargs form call. Ignoring.
    if (formatArgs.size() == 1
        && ((JCExpression) formatArgs.get(0)).type.getKind() == TypeKind.ARRAY) {
      return Description.NO_MATCH;
    }
    String formatString = null;
    if (formatExpression.getKind() == Kind.STRING_LITERAL) {
      formatString = ((JCLiteral) formatExpression).getValue().toString();
    } else {
      Symbol sym = ASTHelpers.getSymbol(formatExpression);
      if (sym instanceof VarSymbol) {
        formatString = (String) ((VarSymbol) sym).getConstValue();
      }
    }
    if (formatString == null) {
      return Description.NO_MATCH;
    }

    List<String> argTypes = new ArrayList<>();
    for (ExpressionTree arg : formatArgs) {
      Type type = state.getTypes().erasure(((JCExpression) arg).type);
      argTypes.add(getFormatterType(type));
    }

    try {
      Formatter.check(formatString, argTypes.toArray(new String[0]));
    } catch (ExtraFormatArgumentsException e) {
      int begin = state.getEndPosition((JCExpression) allArgs.get(formatIndex + e.used));
      int end = state.getEndPosition((JCMethodInvocation) tree);
      if (end < 0) {
        return describeMatch(tree);
      }
      Fix fix = SuggestedFix.replace(begin, end - 1, "");
      String message = String.format(EXTRA_ARGUMENTS_MESSAGE, e.used, e.provided);
      return buildDescription(tree)
          .setMessage(message)
          .addFix(fix)
          .build();
    } catch (Exception e) {
      // TODO(user): provide fixes for other problems
    }
    return Description.NO_MATCH;
  }
}
