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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import java.util.List;
import java.util.Optional;

/**
 * Utility methods for refactoring try-fail pattern to assertThrows, which is preferred. Used by
 * {@link TryFailRefactoring} and {@link MissingFail}.
 */
public class AssertThrowsUtils {

  /**
   * Transforms a try-catch block in the try-fail pattern into a call to JUnit's {@code
   * assertThrows}, inserting the behavior of the {@code try} block into a lambda parameter, and
   * assigning the expected exception to a variable, if it is used within the {@code catch} block.
   * For example:
   *
   * <pre>
   * try {
   *   foo();
   *   fail();
   * } catch (MyException expected) {
   *   assertThat(expected).isEqualTo(other);
   * }
   * </pre>
   *
   * becomes
   *
   * <pre>
   * {@code MyException expected = assertThrows(MyException.class, () -> foo());}
   * assertThat(expected).isEqualTo(other);
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
      Optional<Tree> failureMessage,
      VisitorState state) {
    List<? extends CatchTree> catchTrees = tryTree.getCatches();
    if (catchTrees.size() != 1) {
      return Optional.empty();
    }
    CatchTree catchTree = Iterables.getOnlyElement(catchTrees);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder fixPrefix = new StringBuilder();
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
    if (!catchStatements.isEmpty()) {
      // TODO(cushon): pick a fresh name for the variable, if necessary
      fixPrefix.append(String.format("%s = ", state.getSourceForNode(catchTree.getParameter())));
    }
    fixPrefix.append(
        String.format(
            "assertThrows(%s%s.class, () -> ",
            failureMessage
                // Supplying a constant string adds little value, since a failure here always means
                // the same thing: the method just called wasn't expected to complete normally, but
                // it did.
                .filter(t -> ASTHelpers.constValue(t, String.class) == null)
                .map(t -> state.getSourceForNode(t) + ", ")
                .orElse(""),
            state.getSourceForNode(catchTree.getParameter().getType())));
    boolean useExpressionLambda =
        throwingStatements.size() == 1
            && getOnlyElement(throwingStatements).getKind() == Kind.EXPRESSION_STATEMENT;
    if (!useExpressionLambda) {
      fixPrefix.append("{");
    }
    fix.replace(
        getStartPosition(tryTree),
        getStartPosition(throwingStatements.iterator().next()),
        fixPrefix.toString());
    if (useExpressionLambda) {
      fix.postfixWith(
          ((ExpressionStatementTree) throwingStatements.iterator().next()).getExpression(), ")");
    } else {
      fix.postfixWith(getLast(throwingStatements), "});");
    }
    if (catchStatements.isEmpty()) {
      fix.replace(
          state.getEndPosition(getLast(throwingStatements)),
          state.getEndPosition(tryTree),
          fixSuffix);
    } else {
      fix.replace(
          /* startPos= */ state.getEndPosition(getLast(throwingStatements)),
          /* endPos= */ getStartPosition(catchStatements.get(0)),
          "\n");
      fix.replace(
          state.getEndPosition(getLast(catchStatements)), state.getEndPosition(tryTree), fixSuffix);
    }
    return Optional.of(fix.build());
  }
}
