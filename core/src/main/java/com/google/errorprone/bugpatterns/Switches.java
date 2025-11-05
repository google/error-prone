/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.isSwitchDefault;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import java.util.regex.Pattern;

final class Switches {
  private static final Pattern SKEW_PATTERN =
      Pattern.compile("\\bskew\\b", Pattern.CASE_INSENSITIVE);

  /**
   * Whether the default case for the given switch is a fallback for potential version skew.
   *
   * <p>This is purely a heuristic that looks for the word "skew" in the comment of the default
   * case.
   */
  static boolean isDefaultCaseForSkew(
      SwitchTree switchTree, CaseTree defaultCase, VisitorState state) {
    checkArgument(isSwitchDefault(defaultCase));

    int indexOfDefault = switchTree.getCases().indexOf(defaultCase);

    // Start position will either be from the end of the previous case, or from the end of the
    // expression being switched on if this is the first case.
    int startPos =
        indexOfDefault > 0
            ? state.getEndPosition(switchTree.getCases().get(indexOfDefault - 1))
            : state.getEndPosition(switchTree.getExpression());

    // End position will be the start of the body/first statement, the start of the next case, or
    // fallback to the end of the switch.
    int endPos;
    if (defaultCase.getBody() != null) {
      endPos = getStartPosition(defaultCase.getBody());
    } else if (!defaultCase.getStatements().isEmpty()) {
      endPos = getStartPosition(defaultCase.getStatements().getFirst());
    } else if (indexOfDefault + 1 < switchTree.getCases().size()) {
      endPos = getStartPosition(switchTree.getCases().get(indexOfDefault + 1));
    } else {
      endPos = state.getEndPosition(switchTree);
    }

    var tokens =
        ErrorProneTokens.getTokens(
            state.getSourceCode().subSequence(startPos, endPos).toString(),
            startPos,
            state.context);

    return tokens.stream()
        .flatMap(token -> token.comments().stream())
        .anyMatch(comment -> SKEW_PATTERN.matcher(comment.getText()).find());
  }

  /**
   * Whether the default case for the given switch is a fallback for potential version skew.
   *
   * <p>This is purely a heuristic that looks for the word "skew" in the comment of the default
   * case.
   */
  static boolean isDefaultCaseForSkew(
      SwitchExpressionTree switchTree, CaseTree defaultCase, VisitorState state) {
    checkArgument(isSwitchDefault(defaultCase));

    int indexOfDefault = switchTree.getCases().indexOf(defaultCase);

    // Start position will either be from the end of the previous case, or from the end of the
    // expression being switched on.
    int startPos =
        indexOfDefault > 0
            ? state.getEndPosition(switchTree.getCases().get(indexOfDefault - 1))
            : state.getEndPosition(switchTree.getExpression());

    // End position will be the start of the body of the default case. In switch expressions the
    // default case will always have a body as it cannot be combined with other cases.
    int endPos =
        getStartPosition(
            switch (defaultCase.getCaseKind()) {
              case STATEMENT -> defaultCase.getStatements().getFirst();
              case RULE -> defaultCase.getBody();
            });

    var tokens =
        ErrorProneTokens.getTokens(
            state.getSourceCode().subSequence(startPos, endPos).toString(),
            startPos,
            state.context);

    return tokens.stream()
        .flatMap(token -> token.comments().stream())
        .anyMatch(comment -> SKEW_PATTERN.matcher(comment.getText()).find());
  }

  private Switches() {}
}
