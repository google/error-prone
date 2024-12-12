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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;

/** A bug pattern; see the summary. */
@BugPattern(summary = "This tag is invalid.", severity = WARNING, documentSuppression = false)
public final class InvalidSnippet extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private void scanTags(VisitorState state, DocTreePath path) {
    new InvalidTagChecker(state).scan(path, null);
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      scanTags(state, path);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      scanTags(state, path);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      scanTags(state, path);
    }
    return Description.NO_MATCH;
  }

  final class InvalidTagChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private InvalidTagChecker(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void unused) {
      if (erroneousTree.getBody().startsWith("{@snippet")) {
        String message =
            "This @snippet tag looks to be malformed. Did you forget the \":\"? Snippets should"
                + " start with \"{@snippet :\" followed by a newline.";
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .build());
      }
      return null;
    }

    @Override
    public Void scan(DocTree tree, Void unused) {
      return super.scan(tree, null);
    }
  }
}
