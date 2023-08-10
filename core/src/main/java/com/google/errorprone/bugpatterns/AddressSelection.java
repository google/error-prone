/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodInvocation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.function.Supplier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer InetAddress.getAllByName to APIs that convert a hostname to a single IP address",
    severity = WARNING)
public final class AddressSelection extends BugChecker
    implements NewClassTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTRUCTORS =
      Matchers.anyOf(
          constructor().forClass("java.net.Socket").withParameters("java.lang.String", "int"),
          constructor()
              .forClass("java.net.InetSocketAddress")
              .withParameters("java.lang.String", "int"));
  private static final Matcher<ExpressionTree> METHODS =
      staticMethod()
          .onClass("java.net.InetAddress")
          .named("getByName")
          .withParameters("java.lang.String");

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!CONSTRUCTORS.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree argument = tree.getArguments().get(0);
    return handleMatch(
        argument,
        argument,
        () -> {
          SuggestedFix.Builder fix = SuggestedFix.builder();
          fix.replace(
              argument, qualifyType(state, fix, "java.net.InetAddress") + ".getLoopbackAddress()");
          return fix.build();
        });
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!METHODS.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree argument = getOnlyElement(tree.getArguments());
    return handleMatch(
        argument,
        tree,
        () ->
            SuggestedFix.builder()
                .merge(renameMethodInvocation(tree, "getLoopbackAddress", state))
                .delete(argument)
                .build());
  }

  private static final ImmutableSet<String> LOOPBACK = ImmutableSet.of("127.0.0.1", "::1");

  private Description handleMatch(
      ExpressionTree argument, ExpressionTree replacement, Supplier<SuggestedFix> fix) {
    String value = constValue(argument, String.class);
    // If it's a numeric loopback address, suggest using the method for that.
    if (LOOPBACK.contains(value)) {
      return describeMatch(replacement, fix.get());
    }
    // If it isn't a constant, or it's "localhost", or it looks like a numeric IP address, then
    // we don't say anything.
    if (value == null || value.equals("localhost") || isNumericIp(value)) {
      return NO_MATCH;
    }
    // Otherwise flag it but don't suggest a fix.
    return describeMatch(replacement);
  }

  /**
   * Returns true if this string looks like it might be a numeric IP address. The matching here is
   * very approximate. We want every numeric IP address to return true, but it's OK if some strings
   * return true even though they are not actually valid numeric IP addresses. Actually parsing
   * numeric IPv6 addresses in all their glory is more than we need here.
   */
  private static boolean isNumericIp(String value) {
    if (value.isEmpty()) {
      return false;
    }
    if (value.contains(":")) {
      return true; // Every numeric IPv6 address contains a colon and no non-numeric hostname does.
    }
    return ASCII_DIGIT.matches(value.charAt(0))
        && ASCII_DIGIT.matches(value.charAt(value.length() - 1));
    // Every numeric IPv4 address begins and ends with an ASCII digit.
  }

  private static final CharMatcher ASCII_DIGIT = CharMatcher.inRange('0', '9');
}
