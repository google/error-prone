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
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Position;

/**
 * Bugpattern to encourage initializing effectively final variables inline with their declaration,
 * if possible.
 */
@BugPattern(
    name = "InitializeInline",
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
    if (symbol == null || !isConsideredFinal(symbol)) {
      return NO_MATCH;
    }
    AssignmentTree assignment =
        new TreePathScanner<AssignmentTree, Void>() {
          @Override
          public AssignmentTree reduce(AssignmentTree a, AssignmentTree b) {
            return a == null ? b : a;
          }

          @Override
          public AssignmentTree visitAssignment(AssignmentTree node, Void unused) {
            if (symbol.equals(getSymbol(node.getVariable()))) {
              Tree grandParent = getCurrentPath().getParentPath().getParentPath().getLeaf();
              if (getCurrentPath().getParentPath().getLeaf() instanceof StatementTree
                  && grandParent.equals(declarationParent)) {
                return node;
              }
            }
            return super.visitAssignment(node, null);
          }
        }.scan(state.getPath().getParentPath(), null);
    if (assignment == null) {
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
            .prefixWith(assignment, modifiers + state.getSourceForNode(tree.getType()) + " ")
            .build());
  }
}
