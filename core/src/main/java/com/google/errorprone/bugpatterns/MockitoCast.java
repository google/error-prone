/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.Category.MOCKITO;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Scope.LookupKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** @author Liam Miller-Cushon (cushon@google.com) */
@BugPattern(
  name = "MockitoCast",
  category = MOCKITO,
  summary = "A bug in Mockito will cause this test to fail at runtime with a ClassCastException",
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class MockitoCast extends BugChecker implements CompilationUnitTreeMatcher {

  private static final String MOCKITO_CLASS = "org.mockito.Mockito";

  private static final String UI_FIELD_ANNOTATION = "com.google.gwt.uibinder.client.UiField";

  private static final String MOCK_ANNOTATION = "org.mockito.Mock";

  /** Answer strategies that always return an instance of the erased return type. */
  private static final ImmutableSet<String> BAD_ANSWER_STRATEGIES =
      ImmutableSet.of("RETURNS_SMART_NULLS", "RETURNS_MOCKS", "RETURNS_DEEP_STUBS");

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, final VisitorState state) {
    Symbol mockitoSym = state.getSymbolFromString(MOCKITO_CLASS);
    if (mockitoSym == null) {
      // fast path if mockito isn't being used
      return Description.NO_MATCH;
    }

    // collect variable symbols for standard Answer constants that don't support generics
    final Set<Symbol> badAnswers = new LinkedHashSet<>();
    for (Symbol member : mockitoSym.members().getSymbols(LookupKind.NON_RECURSIVE)) {
      if (member.getKind() != ElementKind.FIELD) {
        continue;
      }
      if (BAD_ANSWER_STRATEGIES.contains(member.getSimpleName().toString())) {
        badAnswers.add(member);
      }
    }

    // collect mocks that are initialized in this compilation unit using a bad answer strategy
    final Set<VarSymbol> mockVariables = MockInitializationScanner.scan(state, badAnswers);

    // check for when(...) calls on mocks using a bad answer strategy
    new WhenNeedsCastScanner(mockVariables, state).scan(state.getPath(), null);

    // errors are reported in WhenNeedsCastScanner
    return Description.NO_MATCH;
  }

  /**
   * Records declarations of and assignments to mock variables where the initializer references an
   * answer strategy that does not support generics.
   */
  static class MockInitializationScanner extends TreeScanner<Void, Void> {

    static Set<VarSymbol> scan(VisitorState state, Set<Symbol> badAnswers) {
      MockInitializationScanner scanner = new MockInitializationScanner(badAnswers);
      state.getPath().getCompilationUnit().accept(scanner, null);
      return scanner.mockVariables;
    }

    private final Set<VarSymbol> mockVariables = new LinkedHashSet<>();
    private final Set<Symbol> badAnswers;

    public MockInitializationScanner(Set<Symbol> badAnswers) {
      this.badAnswers = badAnswers;
    }

    @Override
    public Void visitVariable(VariableTree node, Void aVoid) {
      recordInitialization(node, node.getInitializer());
      return super.visitVariable(node, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
      recordInitialization(node.getVariable(), node.getExpression());
      return super.visitAssignment(node, aVoid);
    }

    private void recordInitialization(Tree varTree, ExpressionTree initializer) {
      if (initializer == null) {
        return;
      }
      Symbol sym = ASTHelpers.getSymbol(varTree);
      if (!(sym instanceof VarSymbol)) {
        return;
      }
      Boolean initializedWithBadAnswer =
          initializer.accept(
              new TreeScanner<Boolean, Void>() {
                @Override
                public Boolean scan(Tree tree, Void unused) {
                  if (badAnswers.contains(ASTHelpers.getSymbol(tree))) {
                    return true;
                  }
                  return super.scan(tree, null);
                }

                @Override
                public Boolean reduce(Boolean r1, Boolean r2) {
                  return firstNonNull(r1, false) || firstNonNull(r2, false);
                }
              },
              null);

      if (firstNonNull(initializedWithBadAnswer, false)) {
        mockVariables.add((VarSymbol) sym);
      }
    }
  }

  private static final Matcher<ExpressionTree> WHEN_MATCHER =
      staticMethod().onClass(MOCKITO_CLASS).named("when");

  /** Scans for when(...) calls that needs a cast added, and emits fixes. */
  class WhenNeedsCastScanner extends TreePathScanner<Void, Void> {

    final Set<VarSymbol> badMocks;
    final VisitorState state;

    WhenNeedsCastScanner(Set<VarSymbol> badMocks, VisitorState state) {
      this.badMocks = badMocks;
      this.state = state;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      Description description = matchMethodInvocation(node, state.withPath(getCurrentPath()));
      if (description != Description.NO_MATCH) {
        state.reportMatch(description);
      }
      return super.visitMethodInvocation(node, null);
    }

    public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
      // look for a call to Mockito.when(arg)
      if (!WHEN_MATCHER.matches(tree, state)) {
        return Description.NO_MATCH;
      }
      // where the only arg is an invocation
      if (tree.getArguments().size() != 1) {
        return Description.NO_MATCH;
      }
      ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
      if (!(arg instanceof JCMethodInvocation)) {
        return Description.NO_MATCH;
      }
      // and the invocation's inferred erased and uninstantiated erased return types differ
      JCMethodInvocation call = (JCMethodInvocation) arg;
      Types types = state.getTypes();
      if (call.meth.type == null) {
        return Description.NO_MATCH;
      }
      Type instantiatedReturnType = types.erasure(call.meth.type.getReturnType());
      if (instantiatedReturnType == null) {
        return Description.NO_MATCH;
      }
      MethodSymbol methodSym = ASTHelpers.getSymbol(call);
      if (methodSym == null) {
        return Description.NO_MATCH;
      }
      if (methodSym.type == null) {
        return Description.NO_MATCH;
      }
      Type uninstantiatedReturnType = types.erasure(methodSym.type.getReturnType());
      if (uninstantiatedReturnType == null) {
        return Description.NO_MATCH;
      }
      if (types.isSameType(instantiatedReturnType, uninstantiatedReturnType)) {
        return Description.NO_MATCH;
      }
      if (!MockAnswerStrategyScanner.scan(call.getMethodSelect(), state, badMocks)) {
        return Description.NO_MATCH;
      }

      final SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      String qual =
          uninstantiatedReturnType.tsym.getTypeParameters().isEmpty()
              ? SuggestedFixes.qualifyType(state, fixBuilder, uninstantiatedReturnType.tsym)
              : "Object";
      fixBuilder.prefixWith(arg, String.format("(%s) ", qual));
      return describeMatch(tree, fixBuilder.build());
    }
  }

  /**
   * Scans for the mock variable in a when(...), and checks if it has a bad answer strategy.
   *
   * <p>@Mock annotations are handled here instead of in {@link MockInitializationScanner} because
   * they're visible across compilation boundaries, so scanning the declarations in the current
   * compilation would result in false negatives.
   */
  static class MockAnswerStrategyScanner extends TreeScanner<Boolean, Void> {

    static boolean scan(Tree tree, VisitorState state, Set<VarSymbol> badMocks) {
      return firstNonNull(tree.accept(new MockAnswerStrategyScanner(state, badMocks), null), false);
    }

    private final VisitorState state;
    private final Set<VarSymbol> badMocks;

    public MockAnswerStrategyScanner(VisitorState state, Set<VarSymbol> badMocks) {
      this.state = state;
      this.badMocks = badMocks;
    }

    @Override
    public Boolean scan(Tree tree, Void aVoid) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym instanceof VarSymbol) {
        VarSymbol varSym = (VarSymbol) sym;
        if (badMocks.contains(ASTHelpers.getSymbol(tree))) {
          return true;
        }
        // custom answer strategies can be specified using @Mock(answer = ...)
        if (ASTHelpers.hasAnnotation(sym, MOCK_ANNOTATION, state)
            && !answerHandlesGenerics(varSym, state)) {
          return true;
        }
        // gwtmockito mocks @UiFields
        if (ASTHelpers.hasAnnotation(varSym, UI_FIELD_ANNOTATION, state)) {
          return true;
        }
      }
      return super.scan(tree, aVoid);
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
      return firstNonNull(r1, false) || firstNonNull(r2, false);
    }

    /**
     * Returns if the variable has a {@code @Mock} annotation that specifies an answer that does not
     * handle generics.
     */
    static boolean answerHandlesGenerics(VarSymbol varSym, VisitorState state) {
      Compound attribute = varSym.attribute(state.getSymbolFromString(MOCK_ANNOTATION));
      String answer = null;
      for (Entry<MethodSymbol, Attribute> e : attribute.getElementValues().entrySet()) {
        if (e.getKey().getSimpleName().contentEquals("answer")) {
          answer = e.getValue().getValue().toString();
          break;
        }
      }
      return !BAD_ANSWER_STRATEGIES.contains(answer);
    }
  }
}
