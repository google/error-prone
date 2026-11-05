/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.base.Suppliers.memoize;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression.PureMethodInvocation;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.Truthiness;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    summary =
        "This Optional has been confirmed to be empty at this point, so the call to `get()` or"
            + " `orElseThrow()` will always throw.",
    severity = WARNING)
public final class OptionalNotPresent extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<ExpressionTree> OPTIONAL_GET =
      anyOf(
          instanceMethod().onDescendantOf("com.google.common.base.Optional").named("get"),
          instanceMethod().onDescendantOf("java.util.Optional").namedAnyOf("get", "orElseThrow"));

  private final ConstantExpressions constantExpressions;

  @Inject
  OptionalNotPresent(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Deque<Supplier<Truthiness>> scopes = new ArrayDeque<>();
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      super(state);
      this.state = state;
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      withinScope(tree.getCondition(), /* not= */ false, tree.getThenStatement());
      withinScope(tree.getCondition(), /* not= */ true, tree.getElseStatement());
      return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      withinScope(tree.getCondition(), /* not= */ false, tree.getTrueExpression());
      withinScope(tree.getCondition(), /* not= */ true, tree.getFalseExpression());
      return null;
    }

    private void withinScope(ExpressionTree condition, boolean not, @Nullable Tree tree) {
      if (tree == null) {
        return;
      }
      scopes.push(memoize(() -> constantExpressions.truthiness(condition, not, state)));
      scan(tree, null);
      scopes.pop();
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      if (!scopes.isEmpty() && OPTIONAL_GET.matches(tree, state)) {
        var receiver = getReceiver(tree);
        if (receiver != null) {
          constantExpressions
              .constantExpression(receiver, state)
              .ifPresent(o -> checkForEmptiness(tree, o));
        }
      }
      return super.visitMethodInvocation(tree, null);
    }

    private void checkForEmptiness(
        MethodInvocationTree tree, ConstantExpression constantExpression) {
      if (getMethodInvocations(Truthiness::requiredTrue)
              .filter(
                  truth ->
                      truth.symbol() instanceof MethodSymbol
                          && truth.symbol().getSimpleName().contentEquals("isEmpty"))
              .flatMap(truth -> truth.receiver().stream())
              .anyMatch(constantExpression::equals)
          || getMethodInvocations(Truthiness::requiredFalse)
              .filter(
                  truth ->
                      truth.symbol() instanceof MethodSymbol
                          && truth.symbol().getSimpleName().contentEquals("isPresent"))
              .flatMap(truth -> truth.receiver().stream())
              .anyMatch(constantExpression::equals)) {
        state.reportMatch(describeMatch(tree));
      }
    }

    private Stream<PureMethodInvocation> getMethodInvocations(
        Function<Truthiness, ImmutableSet<ConstantExpression>> selector) {
      return scopes.stream()
          .map(Supplier::get)
          .flatMap(t -> selector.apply(t).stream())
          .flatMap(ce -> ce instanceof PureMethodInvocation pmi ? Stream.of(pmi) : Stream.empty());
    }
  }
}
