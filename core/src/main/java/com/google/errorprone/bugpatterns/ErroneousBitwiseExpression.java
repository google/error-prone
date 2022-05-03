/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "This expression evaluates to 0. If this isn't an error, consider expressing it as a"
            + " literal 0.",
    severity = WARNING)
public final class ErroneousBitwiseExpression extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (tree.getKind() != Kind.AND) {
      return NO_MATCH;
    }
    Object constantValue = ASTHelpers.constValue(tree);
    // Constants of the form A & B which evaluate to a literal 0 are probably trying to combine
    // bitwise flags using |.
    return Objects.equals(constantValue, 0) || Objects.equals(constantValue, 0L)
        ? describeMatch(
            tree,
            SuggestedFix.replace(
                /* startPos= */ state.getEndPosition(tree.getLeftOperand()),
                /* endPos= */ getStartPosition(tree.getRightOperand()),
                " | "))
        : NO_MATCH;
  }
}
