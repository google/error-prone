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
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocTreePath;
import static com.google.errorprone.bugpatterns.javadoc.Utils.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;

/** Discourages using URLs in {@literal @}see tags. */
@BugPattern(
    summary =
        "URLs should not be used in @see tags; they are designed for Java elements which could be"
            + " used with @link.",
    severity = WARNING)
public final class UrlInSee extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      new UrlInSeeChecker(state).scan(path, null);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      new UrlInSeeChecker(state).scan(path, null);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      new UrlInSeeChecker(state).scan(path, null);
    }
    return NO_MATCH;
  }

  private final class UrlInSeeChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private UrlInSeeChecker(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void unused) {
      if (erroneousTree.getBody().startsWith("@see http")) {
        state.reportMatch(
            describeMatch(
                diagnosticPosition(getCurrentPath(), state),
                replace(
                    erroneousTree, erroneousTree.getBody().replaceFirst("@see", "See"), state)));
      }
      return super.visitErroneous(erroneousTree, unused);
    }
  }
}
