/*
 * Copyright 2019 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageLite;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utility class to find calls "nearby" other calls.
 *
 * <p>TODO(glorioso): Coalesce this with ByteBufferBackingArray since they have similar aims?
 */
public class NearbyCallers {
  private NearbyCallers() {}

  // lifted from com.google.devtools.javatools.refactory.refaster.cleanups.proto.ProtoMatchers
  private static final Matcher<ExpressionTree> IS_IMMUTABLE_PROTO_GETTER =
      instanceMethod()
          .onDescendantOfAny(GeneratedMessage.class.getName(), GeneratedMessageLite.class.getName())
          .withNameMatching(Pattern.compile("get(?!CachedSize$|SerializedSize$).+"))
          .withParameters();

  /**
   * Returns whether or not there is a call matching {@code secondaryMethodMatcher} with the same
   * receiver as the method call represented by {@code primaryMethod} in "nearby" code.
   *
   * <p>This is generally used to ensure that developers calling {@code primaryMethod} should likely
   * also be checking {@code secondaryMethodMatcher} (as a signal that they understand the
   * edge-cases of {@code primaryMethod}).
   *
   * @param primaryMethod the tree that contains the primary method call (e.g.: what might have
   *     weird edge-cases).
   * @param secondaryMethodMatcher A matcher to identify the secondary method call (e.g.: that shows
   *     a demonstration of the knowledge of the edge-cases in {@code primaryMethod})
   * @param state the visitor state of this matcher
   * @param checkProtoChains whether or not to check a chain of protobuf getter methods to see if
   *     it's the 'same receiver' (e.g., {@code a.getB().getC().getSeconds()} and {@code
   *     a.getB().getC().getNanos()}
   */
  static boolean containsCallToSameReceiverNearby(
      MethodInvocationTree primaryMethod,
      Matcher<ExpressionTree> secondaryMethodMatcher,
      VisitorState state,
      boolean checkProtoChains) {
    ExpressionTree primaryMethodReceiver = ASTHelpers.getReceiver(primaryMethod);
    TreeScanner<Boolean, Void> scanner =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean reduce(Boolean r1, Boolean r2) {
            return firstNonNull(r1, Boolean.FALSE) || firstNonNull(r2, Boolean.FALSE);
          }

          @Override
          public Boolean visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            return false;
          }

          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree secondaryMethod, Void unused) {
            if (super.visitMethodInvocation(secondaryMethod, unused)) {
              return true;
            }
            if (secondaryMethod == null
                || !secondaryMethodMatcher.matches(secondaryMethod, state)) {
              return false;
            }

            ExpressionTree secondaryMethodReceiver = ASTHelpers.getReceiver(secondaryMethod);
            if (secondaryMethodReceiver == null) {
              return false;
            }

            // if the methods are being invoked directly on the same variable...
            if (primaryMethodReceiver != null
                && ASTHelpers.sameVariable(primaryMethodReceiver, secondaryMethodReceiver)) {
              return true;
            }

            // If we're checking proto chains, look for the root variables and see if they're the
            // same.
            return checkProtoChains && protoChainsMatch(primaryMethod, secondaryMethod);
          }

          private boolean protoChainsMatch(
              MethodInvocationTree primaryMethod, MethodInvocationTree secondaryMethod) {
            ExpressionTree primaryRootAssignable = ASTHelpers.getRootAssignable(primaryMethod);
            ExpressionTree secondaryRootAssignable = ASTHelpers.getRootAssignable(secondaryMethod);
            if (primaryRootAssignable == null
                || secondaryRootAssignable == null
                || !ASTHelpers.sameVariable(primaryRootAssignable, secondaryRootAssignable)) {
              return false;
            }

            // build up a list of method invocations for both invocations
            return buildProtoGetterChain(primaryMethod, state)
                .flatMap(
                    primaryChain ->
                        buildProtoGetterChain(secondaryMethod, state).map(primaryChain::equals))
                .orElse(false);
          }
        };
    ImmutableList<Tree> treesToScan = getNearbyTreesToScan(state);
    return !treesToScan.isEmpty() && scanner.scan(treesToScan, null);
  }

  // Return the chain of receivers from expr (intended to be a MethodInvocation) so long
  // as it's entirely composed of proto getters, followed by a terminal identifier, e.g.:
  //
  // FooProto x = ...;
  // String value = x.getA().getB().getC().expr()
  //
  // the chain would be [getC(), getB(), getA(), x]
  private static Optional<ImmutableList<Symbol>> buildProtoGetterChain(
      ExpressionTree expr, VisitorState state) {
    ImmutableList.Builder<Symbol> symbolChain = ImmutableList.builder();
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      // if the method isn't an immutable protobuf getter, return false
      if (!IS_IMMUTABLE_PROTO_GETTER.matches(expr, state)) {
        return Optional.empty();
      }
      if (expr instanceof JCFieldAccess) {
        expr = ((JCFieldAccess) expr).getExpression();
      }
      symbolChain.add(ASTHelpers.getSymbol(expr));
    }
    return Optional.of(symbolChain.build());
  }

  private static ImmutableList<Tree> getNearbyTreesToScan(VisitorState state) {
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case BLOCK:
          // if we reach a block tree, then _only_ scan that block
          return ImmutableList.of(parent);

        case LAMBDA_EXPRESSION:
          // if we reach a lambda tree, then don't scan anything since we don't know where/when that
          // lambda will actually be executed.
          // TODO(glorioso): for simple expression lambdas, consider looking for use sites and scan
          // *those* sites, but binding the lambda variable to its use site might be rough :(

          // e.g.:
          //   Function<Duration, Long> NANOS = d -> d.getNano();
          //   Function<Duration, Long> SECONDS = d -> d.getSeconds();
          //   ...
          //   long nanos = NANOS.apply(myDuration) + SECONDS.apply(myDuration) * 1_000_000L;
          //
          // how do we track myDuration through both layers?
          return ImmutableList.of();

        case CLASS:
          // if we get all the way up to the class tree, then _only_ scan the other class-level
          // fields
          ImmutableList.Builder<Tree> treesToScan = ImmutableList.builder();
          for (Tree member : ((ClassTree) parent).getMembers()) {
            if (member instanceof VariableTree) {
              ExpressionTree expressionTree = ((VariableTree) member).getInitializer();
              if (expressionTree != null) {
                treesToScan.add(expressionTree);
              }
            }
          }
          return treesToScan.build();

        default:
          // fall out, continue searching up the tree
      }
    }
    return ImmutableList.of();
  }
}
