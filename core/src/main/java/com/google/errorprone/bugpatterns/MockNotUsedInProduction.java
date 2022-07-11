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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Stream.concat;
import static javax.lang.model.element.ElementKind.FIELD;

import com.google.common.collect.ImmutableMap;
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
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary =
        "This mock is configured but never escapes to be used in production code. Should it be"
            + " removed?")
public final class MockNotUsedInProduction extends BugChecker
    implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<VarSymbol, Tree> mocks = findMocks(state);
    if (mocks.isEmpty()) {
      return NO_MATCH;
    }
    Set<VarSymbol> usedMocks = new HashSet<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
        // Don't count references to mocks within the arguments of a when(...) call to be a usage.
        // We still need to scan the receiver for the case of
        // `doReturn(someMockWhichIsAUsage).when(aMockWhichIsNotAUsage);`
        if (WHEN_OR_VERIFY.matches(invocation, state)) {
          scan(invocation.getMethodSelect(), null);
          return null;
        }
        return super.visitMethodInvocation(invocation, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelect, Void unused) {
        handle(memberSelect);
        return super.visitMemberSelect(memberSelect, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifier, Void unused) {
        handle(identifier);
        return super.visitIdentifier(identifier, null);
      }

      private void handle(Tree tree) {
        var symbol = getSymbol(tree);
        if (symbol instanceof VarSymbol) {
          usedMocks.add((VarSymbol) symbol);
        }
      }
    }.scan(state.getPath(), null);
    mocks.forEach(
        (sym, mockTree) -> {
          if (usedMocks.contains(sym)) {
            return;
          }
          state.reportMatch(describeMatch(mockTree, generateFix(sym, state)));
        });
    return NO_MATCH;
  }

  /**
   * Very crudely deletes every variable or expression statement which contains a reference to
   * {@code sym}. This is inefficient insofar as we scan the entire file again, but only when
   * generating a fix.
   */
  private static SuggestedFix generateFix(VarSymbol sym, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    new TreePathScanner<Void, Void>() {

      @Override
      public Void scan(Tree tree, Void unused) {
        if (Objects.equals(getSymbol(tree), sym)) {
          // Yes, at this point, the current path hasn't been updated to include `tree`...
          concat(Stream.of(tree), stream(getCurrentPath()))
              .filter(t -> t instanceof ExpressionStatementTree || t instanceof VariableTree)
              .findFirst()
              .ifPresent(fix::delete);
        }
        return super.scan(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  private ImmutableMap<VarSymbol, Tree> findMocks(VisitorState state) {
    Map<VarSymbol, Tree> mocks = new HashMap<>();
    AtomicBoolean injectMocks = new AtomicBoolean(false);
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol symbol = getSymbol(tree);
        if (INJECT_MOCKS_ANNOTATED.matches(tree, state)) {
          injectMocks.set(true);
        }
        if (isEligible(symbol)
            && (MOCK_OR_SPY_ANNOTATED.matches(tree, state)
                || (tree.getInitializer() != null && MOCK.matches(tree.getInitializer(), state)))) {
          mocks.put(symbol, tree);
        }
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        if (MOCK.matches(tree.getExpression(), state)) {
          var symbol = getSymbol(tree.getVariable());
          if (isEligible(symbol)) {
            mocks.put((VarSymbol) symbol, tree);
          }
        }
        return super.visitAssignment(tree, null);
      }

      private boolean isEligible(Symbol symbol) {
        return symbol instanceof VarSymbol
            && (!symbol.getKind().equals(FIELD) || canBeRemoved((VarSymbol) symbol))
            && annotatedAtMostMock(symbol);
      }

      private boolean annotatedAtMostMock(Symbol symbol) {
        return symbol.getAnnotationMirrors().stream()
            .allMatch(a -> a.getAnnotationType().asElement().getSimpleName().contentEquals("Mock"));
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    // A bit hacky: but if we saw InjectMocks, just claim there are no potentially unused mocks.
    return injectMocks.get() ? ImmutableMap.of() : ImmutableMap.copyOf(mocks);
  }

  private static final Matcher<ExpressionTree> MOCK =
      anyOf(
          staticMethod()
              .onClass("org.mockito.Mockito")
              .namedAnyOf("mock")
              .withParameters("java.lang.Class"),
          staticMethod().onClass("org.mockito.Mockito").namedAnyOf("spy"));

  private static final Matcher<VariableTree> MOCK_OR_SPY_ANNOTATED =
      anyOf(hasAnnotation("org.mockito.Mock"), hasAnnotation("org.mockito.Spy"));

  private static final Matcher<VariableTree> INJECT_MOCKS_ANNOTATED =
      hasAnnotation("org.mockito.InjectMocks");

  private static final Matcher<ExpressionTree> WHEN_OR_VERIFY =
      anyMethod().anyClass().namedAnyOf("when", "verify");
}
