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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Checks if {@code Optional#of} is chained with a redundant method.
 *
 * <p>{@code Optional#of} will always return a non-empty optional. Using any of the following
 * methods:
 *
 * <ul>
 *   <li>{@code isPresent}
 *   <li>{@code ifPresent}
 *   <li>{@code orElse}
 *   <li>{@code orElseGet}
 *   <li>{@code orElseThrow}
 *   <li>{@code or} (only for Guava optionals)
 *   <li>{@code orNull} (only for Guava optionals)
 * </ul>
 *
 * on it is unnecessary and a potential source of bugs.
 */
@BugPattern(
    name = "OptionalOfRedundantMethod",
    summary =
        "Optional.of() always returns a non-empty optional. Using"
            + " ifPresent/isPresent/orElse/orElseGet/orElseThrow/isPresent/or/orNull method on it"
            + " is unnecessary and most probably a bug.",
    severity = ERROR)
public class OptionalOfRedundantMethod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> OPTIONAL_OF_MATCHER =
      anyOf(
          staticMethod().onClass("java.util.Optional").named("of"),
          staticMethod().onClass("com.google.common.base.Optional").named("of"));

  private static final Matcher<ExpressionTree> REDUNDANT_METHOD_MATCHER =
      anyOf(
          instanceMethod()
              .onExactClass("java.util.Optional")
              .namedAnyOf("ifPresent", "isPresent", "orElse", "orElseGet", "orElseThrow"),
          instanceMethod()
              .onExactClass("com.google.common.base.Optional")
              .namedAnyOf("isPresent", "or", "orNull"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree childMethodInvocationTree = ASTHelpers.getReceiver(tree);
    if (!(childMethodInvocationTree instanceof MethodInvocationTree)) {
      return NO_MATCH;
    }
    if (!OPTIONAL_OF_MATCHER.matches((MethodInvocationTree) childMethodInvocationTree, state)
        || !REDUNDANT_METHOD_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    String methodName = ASTHelpers.getSymbol(tree).getSimpleName().toString();
    return buildDescription(tree)
        .setMessage(
            String.format(
                "Optional.of() always returns a non-empty Optional. Using '%s' method on it is"
                    + " unnecessary and most probably a bug.",
                methodName))
        .build();
  }
}
