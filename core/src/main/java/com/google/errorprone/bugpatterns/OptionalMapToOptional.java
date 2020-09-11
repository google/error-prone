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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;

/** Matches {@code Optional#map} mapping to another {@code Optional}. */
@BugPattern(
    name = "OptionalMapToOptional",
    summary = "Mapping to another Optional will yield a nested Optional. Did you mean flatMap?",
    severity = WARNING)
public final class OptionalMapToOptional extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MAP =
      anyOf(
          instanceMethod().onExactClass("java.util.Optional").named("map"),
          instanceMethod().onExactClass("com.google.common.base.Optional").named("transform"));

  private static final Matcher<ExpressionTree> ANYTHING_BUT_ISPRESENT =
      anyOf(
          allOf(
              instanceMethod().onExactClass("java.util.Optional"),
              not(instanceMethod().onExactClass("java.util.Optional").named("isPresent"))),
          allOf(
              instanceMethod().onExactClass("com.google.common.base.Optional"),
              not(
                  instanceMethod()
                      .onExactClass("com.google.common.base.Optional")
                      .named("isPresent"))));

  private static final TypePredicate OPTIONAL =
      TypePredicates.isDescendantOfAny(
          ImmutableList.of("java.util.Optional", "com.google.common.base.Optional"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MAP.matches(tree, state)) {
      return NO_MATCH;
    }
    TreePath path = state.getPath();
    // Heuristic: if another Optional instance method is invoked on this, it's usually clear what's
    // going on, unless that method is `isPresent()`.
    if (path.getParentPath().getLeaf() instanceof MemberSelectTree
        && path.getParentPath().getParentPath().getLeaf() instanceof MethodInvocationTree
        && ANYTHING_BUT_ISPRESENT.matches(
            (MethodInvocationTree) path.getParentPath().getParentPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    TargetType targetType =
        ASTHelpers.targetType(
            state.withPath(new TreePath(state.getPath(), tree.getArguments().get(0))));
    if (targetType == null) {
      return NO_MATCH;
    }
    List<Type> typeArguments = targetType.type().getTypeArguments();
    // If the receiving Optional is raw, so will the target type of Function.
    if (typeArguments.isEmpty()) {
      return NO_MATCH;
    }
    Type typeMappedTo = typeArguments.get(1);
    if (!OPTIONAL.apply(typeMappedTo, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }
}
