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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "LoopOverCharArray",
    summary = "toCharArray allocates a new array, using charAt is more efficient",
    severity = WARNING)
public class LoopOverCharArray extends BugChecker implements BugChecker.EnhancedForLoopTreeMatcher {

  private static final Matcher<ExpressionTree> TO_CHAR_ARRAY =
      instanceMethod().onExactClass("java.lang.String").named("toCharArray");

  @Override
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    if (!TO_CHAR_ARRAY.matches(tree.getExpression(), state)) {
      return NO_MATCH;
    }
    ExpressionTree receiver = ASTHelpers.getReceiver(tree.getExpression());
    if (!(receiver instanceof IdentifierTree)) {
      return NO_MATCH;
    }
    StatementTree body = tree.getStatement();
    if (!(body instanceof BlockTree)) {
      return NO_MATCH;
    }
    List<? extends StatementTree> statements = ((BlockTree) body).getStatements();
    if (statements.isEmpty()) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    // The fix uses `i` as the loop index variable, so give up if there's already an `i` in scope
    if (!alreadyDefinesIdentifier(tree)) {
      description.addFix(
          SuggestedFix.replace(
              getStartPosition(tree),
              getStartPosition(statements.get(0)),
              String.format(
                  "for (int i = 0; i < %s.length(); i++) { char %s = %s.charAt(i);",
                  state.getSourceForNode(receiver),
                  tree.getVariable().getName(),
                  state.getSourceForNode(receiver))));
    }
    return description.build();
  }

  private static boolean alreadyDefinesIdentifier(Tree tree) {
    boolean[] result = {false};
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree node, Void unused) {
        if (node.getName().contentEquals("i")) {
          result[0] = true;
        }
        return super.visitIdentifier(node, unused);
      }
    }.scan(tree, null);
    return result[0];
  }
}
