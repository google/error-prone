/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;

/**
 * Finds common Javadoc errors, and tries to suggest useful fixes.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "ReturnFromVoid",
    summary = "Void methods should not have a @return tag.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class ReturnFromVoid extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new VoidReturnTypeChecker(state, methodTree).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class VoidReturnTypeChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;
    private final MethodTree methodTree;

    private VoidReturnTypeChecker(VisitorState state, MethodTree methodTree) {
      this.state = state;
      this.methodTree = methodTree;
    }

    @Override
    public Void visitReturn(ReturnTree returnTree, Void unused) {
      if (isSameType(getType(methodTree.getReturnType()), state.getSymtab().voidType, state)) {
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .addFix(Utils.replace(returnTree, "", state))
                .build());
      }
      return super.visitReturn(returnTree, null);
    }
  }
}
