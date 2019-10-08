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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Strings;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "XorPower",
    summary = "The `^` operator is binary XOR, not a power operator.",
    severity = ERROR,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public class XorPower extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!tree.getKind().equals(Tree.Kind.XOR)) {
      return NO_MATCH;
    }
    Integer lhs = ASTHelpers.constValue(tree.getLeftOperand(), Integer.class);
    if (lhs == null) {
      return NO_MATCH;
    }
    switch (lhs.intValue()) {
      case 2:
      case 10:
        break;
      default:
        return NO_MATCH;
    }
    Integer rhs = ASTHelpers.constValue(tree.getRightOperand(), Integer.class);
    if (rhs == null) {
      return NO_MATCH;
    }
    if (state.getSourceForNode(tree.getRightOperand()).startsWith("0")) {
      // hex and octal literals
      return NO_MATCH;
    }
    Description.Builder description =
        buildDescription(tree)
            .setMessage(
                String.format(
                    "The ^ operator is binary XOR, not a power operator, so '%s' will always"
                        + " evaluate to %d.",
                    state.getSourceForNode(tree), lhs ^ rhs));
    switch (lhs.intValue()) {
      case 2:
        if (rhs <= 31) {
          description.addFix(SuggestedFix.replace(tree, String.format("1 << %d", rhs)));
        }
        break;
      case 10:
        if (rhs <= 9) {
          description.addFix(SuggestedFix.replace(tree, "1" + Strings.repeat("0", rhs)));
        }
        break;
      default:
        throw new AssertionError(lhs);
    }
    return description.build();
  }
}
