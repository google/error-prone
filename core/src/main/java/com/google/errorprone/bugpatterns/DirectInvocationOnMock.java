/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyStaticImport;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.MoreAnnotations.getAnnotationValue;
import static java.lang.String.format;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** A bugpattern; see the description. */
@BugPattern(
    summary =
        "Methods should not be directly invoked on mocks. Should this be part of a verify(..)"
            + " call?",
    severity = WARNING)
public final class DirectInvocationOnMock extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableSet<VarSymbol> mocks = findMocks(state);

    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        Tree parent =
            stream(getCurrentPath())
                .skip(1)
                .filter(t -> !(t instanceof TypeCastTree))
                .findFirst()
                .get();
        var receiver = getReceiver(tree);
        if (isMock(receiver)
            && !(parent instanceof ExpressionTree
                && WHEN.matches((ExpressionTree) parent, state))) {
          var description = buildDescription(tree);
          if (getCurrentPath().getParentPath().getLeaf() instanceof ExpressionStatementTree) {
            var fix = SuggestedFix.builder();
            String verify = qualifyStaticImport("org.mockito.Mockito.verify", fix, state);
            description.addFix(
                fix.replace(receiver, format("%s(%s)", verify, state.getSourceForNode(receiver)))
                    .setShortDescription("turn into verify() call")
                    .build());
            description.addFix(
                SuggestedFix.builder()
                    .delete(tree)
                    .setShortDescription("delete redundant invocation")
                    .build());
          }
          state.reportMatch(description.build());
        }
        return super.visitMethodInvocation(tree, null);
      }

      private boolean isMock(ExpressionTree tree) {
        var symbol = getSymbol(tree);
        return symbol != null
            && (mocks.contains(symbol)
                || symbol.getAnnotationMirrors().stream()
                    .filter(am -> am.type.tsym.getQualifiedName().contentEquals("org.mockito.Mock"))
                    .findFirst()
                    .filter(am -> getAnnotationValue(am, "answer").isEmpty())
                    .isPresent());
      }
    }.scan(state.getPath(), null);

    return NO_MATCH;
  }

  private ImmutableSet<VarSymbol> findMocks(VisitorState state) {
    ImmutableSet.Builder<VarSymbol> mocks = ImmutableSet.builder();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        if (tree.getInitializer() != null && MOCK.matches(tree.getInitializer(), state)) {
          mocks.add(getSymbol(tree));
        }
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        if (MOCK.matches(tree.getExpression(), state)) {
          var symbol = getSymbol(tree.getVariable());
          if (symbol instanceof VarSymbol) {
            mocks.add((VarSymbol) symbol);
          }
        }
        return super.visitAssignment(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return mocks.build();
  }

  private static final Matcher<ExpressionTree> MOCK =
      staticMethod().onClass("org.mockito.Mockito").named("mock").withParameters("java.lang.Class");

  private static final Matcher<ExpressionTree> WHEN = anyMethod().anyClass().named("when");
}
