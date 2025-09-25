/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getJavadoccableTrees;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.ErrorProneComment.ErrorProneCommentStyle;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "Avoid using `/**` for comments which aren't actually Javadoc.",
    severity = WARNING,
    documentSuppression = false)
public final class NotJavadoc extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<Integer, TreePath> javadocableTrees = getJavadoccableTrees(tree);
    ImmutableSet<Integer> seeminglyJavadocableTrees = getSeeminglyJavadocableTrees(tree);
    ImmutableRangeSet<Integer> suppressedRegions = suppressedRegions(state);
    for (ErrorProneToken token : getTokens(state.getSourceCode().toString(), state.context)) {
      for (ErrorProneComment comment : token.comments()) {
        if (!comment.getStyle().equals(ErrorProneCommentStyle.JAVADOC_BLOCK)
            || comment.getText().equals("/**/")) {
          continue;
        }
        if (javadocableTrees.containsKey(token.pos())) {
          continue;
        }

        if (suppressedRegions.intersects(
            Range.closed(
                comment.getSourcePos(0), comment.getSourcePos(comment.getText().length() - 1)))) {
          continue;
        }

        int endPos = 2;
        while (comment.getText().charAt(endPos) == '*') {
          endPos++;
        }
        var message =
            seeminglyJavadocableTrees.contains(token.pos())
                ? "This comment seems to be attached to a method or class, but methods and classes"
                    + " nested within methods cannot be documented with Javadoc."
                : message();
        state.reportMatch(
            buildDescription(getDiagnosticPosition(comment.getSourcePos(0), tree))
                .setMessage(message)
                .addFix(replace(comment.getSourcePos(1), comment.getSourcePos(endPos - 1), ""))
                .build());
      }
    }
    return NO_MATCH;
  }

  private static ImmutableSet<Integer> getSeeminglyJavadocableTrees(CompilationUnitTree tree) {
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void scan(Tree tree, Void unused) {
            if (tree instanceof MethodTree || tree instanceof ClassTree) {
              builder.add(getStartPosition(tree));
            }
            return super.scan(tree, null);
          }
        },
        null);
    return builder.build();
  }
}
