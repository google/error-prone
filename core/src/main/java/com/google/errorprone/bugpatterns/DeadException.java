/*
 * Copyright 2011 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.suppliers.Suppliers.THROWABLE_TYPE;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.IF;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.ChildMultiMatcher;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

/** @author alexeagle@google.com (Alex Eagle) */
@BugPattern(
    name = "DeadException",
    altNames = "ThrowableInstanceNeverThrown",
    summary = "Exception created but not thrown",
    category = JDK,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class DeadException extends BugChecker implements NewClassTreeMatcher {

  public static final Matcher<Tree> MATCHER =
      allOf(
          parentNode(kindIs(EXPRESSION_STATEMENT)),
          isSubtypeOf(THROWABLE_TYPE),
          not(
              anyOf(
                  enclosingClass(JUnitMatchers.isJUnit3TestClass),
                  enclosingClass(JUnitMatchers.isAmbiguousJUnitVersion),
                  enclosingClass(JUnitMatchers.isJUnit4TestClass))));

  @Override
  public Description matchNewClass(NewClassTree newClassTree, VisitorState state) {
    if (!MATCHER.matches(newClassTree, state)) {
      return Description.NO_MATCH;
    }

    StatementTree parent = (StatementTree) state.getPath().getParentPath().getLeaf();

    boolean isLastStatement =
        anyOf(
                new ChildOfBlockOrCase<>(
                    ChildMultiMatcher.MatchType.LAST, Matchers.<StatementTree>isSame(parent)),
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

  private static class ChildOfBlockOrCase<T extends Tree>
      extends ChildMultiMatcher<T, StatementTree> {
    public ChildOfBlockOrCase(MatchType matchType, Matcher<StatementTree> nodeMatcher) {
      super(matchType, nodeMatcher);
    }

    @Override
    protected Iterable<? extends StatementTree> getChildNodes(T tree, VisitorState state) {
      Tree enclosing = state.findEnclosing(CaseTree.class, BlockTree.class);
      if (enclosing == null) {
        return ImmutableList.of();
      }
      if (enclosing instanceof BlockTree) {
        return ((BlockTree) enclosing).getStatements();
      } else if (enclosing instanceof CaseTree) {
        return ((CaseTree) enclosing).getStatements();
      } else {
        // findEnclosing given two types must return something of one of those types
        throw new IllegalStateException("enclosing tree not a BlockTree or CaseTree");
      }
    }
  }
}
