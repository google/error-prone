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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
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
          instanceMethod().onExactClass("java.lang.String").named("formatted"),
          // Exclude zero-arg java.io.Console.readPassword from format methods.
          instanceMethod()
              .onExactClass("java.io.Console")
              .withSignature("readPassword(java.lang.String,java.lang.Object...)"),
          // Exclude zero-arg method java.io.Console.readLine from format methods.
          instanceMethod()
              .onExactClass("java.io.Console")
              .withSignature("readLine(java.lang.String,java.lang.Object...)"));

  public static ImmutableList<ExpressionTree> formatMethodArguments(
      MethodInvocationTree tree, VisitorState state) {
    if (!FORMAT_METHOD.matches(tree, state)) {
      return ImmutableList.of();
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return ImmutableList.of();
    }
    ImmutableList<ExpressionTree> args = ImmutableList.copyOf(tree.getArguments());
    // skip the first argument of printf(Locale,String,Object...)
    if (ASTHelpers.isSameType(
        ASTHelpers.getType(args.get(0)), state.getTypeFromString(Locale.class.getName()), state)) {
      args = args.subList(1, args.size());
    }
    return args;
  }

  private FormatStringUtils() {}
}
