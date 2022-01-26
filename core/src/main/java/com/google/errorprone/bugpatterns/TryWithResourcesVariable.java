/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "This variable is unnecessary, the try-with-resources resource can be a reference to a"
            + " final or effectively final variable",
    severity = WARNING)
public class TryWithResourcesVariable extends BugChecker implements TryTreeMatcher {
  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    for (Tree resource : tree.getResources()) {
      if (!(resource instanceof VariableTree)) {
        continue;
      }
      VariableTree variableTree = (VariableTree) resource;
      ExpressionTree initializer = variableTree.getInitializer();
      if (!(initializer instanceof IdentifierTree)) {
        continue;
      }
      if (!isConsideredFinal(getSymbol(initializer))) {
        continue;
      }
      String name = ((IdentifierTree) initializer).getName().toString();
      state.reportMatch(
          describeMatch(
              resource,
              SuggestedFix.builder()
                  .replace(getStartPosition(variableTree), state.getEndPosition(initializer), name)
                  .merge(SuggestedFixes.renameVariableUsages(variableTree, name, state))
                  .build()));
    }
    return NO_MATCH;
  }
}
