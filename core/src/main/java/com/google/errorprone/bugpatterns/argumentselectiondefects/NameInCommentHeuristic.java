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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment.MatchType;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Heuristic to detect if the name for the formal parameter has been used in the comment for an
 * actual parameter - if it has then we shouldn't swap that parameter.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
class NameInCommentHeuristic implements Heuristic {

  /**
   * Return true if there are no comments on the original actual parameter of a change which match
   * the name of the formal parameter.
   */
  @Override
  public boolean isAcceptableChange(
      Changes changes, Tree node, MethodSymbol symbol, VisitorState state) {
    // Now check to see if there is a comment in the position of any actual parameter we want to
    // change which matches the formal parameter
    ImmutableList<Commented<ExpressionTree>> comments = findCommentsForArguments(node, state);

    return changes.changedPairs().stream()
        .noneMatch(
            p -> {
              MatchType match =
                  NamedParameterComment.match(comments.get(p.formal().index()), p.formal().name())
                      .matchType();
              return match == MatchType.EXACT_MATCH || match == MatchType.APPROXIMATE_MATCH;
            });
  }

  private static ImmutableList<Commented<ExpressionTree>> findCommentsForArguments(
      Tree tree, VisitorState state) {
    switch (tree.getKind()) {
      case METHOD_INVOCATION:
        return Comments.findCommentsForArguments((MethodInvocationTree) tree, state);
      case NEW_CLASS:
        return Comments.findCommentsForArguments((NewClassTree) tree, state);
      default:
        throw new IllegalArgumentException(
            "Only MethodInvocationTree or NewClassTree is supported");
    }
  }
}
