/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.lastStatement;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.suppliers.Suppliers.EXCEPTION_TYPE;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Enclosing;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "DeadException", altNames = "ThrowableInstanceNeverThrown",
    summary = "Exception created but not thrown",
    explanation =
        "The exception is created with new, but is not thrown, and the reference is lost.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class DeadException extends BugChecker implements NewClassTreeMatcher {

  public static final Matcher<Tree> MATCHER = allOf(
      parentNode(kindIs(EXPRESSION_STATEMENT)),
      isSubtypeOf(EXCEPTION_TYPE),
      not(enclosingMethod(JUnitMatchers.wouldRunInJUnit4)),
      anyOf(not(enclosingMethod(JUnitMatchers.isJunit3TestCase)),
            not(enclosingClass(JUnitMatchers.isJUnit3TestClass)))
  );

  @Override
  public Description matchNewClass(NewClassTree newClassTree, VisitorState state) {
    if (!MATCHER.matches(newClassTree, state)) {
      return Description.NO_MATCH;
    }

    StatementTree parent = (StatementTree) state.getPath().getParentPath().getLeaf();

    Matcher<List<StatementTree>> isLastStatementOfList =
        lastStatement(Matchers.<StatementTree>isSame(parent));
    boolean isLastStatement = anyOf(
        new Enclosing.BlockOrCase<>(
            inBlockWhereStatement(isLastStatementOfList),
            inCaseWhereStatement(isLastStatementOfList)),
        // it could also be a bare if statement with no braces
        parentNode(parentNode(kindIs(IF))))
        .matches(newClassTree, state);

    Fix fix;
    if (isLastStatement) {
      fix = SuggestedFix.prefixWith(newClassTree, "throw ");
    } else {
      fix = SuggestedFix.delete(parent);
    }

    return describeMatch(newClassTree, fix);
  }

  /*
   * state.getTreePath() is set incorrectly by the following classes in their call to
   * statementsMatcher. The problem is that it's weird to have to choose a proper path for the
   * multiple statements that statementsMatcher expects. The best approach might be to pick the path
   * of an arbitrary statement, but of course that has its own problems. Plus, the block might be
   * empty. Fortunately, in this case, we use only lastStatement, which doesn't use the TreePath.
   * Thus, it doesn't matter if the TreePath is wrong. But this problem casts suspicion on the use
   * of any Matcher<NotATree>, since without a unique tree, we can't have a unique TreePath in the
   * VisitorState passed to its match() method.
   */

  private static Matcher<BlockTree> inBlockWhereStatement(
      final Matcher<List<StatementTree>> statementsMatcher) {
    return new Matcher<BlockTree>() {
      @Override
      public boolean matches(BlockTree t, VisitorState state) {
        // state.getTreePath() will be wrong for statementsMatcher. See doc above.
        return statementsMatcher.matches(ImmutableList.copyOf(t.getStatements()), state);
      }
    };
  }

  private static Matcher<CaseTree> inCaseWhereStatement(
      final Matcher<List<StatementTree>> statementsMatcher) {
    return new Matcher<CaseTree>() {
      @Override
      public boolean matches(CaseTree t, VisitorState state) {
        // state.getTreePath() will be wrong for statementsMatcher. See doc above.
        return statementsMatcher.matches(ImmutableList.copyOf(t.getStatements()), state);
      }
    };
  }
}
