/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import java.util.Iterator;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
  name = "ShortCircuitBoolean",
  category = JDK,
  summary = "Prefer the short-circuiting boolean operators && and || to & and |.",
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ShortCircuitBoolean extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case AND:
      case OR:
        break;
      default:
        return NO_MATCH;
    }
    if (!isSameType(getType(tree), state.getSymtab().booleanType, state)) {
      return NO_MATCH;
    }

    Iterator<Tree> stateIterator = state.getPath().getParentPath().iterator();
    Tree parent = stateIterator.next();

    if (parent instanceof BinaryTree
        && (parent.getKind() == Kind.AND || parent.getKind() == Kind.OR)) {
      return NO_MATCH;
    } else {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      new TreeScannerBinary(state).scan(tree, fix);
      return describeMatch(tree, fix.build());
    }
  }

  /** Replaces the operators when visiting the binary nodes */
  public static class TreeScannerBinary extends TreeScanner<Void, SuggestedFix.Builder> {
    /** saved state */
    public VisitorState state;

    /** constructor */
    public TreeScannerBinary(VisitorState currState) {
      this.state = currState;
    }

    @Override
    public Void visitBinary(BinaryTree tree, SuggestedFix.Builder p) {
      if (tree.getKind() == Kind.AND || tree.getKind() == Kind.OR) {
        p.replace(
            state.getEndPosition(tree.getLeftOperand()),
            ((JCTree) tree.getRightOperand()).getStartPosition(),
            tree.getKind() == Tree.Kind.AND ? " && " : " || ");
      }

      return super.visitBinary(tree, p);
    }
  }
}



