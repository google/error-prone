/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.isRuleKind;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This break is unnecessary, fallthrough does not occur in -> switches",
    severity = WARNING)
public class UnnecessaryBreakInSwitch extends BugChecker implements BugChecker.CaseTreeMatcher {
  @Override
  public Description matchCase(CaseTree tree, VisitorState state) {
    if (!isRuleKind(tree)) {
      return NO_MATCH;
    }
    Tree body = ASTHelpers.getCaseTreeBody(tree);
    ImmutableList<BreakTree> unnecessaryBreaks = unnecessaryBreaks(body);
    if (unnecessaryBreaks.isEmpty()) {
      return NO_MATCH;
    }
    unnecessaryBreaks.forEach(
        unnecessaryBreak ->
            state.reportMatch(
                describeMatch(unnecessaryBreak, SuggestedFix.delete(unnecessaryBreak))));
    return NO_MATCH;
  }

  private ImmutableList<BreakTree> unnecessaryBreaks(Tree tree) {
    ImmutableList.Builder<BreakTree> result = ImmutableList.builder();
    new SimpleTreeVisitor<Void, Void>() {
      @Override
      public Void visitBreak(BreakTree node, Void unused) {
        if (node.getLabel() == null) {
          result.add(node);
        }
        return null;
      }

      @Override
      public Void visitBlock(BlockTree node, Void unused) {
        visit(getLast(node.getStatements(), null), null);
        return null;
      }

      @Override
      public Void visitIf(IfTree node, Void unused) {
        visit(node.getThenStatement(), null);
        visit(node.getElseStatement(), null);
        return null;
      }
    }.visit(tree, null);
    return result.build();
  }
}
