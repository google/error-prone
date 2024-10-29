/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Both branches contain identical code", severity = ERROR)
public class DuplicateBranches extends BugChecker
    implements IfTreeMatcher, ConditionalExpressionTreeMatcher {
  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree tree, VisitorState state) {
    return match(tree, tree.getTrueExpression(), tree.getFalseExpression(), state);
  }

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    if (tree.getElseStatement() == null) {
      return NO_MATCH;
    }
    return match(tree, tree.getThenStatement(), tree.getElseStatement(), state);
  }

  // The comparison relies on Tree#toString, which isn't free for very long trees. Only check
  // relatively short trees.
  private static final int MAX_LENGTH_TO_COMPARE = 750;

  @SuppressWarnings("TreeToString")
  private Description match(Tree tree, Tree thenTree, Tree elseTree, VisitorState state) {
    if (state.getSourceForNode(thenTree).length() > MAX_LENGTH_TO_COMPARE
        || state.getSourceForNode(elseTree).length() > MAX_LENGTH_TO_COMPARE) {
      return NO_MATCH;
    }
    // This could do something similar to com.sun.tools.javac.comp.TreeDiffer. That doesn't
    // do exactly what we want here, which is to compare the syntax including of identifiers and
    // not their underlying symbols, and it would require a lot of case work to implement for all
    // AST nodes.
    if (!thenTree.toString().equals(elseTree.toString())) {
      return NO_MATCH;
    }
    int start = getStartPosition(elseTree);
    int end = state.getEndPosition(elseTree);
    boolean needsBraces = false;
    if (elseTree instanceof BlockTree) {
      needsBraces = !state.getPath().getParentPath().getLeaf().getKind().equals(Kind.BLOCK);
      var statements = ((BlockTree) elseTree).getStatements();
      start = getStartPosition(statements.get(0));
      end = state.getEndPosition(getLast(statements));
    }
    String comments =
        ErrorProneTokens.getTokens(
                state.getSourceCode().subSequence(getStartPosition(tree), start).toString(),
                getStartPosition(tree),
                state.context)
            .stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .map(c -> c.getText())
            .collect(joining("\n"));
    if (!comments.isEmpty()) {
      comments += "\n";
    }
    String replacement = comments + state.getSourceCode().subSequence(start, end);
    if (needsBraces) {
      replacement = "{\n" + replacement + "}";
    }
    return describeMatch(tree, SuggestedFix.replace(tree, replacement));
  }
}
