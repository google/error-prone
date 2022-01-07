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
import static java.lang.String.format;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantBooleanExpression;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.Truthiness;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.Tree;
import java.util.Set;

/** Bugpattern to find conditions which are checked more than once. */
@BugPattern(
    name = "AlreadyChecked",
    severity = WARNING,
    summary = "This condition has already been checked.")
public final class AlreadyChecked extends BugChecker implements CompilationUnitTreeMatcher {

  private final ConstantExpressions constantExpressions;

  public AlreadyChecked(ErrorProneFlags flags) {
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Multiset<ConstantBooleanExpression> truths = HashMultiset.create();
    private final Multiset<ConstantBooleanExpression> falsehoods = HashMultiset.create();
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      Truthiness truthiness =
          constantExpressions.truthiness(tree.getCondition(), /* not= */ false, state);

      checkCondition(tree.getCondition(), truthiness);

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
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      checkCondition(
          tree.getCondition(), constantExpressions.truthiness(tree.getCondition(), false, state));
      return super.visitConditionalExpression(tree, null);
    }

    void checkCondition(Tree tree, Truthiness truthiness) {
      Set<ConstantBooleanExpression> alreadyKnownFalsehoods =
          union(
              intersection(truthiness.requiredTrue(), falsehoods.elementSet()),
              intersection(truthiness.requiredFalse(), truths.elementSet()));
      if (!alreadyKnownFalsehoods.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(
                    format(
                        "This condition (on %s) is known to be false here. It (or its complement)"
                            + " has already been checked.",
                        alreadyKnownFalsehoods))
                .build());
      }
      Set<ConstantBooleanExpression> alreadyKnownTruths =
          union(
              intersection(truthiness.requiredTrue(), truths.elementSet()),
              intersection(truthiness.requiredFalse(), falsehoods.elementSet()));
      if (!alreadyKnownTruths.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(
                    format(
                        "This condition (on %s) is already known to be true; it (or its complement)"
                            + " has already been checked.",
                        alreadyKnownTruths))
                .build());
      }
    }
  }
}
