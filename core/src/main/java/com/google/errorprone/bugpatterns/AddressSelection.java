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
import java.util.Objects;
import java.util.function.Supplier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer InetAddress.getAllName to APIs that convert a hostname to a single IP address",
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
    if (Objects.equals(value, "localhost")) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(replacement);
    if (LOOPBACK.contains(value)) {
      description.addFix(fix.get());
    }
    return description.build();
  }
}
