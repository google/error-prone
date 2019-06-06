/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isLastStatementInBlock;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * This checker finds and fixes empty statements after an if, with no else part. For example: if
 * (foo == 10);
 *
 * <p>It attempts to match javac's -Xlint:empty warning behavior, which can be found in
 * com/sun/tools/javac/comp/Check.java.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
    name = "EmptyIf",
    altNames = {"empty"},
    summary = "Empty statement after if",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class EmptyIfStatement extends BugChecker implements EmptyStatementTreeMatcher {

  /**
   * Match empty statement if: - Parent statement is an if - The then part of the parent if is an
   * empty statement, and - The else part of the parent if does not exist
   */
  @Override
  public Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state) {
    TreePath parentPath = state.getPath().getParentPath();
    Tree parent = parentPath.getLeaf();
    if (!(parent instanceof IfTree)) {
      return NO_MATCH;
    }
    IfTree ifTree = (IfTree) parent;
    if (!(ifTree.getThenStatement() instanceof EmptyStatementTree)
        || ifTree.getElseStatement() != null) {
      return NO_MATCH;
    }

    /*
     * We suggest different fixes depending on what follows the parent if statement.
     * If there is no statement following the if, then suggest deleting the whole
     * if statement. If the next statement is a block, then suggest deleting the
     * empty then part of the if.  If the next statement is not a block, then also
     * suggest deleting the empty then part of the if.
     */
    if (isLastStatementInBlock().matches(ifTree, state.withPath(parentPath))) {
      // No following statements. Delete whole if.
      return describeMatch(parent, SuggestedFix.delete(parent));
    } else {
      // There are more statements. Delete the empty then part of the if.
      return describeMatch(
          ifTree.getThenStatement(), SuggestedFix.delete(ifTree.getThenStatement()));
    }
  }
}
