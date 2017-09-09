/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import javax.annotation.Nullable;

/** @author lowasser@google.com (Louis Wasserman) */
@BugPattern(
  name = "EqualsNaN",
  summary = "== NaN always returns false; use the isNaN methods instead",
  explanation =
      "As per JLS 15.21.1, == NaN comparisons always return false, even NaN == NaN. "
          + "Instead, use the isNaN methods to check for NaN.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class EqualsNaN extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    String prefix;
    switch (tree.getKind()) {
      case EQUAL_TO:
        prefix = "";
        break;
      case NOT_EQUAL_TO:
        prefix = "!";
        break;
      default:
        return Description.NO_MATCH;
    }
    JCExpression left = (JCExpression) tree.getLeftOperand();
    JCExpression right = (JCExpression) tree.getRightOperand();

    String leftMatch = matchNaN(left);
    if (leftMatch != null) {
      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree, String.format("%s%s.isNaN(%s)", prefix, leftMatch, toString(right, state))));
    }
    String rightMatch = matchNaN(right);
    if (rightMatch != null) {
      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree, String.format("%s%s.isNaN(%s)", prefix, rightMatch, toString(left, state))));
    }
    return Description.NO_MATCH;
  }

  private CharSequence toString(JCTree tree, VisitorState state) {
    CharSequence source = state.getSourceForNode(tree);
    return (source == null) ? tree.toString() : source;
  }

  @Nullable
  private String matchNaN(ExpressionTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym != null
        && sym.owner != null
        && sym.owner.asType() != null
        && sym.getSimpleName().contentEquals("NaN")) {
      if (sym.owner.getQualifiedName().contentEquals("java.lang.Double")) {
        return "Double";
      } else if (sym.owner.getQualifiedName().contentEquals("java.lang.Float")) {
        return "Float";
      }
    }
    return null;
  }
}
