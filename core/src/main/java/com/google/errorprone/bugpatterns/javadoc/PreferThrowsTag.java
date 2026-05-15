/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.fixes.SuggestedFix.replace;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;

/**
 * Prefer the {@code @throws} tag instead of the {@code @exception} tag.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(summary = "Prefer the @throws javadoc tag instead of @exception.", severity = WARNING)
public final class PreferThrowsTag extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new DocTreePathScanner<Void, Void>() {
        @Override
        public Void visitThrows(ThrowsTree throwsTree, Void unused) {
          if (throwsTree.getTagName().equals("exception")) {
            int startPos = Utils.getStartPosition(throwsTree, state);
            int endPos = startPos + "@exception".length();
            state.reportMatch(
                describeMatch(
                    diagnosticPosition(getCurrentPath(), state),
                    replace(startPos, endPos, "@throws")));
          }
          return super.visitThrows(throwsTree, null);
        }
      }.scan(path, null);
    }
    return Description.NO_MATCH;
  }
}
