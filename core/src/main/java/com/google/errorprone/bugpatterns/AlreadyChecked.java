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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
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
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    private final Deque<TreePath> enclosingMethod = new ArrayDeque<>();
    private final Deque<Set<ConstantBooleanExpression>> truthsInMethod = new ArrayDeque<>();
    private final Deque<Set<ConstantBooleanExpression>> falsehoodsInMethod = new ArrayDeque<>();

    private final Multiset<ConstantBooleanExpression> truths = HashMultiset.create();
    private final Multiset<ConstantBooleanExpression> falsehoods = HashMultiset.create();
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      scan(tree.getCondition(), null);
      Truthiness truthiness =
          constantExpressions.truthiness(tree.getCondition(), /* not= */ false, state);

      withinScope(truthiness, tree.getThenStatement());

      withinScope(
          constantExpressions.truthiness(tree.getCondition(), /* not= */ true, state),
          tree.getElseStatement());
      return null;
    }

    @Override
    public Void visitReturn(ReturnTree tree, Void unused) {
      super.visitReturn(tree, null);
      handleMethodExitingStatement();
      return null;
    }

    @Override
    public Void visitThrow(ThrowTree tree, Void unused) {
      super.visitThrow(tree, null);
      handleMethodExitingStatement();
      return null;
    }

    private void handleMethodExitingStatement() {
      TreePath ifPath = getCurrentPath().getParentPath();
      Tree previous = null;
      while (ifPath != null && ifPath.getLeaf() instanceof BlockTree) {
        previous = ifPath.getLeaf();
        ifPath = ifPath.getParentPath();
      }
      if (ifPath == null) {
        return;
      }
      TreePath methodPath = escapeBlock(ifPath.getParentPath());
      if (methodPath == null || !(ifPath.getLeaf() instanceof IfTree)) {
        return;
      }
      IfTree ifTree = (IfTree) ifPath.getLeaf();
      boolean then = ifTree.getThenStatement().equals(previous);

      if (!enclosingMethod.isEmpty()
          && enclosingMethod.getLast().getLeaf().equals(methodPath.getLeaf())) {
        Truthiness truthiness =
            constantExpressions.truthiness(ifTree.getCondition(), /* not= */ then, state);
        truths.addAll(truthiness.requiredTrue());
        falsehoods.addAll(truthiness.requiredFalse());
        truthsInMethod.getLast().addAll(truthiness.requiredTrue());
        falsehoodsInMethod.getLast().addAll(truthiness.requiredFalse());
      }
    }

    private @Nullable TreePath escapeBlock(@Nullable TreePath path) {
      while (path != null && path.getLeaf() instanceof BlockTree) {
        path = path.getParentPath();
      }
      return path;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
      withinMethod(() -> super.visitLambdaExpression(tree, null));
      return null;
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      withinMethod(() -> super.visitMethod(tree, null));
      return null;
    }

    private void withinMethod(Runnable runnable) {
      enclosingMethod.addLast(getCurrentPath());
      truthsInMethod.addLast(new HashSet<>());
      falsehoodsInMethod.addLast(new HashSet<>());

      runnable.run();

      enclosingMethod.removeLast();
      removeOccurrences(truths, truthsInMethod.removeLast());
      removeOccurrences(falsehoods, falsehoodsInMethod.removeLast());
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
      scan(tree.getCondition(), null);
      Truthiness truthiness =
          constantExpressions.truthiness(tree.getCondition(), /* not= */ false, state);

      withinScope(truthiness, tree.getTrueExpression());

      withinScope(
          constantExpressions.truthiness(tree.getCondition(), /* not= */ true, state),
          tree.getFalseExpression());
      return null;
    }

    @Override
    public Void scan(Tree tree, Void unused) {
      // Fast path out if we don't know anything.
      if (truths.isEmpty() && falsehoods.isEmpty()) {
        return super.scan(tree, null);
      }
      // As a heuristic, it's fairly harmless to do `if (a) { foo(a); }`. It's possibly nicer than
      // a parameter comment for a literal boolean.
      if (getCurrentPath().getLeaf() instanceof MethodInvocationTree
          || getCurrentPath().getLeaf() instanceof NewClassTree) {
        return super.scan(tree, null);
      }
      if (!(tree instanceof ExpressionTree)
          || !isSameType(getType(tree), state.getSymtab().booleanType, state)) {
        return super.scan(tree, null);
      }
      constantExpressions
          .constantBooleanExpression((ExpressionTree) tree, state)
          .ifPresent(
              e -> {
                if (truths.contains(e)) {
                  state.reportMatch(
                      buildDescription(tree)
                          .setMessage(
                              format(
                                  "This condition (on %s) is already known to be true; it (or its"
                                      + " complement) has already been checked.",
                                  e))
                          .build());
                }

                if (falsehoods.contains(e)) {
                  state.reportMatch(
                      buildDescription(tree)
                          .setMessage(
                              format(
                                  "This condition (on %s) is known to be false here. It (or its"
                                      + " complement) has already been checked.",
                                  e))
                          .build());
                }
              });
      return super.scan(tree, null);
    }
  }
}
