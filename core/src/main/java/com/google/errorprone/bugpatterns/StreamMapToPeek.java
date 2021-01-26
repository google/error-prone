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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** Looks for invocations of {@link Stream#map} that can be simplified to {@link Stream#peek}. */
@BugPattern(
    name = "StreamMapToPeek",
    summary = "If Stream.map can be replaced with Stream.peek, prefer that",
    severity = SeverityLevel.WARNING)
public class StreamMapToPeek extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> STREAM_MAP_INVOCATION =
      Matchers.instanceMethod().onExactClass("java.util.stream.Stream").named("map");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // Make sure the method is Stream.map.
    if (!STREAM_MAP_INVOCATION.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // Make sure the parameter is a lambda.
    ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
    if (arg.getKind() != Kind.LAMBDA_EXPRESSION) {
      return Description.NO_MATCH;
    }

    // Make sure it's a statement lambda.
    LambdaExpressionTree lambda = (LambdaExpressionTree) arg;
    if (lambda.getBodyKind() != BodyKind.STATEMENT) {
      return Description.NO_MATCH;
    }

    // Make sure the lambda isn't doing type conversion.
    Type lambdaType = ASTHelpers.getType(lambda);
    checkNotNull(lambdaType, "type analysis failed");
    List<Type> typeArguments = lambdaType.getTypeArguments();
    if (typeArguments.size() != 2) {
      // No type arguments -- perhaps raw Stream?
      return Description.NO_MATCH;
    }
    if (!state.getTypes().isSameType(typeArguments.get(0), typeArguments.get(1))) {
      return Description.NO_MATCH;
    }

    // Make sure the last statement in the block is a return (it could instead be
    // e.g. a throws statement).
    BlockTree block = (BlockTree) lambda.getBody();
    StatementTree lastStatement = Iterables.getLast(block.getStatements());
    if (lastStatement.getKind() != Kind.RETURN) {
      return Description.NO_MATCH;
    }

    // Make sure there's no other return statement.
    if (countReturns(block) != 1) {
      return Description.NO_MATCH;
    }

    // Make sure the return expression is just the parameter of the lambda expression.
    ReturnTree returnStmt = (ReturnTree) lastStatement;
    if (returnStmt.getExpression().getKind() != Kind.IDENTIFIER) {
      return Description.NO_MATCH;
    }
    IdentifierTree identifier = (IdentifierTree) returnStmt.getExpression();
    VariableTree var = Iterables.getOnlyElement(lambda.getParameters());
    if (!identifier.getName().equals(var.getName())) {
      return Description.NO_MATCH;
    }

    // Make sure the parameter isn't assigned in the block.
    if (isAssigned(block, identifier.getName())) {
      return Description.NO_MATCH;
    }

    // Create suggestion.
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .merge(SuggestedFixes.renameMethodInvocation(tree, "peek", state))
            .delete(returnStmt)
            .build());
  }

  private static final TreeScanner<Integer, Void> COUNT_RETURNS =
      new TreeScanner<Integer, Void>() {
        @Override
        public Integer visitReturn(ReturnTree returnTree, Void aVoid) {
          return 1;
        }

        @Override
        public Integer reduce(Integer a, Integer b) {
          return firstNonNull(a, 0) + firstNonNull(b, 0);
        }
      };

  /**
   * Counts the number of returns in a tree. Note this will overcount when the tree includes nested
   * structures with inner returns.
   */
  private static int countReturns(Tree tree) {
    return COUNT_RETURNS.scan(tree, null);
  }

  /** Returns true if the parameter is assigned within the tree. */
  private static boolean isAssigned(Tree tree, Name param) {
    return new TreeScanner<Boolean, Void>() {
      @Override
      public Boolean visitAssignment(AssignmentTree assignmentTree, Void aVoid) {
        ExpressionTree variable = assignmentTree.getVariable();
        return variable.getKind() == Kind.IDENTIFIER
            && ((IdentifierTree) variable).getName().equals(param);
      }

      @Override
      public Boolean reduce(Boolean a, Boolean b) {
        return firstNonNull(a, false) || firstNonNull(b, false);
      }
    }.scan(tree, null);
  }
}
