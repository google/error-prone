/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.union;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static java.lang.String.format;

import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bugpattern to find conditions which are checked more than once, and either vacuously true or
 * false.
 */
@BugPattern(
    name = "AlreadyChecked",
    severity = WARNING,
    summary = "This condition has already been checked.")
public final class AlreadyChecked extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Multiset<VarSymbol> truths;
    private final Multiset<VarSymbol> falsehoods;
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      this.state = state;
      truths = HashMultiset.create();
      falsehoods = HashMultiset.create();
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      Truthiness truthiness = Truthiness.from(tree.getCondition(), /* not= */ false);

      checkCondition(tree.getCondition(), truthiness);

      withinScope(truthiness, tree.getThenStatement());

      withinScope(Truthiness.from(tree.getCondition(), /* not= */ true), tree.getElseStatement());
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
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      checkCondition(tree.getCondition(), Truthiness.from(tree.getCondition(), false));
      return super.visitConditionalExpression(tree, null);
    }

    void checkCondition(Tree tree, Truthiness truthiness) {
      Set<VarSymbol> vacuousFalsehoods =
          union(
              intersection(truthiness.requiredTrue(), falsehoods.elementSet()),
              intersection(truthiness.requiredFalse(), truths.elementSet()));
      if (!vacuousFalsehoods.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(format("This condition (on %s) is vacuously false.", vacuousFalsehoods))
                .build());
      }
      Set<VarSymbol> vacuousTruths =
          union(
              intersection(truthiness.requiredTrue(), truths.elementSet()),
              intersection(truthiness.requiredFalse(), falsehoods.elementSet()));
      if (!vacuousTruths.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(format("This condition (on %s) is vacuously true.", vacuousTruths))
                .build());
      }
    }
  }

  @AutoValue
  abstract static class Truthiness {
    abstract ImmutableSet<VarSymbol> requiredTrue();

    abstract ImmutableSet<VarSymbol> requiredFalse();

    private static Truthiness create(
        Iterable<VarSymbol> requiredTrue, Iterable<VarSymbol> requiredFalse) {
      return new AutoValue_AlreadyChecked_Truthiness(
          ImmutableSet.copyOf(requiredTrue), ImmutableSet.copyOf(requiredFalse));
    }

    /**
     * Scans an {@link ExpressionTree} to find anything guaranteed to be false or true if this
     * expression is true.
     */
    private static Truthiness from(ExpressionTree tree, boolean not) {
      ImmutableSet.Builder<VarSymbol> requiredTrue = ImmutableSet.builder();
      ImmutableSet.Builder<VarSymbol> requiredFalse = ImmutableSet.builder();

      // Keep track of whether we saw an expression too complex for us to handle, and failed.
      AtomicBoolean failed = new AtomicBoolean();

      tree.accept(
          new SimpleTreeVisitor<Void, Void>() {
            boolean negated = not;

            @Override
            public Void visitParenthesized(ParenthesizedTree tree, Void unused) {
              return visit(tree.getExpression(), null);
            }

            @Override
            public Void visitUnary(UnaryTree tree, Void unused) {
              if (tree.getKind().equals(Kind.LOGICAL_COMPLEMENT)) {
                negated = !negated;
                visit(tree.getExpression(), null);
                negated = !negated;
              }
              return null;
            }

            @Override
            public Void visitBinary(BinaryTree tree, Void unused) {
              if (negated
                  ? tree.getKind().equals(Kind.CONDITIONAL_OR)
                  : tree.getKind().equals(Kind.CONDITIONAL_AND)) {
                visit(tree.getLeftOperand(), null);
                visit(tree.getRightOperand(), null);
              } else {
                failed.set(true);
              }
              return null;
            }

            @Override
            public Void visitIdentifier(IdentifierTree tree, Void unused) {
              Symbol symbol = getSymbol(tree);
              if (symbol instanceof VarSymbol && isConsideredFinal(symbol)) {
                if (negated) {
                  requiredFalse.add((VarSymbol) symbol);
                } else {
                  requiredTrue.add((VarSymbol) symbol);
                }
              }
              return null;
            }
          },
          null);

      if (failed.get()) {
        return create(ImmutableSet.of(), ImmutableSet.of());
      }

      return create(requiredTrue.build(), requiredFalse.build());
    }
  }
}
