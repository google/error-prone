/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.sun.source.tree.Tree.Kind.IF;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

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
  explanation =
      "An if statement contains an empty statement as the then clause. A semicolon may "
          + "have been inserted by accident.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class EmptyIfStatement extends BugChecker implements EmptyStatementTreeMatcher {

  /**
   * Match empty statement if: - Parent statement is an if - The then part of the parent if is an
   * empty statement, and - The else part of the parent if does not exist
   */
  @Override
  public Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state) {
    boolean matches = false;
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() == IF) {
      IfTree parentAsIf = (IfTree) parent;
      matches =
          (parentAsIf.getThenStatement() instanceof EmptyStatementTree)
              && (parentAsIf.getElseStatement() == null);
    }
    if (!matches) {
      return Description.NO_MATCH;
    }

    /*
     * We suggest different fixes depending on what follows the parent if statement.
     * If there is no statement following the if, then suggest deleting the whole
     * if statement. If the next statement is a block, then suggest deleting the
     * empty then part of the if.  If the next statement is not a block, then also
     * suggest deleting the empty then part of the if.
     */
    boolean nextStmtIsNull =
        parentNode(nextStatement(Matchers.<StatementTree>isSame(null))).matches(tree, state);

    assert (state.getPath().getParentPath().getLeaf().getKind() == IF);
    IfTree ifParent = (IfTree) state.getPath().getParentPath().getLeaf();
    if (nextStmtIsNull) {
      // No following statements. Delete whole if.
      return describeMatch(parent, SuggestedFix.delete(parent));
    } else {
      // There are more statements. Delete the empty then part of the if.
      return describeMatch(
          ifParent.getThenStatement(), SuggestedFix.delete(ifParent.getThenStatement()));
    }
  }
}
