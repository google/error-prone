/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.common.base.Predicates.notNull;
import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.variableType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;

/** @author Ron Shapiro */
@BugPattern(
    name = "AndroidInjectionBeforeSuper",
    summary =
        "AndroidInjection.inject() should always be invoked before calling super.lifecycleMethod()",
    category = DAGGER,
    severity = ERROR)
public final class AndroidInjectionBeforeSuper extends BugChecker implements MethodTreeMatcher {

  private enum MatchType {
    ACTIVITY(
        "android.app.Activity",
        "onCreate",
        ImmutableList.of(variableType(isSameType("android.os.Bundle"))),
        "dagger.android.AndroidInjection"),
    FRAMEWORK_FRAGMENT(
        "android.app.Fragment",
        "onAttach",
        ImmutableList.of(variableType(isSameType("android.content.Context"))),
        "dagger.android.AndroidInjection"),
    FRAMEWORK_FRAGMENT_PRE_API23(
        "android.app.Fragment",
        "onAttach",
        ImmutableList.of(variableType(isSameType("android.app.Activity"))),
        "dagger.android.AndroidInjection"),
    SUPPORT_FRAGMENT(
        "android.support.v4.app.Fragment",
        "onAttach",
        ImmutableList.of(variableType(isSameType("android.content.Context"))),
        "dagger.android.support.AndroidSupportInjection"),
    SUPPORT_FRAGMENT_PRE_API23(
        "android.support.v4.app.Fragment",
        "onAttach",
        ImmutableList.of(variableType(isSameType("android.app.Activity"))),
        "dagger.android.support.AndroidSupportInjection"),
    SERVICE(
        "android.app.Service", "onCreate", ImmutableList.of(), "dagger.android.AndroidInjection"),
    ;

    private final String lifecycleMethod;
    private final Matcher<MethodTree> methodMatcher;
    private final MethodNameMatcher methodInvocationMatcher;
    private final ParameterMatcher injectMethodMatcher;

    MatchType(
        String componentType,
        String lifecycleMethod,
        ImmutableList<Matcher<VariableTree>> lifecycleMethodParameters,
        String staticMethodClass) {
      this.lifecycleMethod = lifecycleMethod;
      methodMatcher =
          allOf(
              methodIsNamed(lifecycleMethod),
              methodHasParameters(lifecycleMethodParameters),
              enclosingClass(isSubtypeOf(componentType)));
      methodInvocationMatcher =
          instanceMethod().onDescendantOf(componentType).named(lifecycleMethod);
      injectMethodMatcher =
          staticMethod().onClass(staticMethodClass).named("inject").withParameters(componentType);
    }
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    for (MatchType matchType : MatchType.values()) {
      if (matchType.methodMatcher.matches(tree, state)) {
        return tree.accept(new LifecycleMethodVisitor(matchType, state), null);
      }
    }
    return Description.NO_MATCH;
  }

  private final class LifecycleMethodVisitor extends SimpleTreeVisitor<Description, Void> {
    private final MatchType matchType;
    private final VisitorState state;

    LifecycleMethodVisitor(MatchType matchType, VisitorState state) {
      this.matchType = matchType;
      this.state = state;
    }

    private boolean foundSuper = false;

    @Override
    public Description visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      if (foundSuper && matchType.injectMethodMatcher.matches(node, state)) {
        return buildDescription(node)
            .setMessage(
                String.format(
                    "AndroidInjection.inject() should always be invoked before calling super.%s()",
                    matchType.lifecycleMethod))
            .build();
      } else if (matchType.methodInvocationMatcher.matches(node, state)) {
        foundSuper = true;
      }
      return null;
    }

    @Override
    public Description visitMethod(MethodTree node, Void aVoid) {
      BlockTree methodBody = node.getBody();
      if (methodBody == null) {
        return Description.NO_MATCH;
      }

      return methodBody.getStatements().stream()
          .map(tree -> tree.accept(this, null))
          .filter(notNull())
          .findFirst()
          .orElse(Description.NO_MATCH);
    }

    @Override
    public Description visitExpressionStatement(ExpressionStatementTree node, Void aVoid) {
      return node.getExpression().accept(this, null);
    }
  }
}
