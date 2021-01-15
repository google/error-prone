/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.sameVariable;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

/**
 * A refactoring to replace Optional.get() with lambda arg in expressions passed as arg to member
 * functions of Optionals.
 */
@BugPattern(
    name = "UnnecessaryOptionalGet",
    summary =
        "This code can be simplified by directly using the lambda parameters instead of calling"
            + " get..() on optional.",
    severity = WARNING)
public final class UnnecessaryOptionalGet extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> OPTIONAL_FUNCTIONS_WITH_FUNCTIONAL_ARG =
      anyOf(
          instanceMethod()
              .onExactClass("java.util.Optional")
              .namedAnyOf("map", "filter", "ifPresent", "flatMap"),
          instanceMethod().onExactClass("java.util.OptionalLong").named("ifPresent"),
          instanceMethod().onExactClass("java.util.OptionalInt").named("ifPresent"),
          instanceMethod().onExactClass("java.util.OptionalDouble").named("ifPresent"),
          instanceMethod().onExactClass("com.google.common.base.Optional").named("transform"));

  private static final Matcher<ExpressionTree> OPTIONAL_GET =
      anyOf(
          instanceMethod()
              .onExactClass("java.util.Optional")
              .namedAnyOf("get", "orElse", "orElseGet", "orElseThrow"),
          instanceMethod()
              .onExactClass("java.util.OptionalLong")
              .namedAnyOf("getAsLong", "orElse", "orElseGet", "orElseThrow"),
          instanceMethod()
              .onExactClass("java.util.OptionalInt")
              .namedAnyOf("getAsInt", "orElse", "orElseGet", "orElseThrow"),
          instanceMethod()
              .onExactClass("java.util.OptionalDouble")
              .namedAnyOf("getAsDouble", "orElse", "orElseGet", "orElseThrow"),
          instanceMethod()
              .onExactClass("com.google.common.base.Optional")
              .namedAnyOf("get", "or", "orNull"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!OPTIONAL_FUNCTIONS_WITH_FUNCTIONAL_ARG.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree onlyArg = getOnlyElement(tree.getArguments());
    if (!onlyArg.getKind().equals(Kind.LAMBDA_EXPRESSION)) {
      return Description.NO_MATCH;
    }
    VariableTree arg = getOnlyElement(((LambdaExpressionTree) onlyArg).getParameters());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    new TreeScanner<Void, VisitorState>() {
      @Override
      public Void visitMethodInvocation(
          MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
        if (OPTIONAL_GET.matches(methodInvocationTree, visitorState)
            && sameVariable(getReceiver(tree), getReceiver(methodInvocationTree))) {
          fix.replace(methodInvocationTree, state.getSourceForNode(arg));
        }
        return super.visitMethodInvocation(methodInvocationTree, visitorState);
      }
    }.scan(onlyArg, state);
    if (fix.isEmpty()) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, fix.build());
  }
}
