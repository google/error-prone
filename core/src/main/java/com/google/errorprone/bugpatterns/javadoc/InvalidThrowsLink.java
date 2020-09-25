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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCErroneous;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Matches misuse of link tags within throws tags. */
@BugPattern(
    name = "InvalidThrowsLink",
    summary =
        "Javadoc links to exceptions in @throws without a @link tag (@throws Exception, not"
            + " @throws {@link Exception}).",
    severity = WARNING,
    tags = StandardTags.STYLE,
    documentSuppression = false)
public final class InvalidThrowsLink extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new ThrowsChecker(state).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class ThrowsChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private ThrowsChecker(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitErroneous(ErroneousTree node, Void unused) {
      Matcher matcher = THROWS_LINK.matcher(node.getBody());
      Comment comment = ((DCDocComment) getCurrentPath().getDocComment()).comment;
      if (matcher.find()) {
        int beforeAt = comment.getSourcePos(((DCErroneous) node).pos + matcher.start());
        int startOfCurly = comment.getSourcePos(((DCErroneous) node).pos + matcher.end());
        SuggestedFix fix =
            SuggestedFix.replace(beforeAt, startOfCurly, "@throws " + matcher.group(1));
        state.reportMatch(
            describeMatch(
                getDiagnosticPosition(beforeAt, getCurrentPath().getTreePath().getLeaf()), fix));
      }
      return super.visitErroneous(node, null);
    }
  }

  private static final Pattern THROWS_LINK = Pattern.compile("^@throws \\{@link ([^}]+)}");
}
