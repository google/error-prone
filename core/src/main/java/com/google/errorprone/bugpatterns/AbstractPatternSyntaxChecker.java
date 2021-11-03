/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.annotation.CheckReturnValue;

/** Finds calls to regex-accepting methods with literal strings. */
@CheckReturnValue
abstract class AbstractPatternSyntaxChecker extends BugChecker
    implements MethodInvocationTreeMatcher {

  /*
   * Match invocations to regex-accepting methods. Subclasses will be consulted to see whether the
   * pattern passed to such methods are acceptable.
   *
   * <p>We deliberately omit Pattern.compile itself, as most of its users appear to be either
   * e.g. passing LITERAL flags or deliberately testing the regex compiler.
   */
  private static final Matcher<MethodInvocationTree> REGEX_USAGE =
      anyOf(
          instanceMethod()
              .onExactClass("java.lang.String")
              .namedAnyOf("matches", "split")
              .withParameters("java.lang.String"),
          instanceMethod()
              .onExactClass("java.lang.String")
              .named("split")
              .withParameters("java.lang.String", "int"),
          instanceMethod()
              .onExactClass("java.lang.String")
              .namedAnyOf("replaceFirst", "replaceAll")
              .withParameters("java.lang.String", "java.lang.String"),
          staticMethod().onClass("java.util.regex.Pattern").named("matches"),
          staticMethod().onClass("com.google.common.base.Splitter").named("onPattern"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!REGEX_USAGE.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree arg = tree.getArguments().get(0);
    String value = ASTHelpers.constValue(arg, String.class);
    if (value == null) {
      return NO_MATCH;
    }
    return matchRegexLiteral(tree, value);
  }

  @ForOverride
  protected abstract Description matchRegexLiteral(MethodInvocationTree tree, String pattern);
}
