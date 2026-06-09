/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.fixes.SuggestedFixes.renameVariableUsages;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes.VariableNamer;
import com.google.errorprone.util.ErrorProneComment;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import java.util.Optional;

/**
 * Utility methods for refactoring try-fail pattern to assertThrows, which is preferred. Used by
 * {@link TryFailRefactoring} and {@link MissingFail}.
 */
public final class AssertThrowsUtils {

  /**
   * Transforms a try-catch block in the try-fail pattern into a call to JUnit's {@code
   * assertThrows}, inserting the behavior of the {@code try} block into a lambda parameter, and
   * assigning the thrown exception to a variable, if it is used within the {@code catch} block. For
   * example:
   *
   * <pre>
   * try {
   *   foo();
   *   fail();
   * } catch (MyException thrown) {
   *   assertThat(thrown).isEqualTo(other);
   * }
   * </pre>
   *
   * becomes
   *
   * <pre>
   * {@code MyException thrown = assertThrows(MyException.class, () -> foo());}
   * assertThat(thrown).isEqualTo(other);
   * </pre>
   *
   * @param tryTree the tree representing the try-catch block to be refactored.
   * @param throwingStatements the list of statements in the {@code throw} clause, <b>excluding</b>
   *     the fail statement.
   * @param state current visitor state (for source positions).
   * @return an {@link Optional} containing a {@link Fix} that replaces {@code tryTree} with an
   *     equivalent {@code assertThrows}, if possible. Returns an {@code Optional.empty()} if a fix
   *     could not be constructed for the given code (e.g. multi-catch).
   */
  public static Optional<Fix> tryFailToAssertThrows(
      TryTree tryTree,
      List<? extends StatementTree> throwingStatements,
      VisitorState state,
      VariableNamer namer) {
    List<? extends CatchTree> catchTrees = tryTree.getCatches();
    if (catchTrees.size() != 1) {
      return Optional.empty();
    }
    CatchTree catchTree = Iterables.getOnlyElement(catchTrees);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder fixPrefix = new StringBuilder();
    ImmutableList<String> comments =
        state
            .getOffsetTokens(getStartPosition(tryTree.getBlock()), state.getEndPosition(tryTree))
            .stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .filter(comment -> !comment.getText().isEmpty())
            .filter(comment -> !comment.getText().matches("/\\*.*=\\s*\\*/"))
            .map(ErrorProneComment::getText)
            .collect(toImmutableList());
    if (!comments.isEmpty()) {
      fixPrefix.append(comments.stream().collect(joining("\n", "", "\n")));
    }
    String fixSuffix = "";
    if (throwingStatements.isEmpty()) {
      return Optional.empty();
    }
    List<? extends StatementTree> catchStatements = catchTree.getBlock().getStatements();
    fix.addStaticImport("org.junit.Assert.assertThrows");
    List<? extends Tree> resources = tryTree.getResources();
    if (!resources.isEmpty()) {
      fixPrefix.append(
          resources.stream().map(state::getSourceForNode).collect(joining("\n", "try (", ") {\n")));
      fixSuffix = "\n}";
    }
    // Hoist all but the last statement out of the lambda to narrow the exception scope.
    for (StatementTree statement : throwingStatements.subList(0, throwingStatements.size() - 1)) {
      fixPrefix.append(state.getSourceForNode(statement)).append("\n");
    }
    if (!catchStatements.isEmpty()) {
      String name = catchTree.getParameter().getName().toString();
      String newName = namer.avoidShadowing(name);
      if (!name.equals(newName)) {
        fix.merge(renameVariableUsages(catchTree.getParameter(), newName, state));
      }
      fixPrefix.append(String.format("var %s = ", newName));
    }
    fixPrefix.append(
        String.format(
            "assertThrows(%s.class, () -> ",
            state.getSourceForNode(catchTree.getParameter().getType())));
    StatementTree lastStatement = getLast(throwingStatements);
    Tree targetTree = lastStatement;
    if (targetTree instanceof ExpressionStatementTree expressionStatement) {
      targetTree = expressionStatement.getExpression();
    }
    if (targetTree instanceof AssignmentTree assignment) {
      targetTree = assignment.getExpression();
    }
    if (targetTree instanceof VariableTree variableTree && variableTree.getInitializer() != null) {
      targetTree = variableTree.getInitializer();
    }

    boolean useExpressionLambda = targetTree instanceof ExpressionTree;
    if (!useExpressionLambda) {
      fixPrefix.append("{");
    }
    fix.replace(getStartPosition(tryTree), getStartPosition(targetTree), fixPrefix.toString());
    if (useExpressionLambda) {
      fix.postfixWith(targetTree, ")");
    } else {
      fix.postfixWith(targetTree, "});");
    }
    if (catchStatements.isEmpty()) {
      fix.replace(state.getEndPosition(lastStatement), state.getEndPosition(tryTree), fixSuffix);
    } else {
      fix.replace(
          /* startPos= */ state.getEndPosition(lastStatement),
          /* endPos= */ getStartPosition(catchStatements.getFirst()),
          "\n");
      fix.replace(
          state.getEndPosition(getLast(catchStatements)), state.getEndPosition(tryTree), fixSuffix);
    }
    return Optional.of(fix.build());
  }

  private AssertThrowsUtils() {}
}
