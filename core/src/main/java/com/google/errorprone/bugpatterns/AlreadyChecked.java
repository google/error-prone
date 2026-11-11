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
import static com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.factKey;
import static com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.mayImply;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.lang.String.format;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.Truthiness;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/** Bugpattern to find conditions which are checked more than once. */
@BugPattern(severity = WARNING, summary = "This condition has already been checked.")
public final class AlreadyChecked extends BugChecker implements CompilationUnitTreeMatcher {

  private final ConstantExpressions constantExpressions;

  @Inject
  AlreadyChecked(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Deque<TreePath> enclosingMethod = new ArrayDeque<>();
    private final Deque<List<Condition>> conditionsInMethod = new ArrayDeque<>();

    private final Multiset<ConstantExpression> truths = HashMultiset.create();
    private final Multiset<ConstantExpression> falsehoods = HashMultiset.create();
    private final List<Condition> pending = new ArrayList<>();

    private IfScanner(VisitorState state) {
      super(state);
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      ExpressionTree condition = tree.getCondition();
      scan(condition, null);
      withinScope(condition, /* not= */ false, tree.getThenStatement());
      withinScope(condition, /* not= */ true, tree.getElseStatement());
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
      Tree previous = getCurrentPath().getLeaf();
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
        Condition condition = new Condition(ifTree.getCondition(), /* not= */ then);
        pending.add(condition);
        conditionsInMethod.getLast().add(condition);
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
      conditionsInMethod.addLast(new ArrayList<>());

      runnable.run();

      enclosingMethod.removeLast();
      conditionsInMethod.removeLast().forEach(this::endScope);
    }

    private void withinScope(ExpressionTree conditionTree, boolean not, @Nullable Tree tree) {
      if (tree == null) {
        return;
      }
      Condition condition = new Condition(conditionTree, not);
      pending.add(condition);
      scan(tree, null);
      endScope(condition);
    }

    private void endScope(Condition condition) {
      if (condition.truthiness == null) {
        pending.remove(condition);
      } else {
        removeOccurrences(truths, condition.truthiness.requiredTrue());
        removeOccurrences(falsehoods, condition.truthiness.requiredFalse());
      }
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      ExpressionTree condition = tree.getCondition();
      scan(condition, null);
      withinScope(condition, /* not= */ false, tree.getTrueExpression());
      withinScope(condition, /* not= */ true, tree.getFalseExpression());
      return null;
    }

    @Override
    public Void scan(Tree tree, Void unused) {
      // Fast path out if we don't know anything.
      if (truths.isEmpty() && falsehoods.isEmpty() && pending.isEmpty()) {
        return super.scan(tree, null);
      }
      // As a heuristic, it's fairly harmless to do `if (a) { foo(a); }`. It's possibly nicer than
      // a parameter comment for a literal boolean.
      if (getCurrentPath().getLeaf() instanceof MethodInvocationTree
          || getCurrentPath().getLeaf() instanceof NewClassTree) {
        return super.scan(tree, null);
      }
      if (!(tree instanceof ExpressionTree expressionTree)
          || !isSameType(getType(tree), state.getSymtab().booleanType, state)) {
        return super.scan(tree, null);
      }

      if (tree instanceof IdentifierTree
          && getCurrentPath().getLeaf() instanceof AssignmentTree assignmentTree
          && assignmentTree.getVariable().equals(tree)) {
        return super.scan(tree, null);
      }

      Object key = factKey(expressionTree);
      if (key == null) {
        return super.scan(tree, null);
      }
      pending.removeIf(c -> {
        if (!mayImply(c.tree, c.not, key)) {
          return false;
        }
        c.truthiness = constantExpressions.truthiness(c.tree, c.not, state);
        truths.addAll(c.truthiness.requiredTrue());
        falsehoods.addAll(c.truthiness.requiredFalse());
        return true;
      });
      if (!hasKey(truths, key) && !hasKey(falsehoods, key)) {
        return super.scan(tree, null);
      }

      constantExpressions
          .constantExpression(expressionTree, state)
          .ifPresent(e -> {
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

  /** A condition whose {@link Truthiness} is computed lazily when needed in its scope. */
  private static final class Condition {
    final ExpressionTree tree;
    final boolean not;
    @Nullable Truthiness truthiness;

    Condition(ExpressionTree tree, boolean not) {
      this.tree = tree;
      this.not = not;
    }
  }

  private static boolean hasKey(Multiset<ConstantExpression> facts, Object key) {
    for (ConstantExpression e : facts.elementSet()) {
      if (key.equals(factKey(e))) {
        return true;
      }
    }
    return false;
  }
}
