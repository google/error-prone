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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "FormatString",
    summary = "Invalid printf-style format string",
    category = JDK,
    severity = ERROR)
public class FormatString extends BugChecker implements MethodInvocationTreeMatcher {

  // TODO(cushon): add support for additional printf methods, maybe with an annotation
  private static final Matcher<ExpressionTree> FORMAT_METHOD =
      anyOf(
          instanceMethod().onDescendantOf("java.io.PrintStream").namedAnyOf("format", "printf"),
          instanceMethod().onDescendantOf("java.io.PrintWriter").namedAnyOf("format", "printf"),
          instanceMethod().onDescendantOf("java.util.Formatter").named("format"),
          staticMethod().onClass("java.lang.String").named("format"),
          staticMethod()
              .onClass("java.io.Console")
              .namedAnyOf("format", "printf", "readline", "readPassword"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!FORMAT_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    Deque<ExpressionTree> args = new ArrayDeque<>(tree.getArguments());
    // skip the first argument of printf(Locale,String,Object...)
    if (ASTHelpers.isSameType(
        ASTHelpers.getType(args.peekFirst()),
        state.getTypeFromString(Locale.class.getName()),
        state)) {
      args.removeFirst();
    }
    FormatStringValidation.ValidationResult result =
        FormatStringValidation.validate(sym, args, state);
    if (result == null) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).setMessage(result.message()).build();
  }
}
