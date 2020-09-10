/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.google.errorprone.util.Reachability.canCompleteNormally;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Matches always-default expressions in oneof switches. */
@BugPattern(
    name = "WrongOneof",
    severity = ERROR,
    summary = "This field is guaranteed not to be set given it's within a switch over a one_of.")
public final class WrongOneof extends BugChecker implements SwitchTreeMatcher {
  private static final TypePredicate ONE_OF_ENUM =
      isDescendantOf("com.google.protobuf.AbstractMessageLite.InternalOneOfEnum");

  private static final Matcher<ExpressionTree> PROTO_METHOD =
      instanceMethod().onDescendantOf("com.google.protobuf.MessageLite");

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    if (!ONE_OF_ENUM.apply(getType(tree.getExpression()), state)) {
      return NO_MATCH;
    }
    ExpressionTree expression = stripParentheses(tree.getExpression());
    if (!(expression instanceof MethodInvocationTree)) {
      return NO_MATCH;
    }
    ExpressionTree receiver = getReceiver(expression);
    if (receiver == null) {
      return NO_MATCH;
    }
    Optional<ImmutableList<Symbol>> receiverSymbolChain =
        symbolizeImmutableExpression(receiver, state);
    if (!receiverSymbolChain.isPresent()) {
      return NO_MATCH;
    }

    ImmutableSet<String> getters =
        enumValues(getType(tree.getExpression()).tsym).stream()
            .map(WrongOneof::getter)
            .collect(toImmutableSet());

    // Keep track of which getters might be set.
    Set<String> allowableGetters = new HashSet<>();
    for (CaseTree caseTree : tree.getCases()) {
      // Break out once we reach a default.
      if (caseTree.getExpression() == null) {
        break;
      }
      allowableGetters.add(
          getter(((IdentifierTree) caseTree.getExpression()).getName().toString()));

      scanForInvalidGetters(getters, allowableGetters, caseTree, receiverSymbolChain.get(), state);

      if (!caseTree.getStatements().isEmpty()
          && !canCompleteNormally(getLast(caseTree.getStatements()))) {
        allowableGetters.clear();
      }
    }
    return NO_MATCH;
  }

  /**
   * Returns a list of the methods called to get to this proto expression, as well as a terminating
   * variable.
   *
   * <p>Absent if the chain of calls is not a sequence of immutable proto getters ending in an
   * effectively final variable.
   *
   * <p>For example {@code a.getFoo().getBar()} would return {@code MethodSymbol[getFoo],
   * MethodSymbol[getBar], VarSymbol[a]}.
   */
  private static Optional<ImmutableList<Symbol>> symbolizeImmutableExpression(
      ExpressionTree tree, VisitorState state) {
    ImmutableList.Builder<Symbol> symbolized = ImmutableList.builder();
    ExpressionTree receiver = tree;
    while (true) {
      if (isPure(receiver, state)) {
        symbolized.add(getSymbol(receiver));
      } else {
        return Optional.empty();
      }
      if (receiver instanceof MethodInvocationTree || receiver instanceof MemberSelectTree) {
        receiver = getReceiver(receiver);
      } else {
        break;
      }
    }
    return Optional.of(symbolized.build());
  }

  private static boolean isPure(ExpressionTree receiver, VisitorState state) {
    if (receiver instanceof IdentifierTree) {
      Symbol symbol = getSymbol(receiver);
      return symbol instanceof VarSymbol && isConsideredFinal(symbol);
    }
    if (PROTO_METHOD.matches(receiver, state)) {
      // Ignore methods which take an argument, i.e. getters for repeated fields. We could check
      // that the argument is always the same, but...
      return ((MethodInvocationTree) receiver).getArguments().isEmpty();
    }
    return false;
  }

  private void scanForInvalidGetters(
      Set<String> getters,
      Set<String> allowableGetters,
      CaseTree caseTree,
      ImmutableList<Symbol> receiverSymbolChain,
      VisitorState state) {
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void unused) {
        ExpressionTree receiver = getReceiver(methodInvocationTree);
        if (receiver == null) {
          return super.visitMethodInvocation(methodInvocationTree, null);
        }
        if (!symbolizeImmutableExpression(receiver, state)
            .map(receiverSymbolChain::equals)
            .orElse(false)) {
          return super.visitMethodInvocation(methodInvocationTree, null);
        }
        String methodName =
            ((MemberSelectTree) methodInvocationTree.getMethodSelect()).getIdentifier().toString();
        if (!allowableGetters.contains(methodName) && getters.contains(methodName)) {
          state.reportMatch(
              buildDescription(methodInvocationTree)
                  .setMessage(
                      String.format(
                          "%s is guaranteed to return the default instance, given this is"
                              + " within a switch over a one_of.",
                          methodName))
                  .build());
        }
        return super.visitMethodInvocation(methodInvocationTree, null);
      }
    }.scan(new TreePath(state.getPath(), caseTree), null);
  }

  private static String getter(String enumCase) {
    return "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, enumCase);
  }
}
