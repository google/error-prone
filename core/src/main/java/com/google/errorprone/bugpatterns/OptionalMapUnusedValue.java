/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/** Replaces {@code Optional.map} with {@code Optional.ifPresent} if the value is unused. */
@BugPattern(
    name = "OptionalMapUnusedValue",
    summary = "Optional.ifPresent is preferred over Optional.map when the return value is unused",
    severity = WARNING)
public final class OptionalMapUnusedValue extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> METHOD_IS_OPTIONAL_MAP =
      instanceMethod().onExactClass("java.util.Optional").named("map");

  private static final Matcher<ExpressionTree> PARENT_IS_STATEMENT =
      parentNode(kindIs(Kind.EXPRESSION_STATEMENT));

  private static final Matcher<MethodInvocationTree> ARG_IS_VOID_COMPATIBLE =
      argument(
          0, anyOf(kindIs(Kind.MEMBER_REFERENCE), OptionalMapUnusedValue::isVoidCompatibleLambda));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (METHOD_IS_OPTIONAL_MAP.matches(tree, state)
        && PARENT_IS_STATEMENT.matches(tree, state)
        && ARG_IS_VOID_COMPATIBLE.matches(tree, state)) {
      return describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "ifPresent", state));
    }
    return NO_MATCH;
  }

  // TODO(b/170476239): Cover all the cases in which the argument is void-compatible, see
  // JLS 15.12.2.1
  private static boolean isVoidCompatibleLambda(ExpressionTree tree, VisitorState state) {
    if (tree instanceof LambdaExpressionTree) {
      LambdaExpressionTree lambdaTree = (LambdaExpressionTree) tree;
      if (lambdaTree.getBodyKind().equals(LambdaExpressionTree.BodyKind.EXPRESSION)) {
        return kindIs(Kind.METHOD_INVOCATION).matches(lambdaTree.getBody(), state);
      }
    }
    return false;
  }
}
