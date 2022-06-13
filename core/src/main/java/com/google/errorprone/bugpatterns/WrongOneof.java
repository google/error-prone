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
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.google.errorprone.util.Reachability.canCompleteNormally;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.util.TreePath;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Matches always-default expressions in oneof switches. */
@BugPattern(
    severity = ERROR,
    summary = "This field is guaranteed not to be set given it's within a switch over a one_of.")
public final class WrongOneof extends BugChecker implements SwitchTreeMatcher {
  private static final TypePredicate ONE_OF_ENUM =
      isDescendantOf("com.google.protobuf.AbstractMessageLite.InternalOneOfEnum");

  private final ConstantExpressions constantExpressions;

  public WrongOneof(ErrorProneFlags flags) {
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

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
    constantExpressions
        .constantExpression(receiver, state)
        .ifPresent(constantReceiver -> processSwitch(tree, constantReceiver, state));
    return NO_MATCH;
  }

  private void processSwitch(
      SwitchTree tree, ConstantExpression constantReceiver, VisitorState state) {
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

      scanForInvalidGetters(getters, allowableGetters, caseTree, constantReceiver, state);

      List<? extends StatementTree> statements = caseTree.getStatements();
      if (statements != null
          && !statements.isEmpty()
          && !canCompleteNormally(getLast(statements))) {
        allowableGetters.clear();
      }
    }
  }

  private void scanForInvalidGetters(
      Set<String> getters,
      Set<String> allowableGetters,
      CaseTree caseTree,
      ConstantExpression receiverSymbolChain,
      VisitorState state) {
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void unused) {
        ExpressionTree receiver = getReceiver(methodInvocationTree);
        if (receiver == null) {
          return super.visitMethodInvocation(methodInvocationTree, null);
        }
        if (!constantExpressions
            .constantExpression(receiver, state)
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
