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

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ContinueTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.Tree;

/** A bug checker; see the summary. */
@BugPattern(
    severity = SeverityLevel.WARNING,
    summary = "This continue statement is redundant and can be removed. It may be misleading.")
public final class RedundantControlFlow extends BugChecker implements ContinueTreeMatcher {
  @Override
  public Description matchContinue(ContinueTree tree, VisitorState state) {
    if (tree.getLabel() != null) {
      return NO_MATCH;
    }
    Tree last = tree;
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case FOR_LOOP:
        case ENHANCED_FOR_LOOP:
        case WHILE_LOOP:
        case DO_WHILE_LOOP:
          return describeMatch(tree, SuggestedFix.delete(tree));
        case CASE:
          // It's arguable that "break" is clearer than "continue" from a switch if they both have
          // the same effect, but it's not what we want to flag here.
          return NO_MATCH;
        case BLOCK:
          var statements = ((BlockTree) parent).getStatements();
          if (statements.indexOf(last) != statements.size() - 1) {
            return NO_MATCH;
          }
        // fall through
        default:
          // fall out
      }
      last = parent;
    }
    return NO_MATCH;
  }
}
