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

import static com.google.common.collect.Multisets.removeOccurrences;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression.ConstantExpressionKind;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.PureMethodInvocation;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.Truthiness;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.stream.Stream;

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

  public OptionalNotPresent(ErrorProneFlags flags) {
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Multiset<ConstantExpression> truths = HashMultiset.create();
    private final Multiset<ConstantExpression> falsehoods = HashMultiset.create();
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      super(state);
      this.state = state;
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      Truthiness truthiness =
          constantExpressions.truthiness(tree.getCondition(), /* not= */ false, state);

      withinScope(truthiness, tree.getThenStatement());

      withinScope(
          constantExpressions.truthiness(tree.getCondition(), /* not= */ true, state),
          tree.getElseStatement());
      return null;
    }

    private void withinScope(Truthiness truthiness, Tree tree) {
      truths.addAll(truthiness.requiredTrue());
      falsehoods.addAll(truthiness.requiredFalse());
      scan(tree, null);
      removeOccurrences(truths, truthiness.requiredTrue());
      removeOccurrences(falsehoods, truthiness.requiredFalse());
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      if (OPTIONAL_GET.matches(tree, state)) {
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
      if (getMethodInvocations(truths)
              .filter(
                  truth ->
                      truth.symbol() instanceof MethodSymbol
                          && truth.symbol().getSimpleName().contentEquals("isEmpty"))
              .flatMap(truth -> truth.receiver().stream())
              .anyMatch(constantExpression::equals)
          || getMethodInvocations(falsehoods)
              .filter(
                  truth ->
                      truth.symbol() instanceof MethodSymbol
                          && truth.symbol().getSimpleName().contentEquals("isPresent"))
              .flatMap(truth -> truth.receiver().stream())
              .anyMatch(constantExpression::equals)) {
        state.reportMatch(describeMatch(tree));
      }
    }

    private Stream<PureMethodInvocation> getMethodInvocations(Multiset<ConstantExpression> truths) {
      return truths.stream()
          .filter(truth -> truth.kind().equals(ConstantExpressionKind.PURE_METHOD))
          .map(ConstantExpression::pureMethod);
    }
  }
}
