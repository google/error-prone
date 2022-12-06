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
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;

/** Finds calls to regex-accepting methods with literal strings. */
@CheckReturnValue
public abstract class AbstractPatternSyntaxChecker extends BugChecker
    implements MethodInvocationTreeMatcher {

  /*
   * Match invocations to regex-accepting methods. Subclasses will be consulted to see whether the
   * pattern passed to such methods are acceptable.
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
          staticMethod()
              .onClass("java.util.regex.Pattern")
              .named("compile")
              .withParameters("java.lang.String"),
          staticMethod().onClass("com.google.common.base.Splitter").named("onPattern"));

  private static final Matcher<MethodInvocationTree> REGEX_USAGE_WITH_FLAGS =
      anyOf(
          staticMethod()
              .onClass("java.util.regex.Pattern")
              .named("compile")
              .withParameters("java.lang.String", "int"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (getMatcherWithoutFlags().matches(tree, state)) {
      String pattern = ASTHelpers.constValue(tree.getArguments().get(0), String.class);
      if (pattern != null) {
        return matchRegexLiteral(tree, state, pattern, 0);
      }
    } else if (getMatcherWithFlags().matches(tree, state)) {
      String pattern = ASTHelpers.constValue(tree.getArguments().get(0), String.class);
      Integer flags = ASTHelpers.constValue(tree.getArguments().get(1), Integer.class);
      if (pattern != null && flags != null) {
        return matchRegexLiteral(tree, state, pattern, flags);
      }
    }
    return NO_MATCH;
  }

  @ForOverride
  protected Matcher<? super MethodInvocationTree> getMatcherWithoutFlags() {
    return REGEX_USAGE;
  }

  @ForOverride
  protected Matcher<? super MethodInvocationTree> getMatcherWithFlags() {
    return REGEX_USAGE_WITH_FLAGS;
  }

  @ForOverride
  protected abstract Description matchRegexLiteral(
      MethodInvocationTree tree, VisitorState state, String pattern, int flags);
}
