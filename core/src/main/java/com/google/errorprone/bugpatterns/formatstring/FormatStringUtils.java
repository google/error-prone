/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.FORMAT_STRING_ANNOTATION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Locale;

/** Format string utilities. */
public final class FormatStringUtils {

  // TODO(cushon): add support for additional printf methods, maybe with an annotation
  private static final Matcher<ExpressionTree> FORMAT_METHOD =
      anyOf(
          instanceMethod()
              .onDescendantOfAny(
                  "java.io.PrintStream",
                  "java.io.PrintWriter",
                  "java.util.Formatter",
                  "java.io.Console")
              .namedAnyOf("format", "printf"),
          staticMethod().onClass("java.lang.String").named("format"),
          // Exclude zero-arg java.io.Console.readPassword from format methods.
          instanceMethod()
              .onExactClass("java.io.Console")
              .withSignature("readPassword(java.lang.String,java.lang.Object...)"),
          // Exclude zero-arg method java.io.Console.readLine from format methods.
          instanceMethod()
              .onExactClass("java.io.Console")
              .withSignature("readLine(java.lang.String,java.lang.Object...)"));

  private static final Matcher<ExpressionTree> FORMATTED_METHOD =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  public static ImmutableList<ExpressionTree> formatMethodAnnotationArguments(
      MethodInvocationTree tree, VisitorState state) {
    return formatMethodAnnotationArguments(tree, getSymbol(tree), tree.getArguments(), state);
  }

  public static ImmutableList<ExpressionTree> formatMethodAnnotationArguments(
      Tree tree, MethodSymbol symbol, List<? extends ExpressionTree> args, VisitorState state) {
    if (!hasAnnotation(symbol, FORMAT_METHOD_ANNOTATION, state)) {
      return ImmutableList.of();
    }
    int formatStringIndex = formatStringIndex(symbol, state);
    if (formatStringIndex == -1) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(args.subList(formatStringIndex, args.size()));
  }

  private static int formatStringIndex(MethodSymbol symbol, VisitorState state) {
    Type stringType = state.getSymtab().stringType;
    var params = symbol.getParameters();
    int firstStringIndex = -1;
    for (int i = 0; i < params.size(); i++) {
      VarSymbol param = params.get(i);
      if (hasAnnotation(param, FORMAT_STRING_ANNOTATION, state)) {
        return i;
      }
      if (firstStringIndex < 0 && isSameType(param.type, stringType, state)) {
        firstStringIndex = i;
      }
    }
    return firstStringIndex;
  }

  public static ImmutableList<ExpressionTree> formatMethodArguments(
      MethodInvocationTree tree, VisitorState state) {

    if (FORMATTED_METHOD.matches(tree, state)) {
      /*
        Java 15 and greater supports the formatted method on an instance of string. If found
        then use the string value as the pattern and all-of-the arguments and send directly to
        the validate method.
      */
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      // an unqualified call to 'formatted', possibly inside the definition
      // of java.lang.String
      if (receiver == null) {
        return ImmutableList.of();
      }
      return ImmutableList.<ExpressionTree>builder()
          .add(receiver)
          .addAll(tree.getArguments())
          .build();
    }

    if (FORMAT_METHOD.matches(tree, state)) {
      ImmutableList<ExpressionTree> args = ImmutableList.copyOf(tree.getArguments());
      // skip the first argument of printf(Locale,String,Object...)
      if (ASTHelpers.isSameType(
          ASTHelpers.getType(args.getFirst()),
          state.getTypeFromString(Locale.class.getName()),
          state)) {
        args = args.subList(1, args.size());
      }
      return args;
    }

    ImmutableList<ExpressionTree> args = formatMethodAnnotationArguments(tree, state);
    if (!args.isEmpty()) {
      return args;
    }

    return ImmutableList.of();
  }

  private FormatStringUtils() {}
}
