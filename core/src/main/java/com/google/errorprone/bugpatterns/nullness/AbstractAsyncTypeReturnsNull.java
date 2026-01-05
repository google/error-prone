/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Streams.concat;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.fixes.SuggestedFix.emptyFix;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.stream.Stream;

/**
 * Superclass for checks that implementations of null-hostile abstract methods do not return
 * definitely null values.
 */
abstract class AbstractAsyncTypeReturnsNull extends BugChecker
    implements ReturnTreeMatcher, LambdaExpressionTreeMatcher {
  private final Class<?> asyncClass;

  AbstractAsyncTypeReturnsNull(Class<?> asyncClass) {
    this.asyncClass = asyncClass;
  }

  @Override
  public final Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (!(tree.getBody() instanceof ExpressionTree body)
        || !hasDefinitelyNullBranch(
            body,
            /* definitelyNullVars= */ ImmutableSet.of(),
            /* varsProvenNullByParentIf= */ ImmutableSet.of(),
            state)) {
      return NO_MATCH;
    }
    Type functionalInterfaceType = ASTHelpers.getType(tree);
    if (functionalInterfaceType == null) {
      return NO_MATCH;
    }

    if (!lambdaOverridesAsyncMethod(tree, state)) {
      return NO_MATCH;
    }

    return describeMatch(tree, provideFix(body));
  }

  @Override
  public final Description matchReturn(ReturnTree tree, VisitorState state) {
    if (tree.getExpression() == null
        || !hasDefinitelyNullBranch(
            tree.getExpression(),
            // TODO: cpovirk - Maybe populate sets to pass here and even above.
            /* definitelyNullVars= */ ImmutableSet.of(),
            /* varsProvenNullByParentIf= */ ImmutableSet.of(),
            state)) {
      return NO_MATCH;
    }
    Type asyncType = state.getTypeFromString(asyncClass.getName());
    boolean matches =
        stream(state.getPath())
            .map(
                t ->
                    switch (t) {
                      case MethodTree methodTree ->
                          findSuperMethods(getSymbol(methodTree), state.getTypes()).stream()
                              .anyMatch(
                                  superMethod ->
                                      superMethod.owner != null
                                          && isSameType(superMethod.owner.type, asyncType, state));
                      case LambdaExpressionTree lambdaTree ->
                          lambdaOverridesAsyncMethod(lambdaTree, state);
                      default -> null;
                    })
            .filter(match -> match != null)
            .findFirst()
            .orElse(false);
    return matches ? describeMatch(tree, provideFix(tree.getExpression())) : NO_MATCH;
  }

  private boolean lambdaOverridesAsyncMethod(LambdaExpressionTree lambdaTree, VisitorState state) {
    Type functionalInterfaceType = ASTHelpers.getType(lambdaTree);
    if (functionalInterfaceType == null) {
      return false;
    }

    var descriptorSymbol = state.getTypes().findDescriptorSymbol(functionalInterfaceType.tsym);

    Type asyncType = state.getTypeFromString(asyncClass.getName());
    return (descriptorSymbol instanceof MethodSymbol ms)
        && concat(
                Stream.of(ms),
                streamSuperMethods((MethodSymbol) descriptorSymbol, state.getTypes()))
            .anyMatch(
                superMethod ->
                    superMethod.owner != null
                        && isSameType(superMethod.owner.type, asyncType, state));
  }

  protected SuggestedFix provideFix(ExpressionTree tree) {
    return tree.getKind() == NULL_LITERAL
        ? SuggestedFix.builder()
            .replace(tree, "immediateFuture(null)")
            .addStaticImport(Futures.class.getName() + ".immediateFuture")
            .build()
        /*
         * TODO: cpovirk - Provide a variant of hasDefinitelyNullBranch that returns a Set<Tree> of
         * definitely null locations, and then generate a fix for those locations.
         */
        : emptyFix();
  }
}
