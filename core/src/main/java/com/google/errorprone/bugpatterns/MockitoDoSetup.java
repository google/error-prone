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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "Prefer using when/thenReturn over doReturn/when for additional type safety.")
public final class MockitoDoSetup extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableSet<VarSymbol> spies = findSpies(state);
    new SuppressibleTreePathScanner<Void, Void>(state) {

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        handle(tree);
        return super.visitMethodInvocation(tree, null);
      }

      private void handle(MethodInvocationTree tree) {
        if (!DO_STUBBER.matches(tree, state)) {
          return;
        }
        TreePath whenPath = getCurrentPath().getParentPath().getParentPath();
        Tree whenCall = whenPath.getLeaf();
        if (!(whenCall instanceof MethodInvocationTree whenMethod)
            || !INSTANCE_WHEN.matches(whenMethod, state)) {
          return;
        }
        if (isSpy(whenMethod.getArguments().get(0))) {
          return;
        }
        if (!(whenPath.getParentPath().getParentPath().getLeaf()
            instanceof MethodInvocationTree mockedMethod)) {
          return;
        }
        if (isSameType(
            getSymbol(mockedMethod).getReturnType(), state.getSymtab().voidType, state)) {
          return;
        }

        SuggestedFix.Builder fix = SuggestedFix.builder();
        var when = SuggestedFixes.qualifyStaticImport("org.mockito.Mockito.when", fix, state);
        fix.replace(whenMethod.getMethodSelect(), when)
            .replace(state.getEndPosition(whenMethod) - 1, state.getEndPosition(whenMethod), "")
            .postfixWith(
                mockedMethod,
                format(
                    ").%s(%s)",
                    NAME_MAPPINGS.get(getSymbol(tree).getSimpleName().toString()),
                    getParameterSource(tree, state)));

        state.reportMatch(describeMatch(tree, fix.build()));
      }

      private boolean isSpy(ExpressionTree tree) {
        var symbol = getSymbol(tree);
        return symbol != null
            && (spies.contains(symbol) || hasAnnotation(symbol, "org.mockito.Spy", state));
      }
    }.scan(state.getPath(), null);
    return NO_MATCH;
  }

  private static String getParameterSource(MethodInvocationTree tree, VisitorState state) {
    return state
        .getSourceCode()
        .subSequence(
            getStartPosition(tree.getArguments().get(0)),
            state.getEndPosition(getLast(tree.getArguments())))
        .toString();
  }

  private static ImmutableSet<VarSymbol> findSpies(VisitorState state) {
    // NOTES: This is extremely conservative in at least two ways.
    // 1) We ignore an entire mock if _any_ method is mocked to throw, not just the relevant method.
    // 2) We could still refactor if the thenThrow comes _after_, or if the _only_ call is
    // thenThrow.
    ImmutableSet.Builder<VarSymbol> spiesOrThrows = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        if (tree.getInitializer() != null && SPY.matches(tree.getInitializer(), state)) {
          spiesOrThrows.add(getSymbol(tree));
        }
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (DO_THROW.matches(tree, state)) {
          var whenCall = getCurrentPath().getParentPath().getParentPath().getLeaf();
          if (whenCall instanceof MethodInvocationTree methodInvocationTree
              && INSTANCE_WHEN.matches(methodInvocationTree, state)) {
            var whenTarget = getSymbol(methodInvocationTree.getArguments().get(0));
            if (whenTarget instanceof VarSymbol varSymbol) {
              spiesOrThrows.add(varSymbol);
            }
          }
        }
        if (THEN_THROW.matches(tree, state)) {
          var receiver = getReceiver(tree);
          if (STATIC_WHEN.matches(receiver, state)) {
            var mock = getReceiver(((MethodInvocationTree) receiver).getArguments().get(0));
            var mockSymbol = getSymbol(mock);
            if (mockSymbol instanceof VarSymbol varSymbol) {
              spiesOrThrows.add(varSymbol);
            }
          }
        }
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        if (SPY.matches(tree.getExpression(), state)) {
          var symbol = getSymbol(tree.getVariable());
          if (symbol instanceof VarSymbol varSymbol) {
            spiesOrThrows.add(varSymbol);
          }
        }
        return super.visitAssignment(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return spiesOrThrows.build();
  }

  private static final ImmutableMap<String, String> NAME_MAPPINGS =
      ImmutableMap.of(
          "doAnswer", "thenAnswer",
          "doReturn", "thenReturn",
          "doThrow", "thenThrow");
  private static final Matcher<ExpressionTree> DO_STUBBER =
      staticMethod().onClass("org.mockito.Mockito").namedAnyOf(NAME_MAPPINGS.keySet());

  private static final Matcher<ExpressionTree> INSTANCE_WHEN =
      instanceMethod().onDescendantOf("org.mockito.stubbing.Stubber").named("when");

  private static final Matcher<ExpressionTree> SPY =
      staticMethod().onClass("org.mockito.Mockito").named("spy");

  private static final Matcher<ExpressionTree> DO_THROW =
      staticMethod().onClass("org.mockito.Mockito").named("doThrow");

  private static final Matcher<ExpressionTree> STATIC_WHEN =
      staticMethod().onClass("org.mockito.Mockito").named("when");

  private static final Matcher<ExpressionTree> THEN_THROW =
      instanceMethod().onDescendantOf("org.mockito.stubbing.OngoingStubbing").named("thenThrow");
}
