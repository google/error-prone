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
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Matches invalid usage of {@literal @inheritDoc}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "InheritDoc",
    summary = "Invalid use of @inheritDoc.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,
    documentSuppression = false)
public final class InheritDoc extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(state);
  }

  private Description handle(VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new InheritDocChecker(state).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class InheritDocChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private InheritDocChecker(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitInheritDoc(InheritDocTree inheritDocTree, Void unused) {
      new SimpleTreeVisitor<Void, Void>() {
        @Override
        public Void visitVariable(VariableTree variableTree, Void unused) {
          state.reportMatch(
              buildDescription(diagnosticPosition(getCurrentPath(), state))
                  .setMessage(
                      "@inheritDoc doesn't make sense on variables as "
                          + "they cannot override a super element.")
                  .build());
          return null;
        }

        @Override
        public Void visitMethod(MethodTree methodTree, Void unused) {
          MethodSymbol methodSymbol = getSymbol(methodTree);
          if (methodSymbol != null && findSuperMethods(methodSymbol, state.getTypes()).isEmpty()) {
            state.reportMatch(
                buildDescription(diagnosticPosition(getCurrentPath(), state))
                    .setMessage(
                        "This method does not override anything to inherit documentation from.")
                    .build());
          }
          return null;
        }

        @Override
        public Void visitClass(ClassTree classTree, Void unused) {
          if (classTree.getExtendsClause() == null && classTree.getImplementsClause().isEmpty()) {
            state.reportMatch(
                buildDescription(diagnosticPosition(getCurrentPath(), state))
                    .setMessage(
                        "This class does not extend or implement anything to inherit "
                            + "documentation from.")
                    .build());
          }
          return null;
        }
      }.visit(getCurrentPath().getTreePath().getLeaf(), null);
      return super.visitInheritDoc(inheritDocTree, null);
    }
  }
}
