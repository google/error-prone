/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Bugpattern to encourage initializing effectively final variables inline with their declaration,
 * if possible.
 */
@BugPattern(
    summary = "Initializing variables in their declaring statement is clearer, where possible.",
    severity = WARNING)
public final class InitializeInline extends BugChecker implements VariableTreeMatcher {
  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Tree declarationParent = state.getPath().getParentPath().getLeaf();
    if (declarationParent instanceof ClassTree) {
      return NO_MATCH;
    }
    VarSymbol symbol = getSymbol(tree);
    if (!isConsideredFinal(symbol)) {
      return NO_MATCH;
    }
    List<TreePath> assignments = new ArrayList<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitAssignment(AssignmentTree node, Void unused) {
        if (symbol.equals(getSymbol(node.getVariable()))) {
          assignments.add(getCurrentPath());
        }
        return super.visitAssignment(node, null);
      }
    }.scan(state.getPath().getParentPath(), null);
    if (assignments.size() != 1) {
      return NO_MATCH;
    }
    TreePath soleAssignmentPath = getOnlyElement(assignments);
    Tree grandParent = soleAssignmentPath.getParentPath().getParentPath().getLeaf();

    if (!(soleAssignmentPath.getParentPath().getLeaf() instanceof StatementTree
        && grandParent.equals(declarationParent))) {
      return NO_MATCH;
    }
    ModifiersTree modifiersTree = tree.getModifiers();
    String modifiers =
        modifiersTree == null || state.getEndPosition(modifiersTree) == Position.NOPOS
            ? ""
            : (state.getSourceForNode(modifiersTree) + " ");
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .replace(tree, "")
            .prefixWith(
                soleAssignmentPath.getLeaf(),
                modifiers + state.getSourceForNode(tree.getType()) + " ")
            .build());
  }
}
