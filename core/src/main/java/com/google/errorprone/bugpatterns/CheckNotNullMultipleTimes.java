/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Checks for the same variable being checked against null twice in a method. */
@BugPattern(
    severity = ERROR,
    summary = "A variable was checkNotNulled multiple times. Did you mean to check something else?")
public final class CheckNotNullMultipleTimes extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<ExpressionTree> CHECK_NOT_NULL =
      staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Multiset<VarSymbol> variables = HashMultiset.create();
    Map<VarSymbol, Tree> lastCheck = new HashMap<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        List<? extends ExpressionTree> arguments = tree.getArguments();
        if (!arguments.isEmpty()
            && arguments.get(0) instanceof IdentifierTree
            // Only consider side-effects-only calls to checkNotNull: it turns out people often
            // intentionally write repeated null-checks for the same variable when using the result
            // as a value, and we would have many false positives if we counted those.
            && getCurrentPath().getParentPath().getLeaf() instanceof StatementTree
            && CHECK_NOT_NULL.matches(tree, state)) {
          Symbol symbol = getSymbol(arguments.get(0));
          if (symbol instanceof VarSymbol && isConsideredFinal(symbol)) {
            variables.add((VarSymbol) symbol);
            lastCheck.put((VarSymbol) symbol, tree);
          }
        }
        return super.visitMethodInvocation(tree, null);
      }

      // Don't descend into ifs and switches, given people often repeat the same checks within
      // top-level conditional branches.
      @Override
      public Void visitSwitch(SwitchTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitIf(IfTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        return null;
      }

      // Sometimes a variable is checked in the try and the catch blocks of a try statement; don't
      // descend into the catch.
      // try {
      //   <some code that might throw>
      //   checkNotNull(frobnicator);
      //   <more code>
      // } catch (Exception e) {
      //   checkNotNull(frobnicator);
      // }
      @Override
      public Void visitTry(TryTree tree, Void unused) {
        return scan(tree.getBlock(), null);
      }
    }.scan(state.getPath(), null);
    for (Multiset.Entry<VarSymbol> entry : variables.entrySet()) {
      if (entry.getCount() > 1) {
        state.reportMatch(
            buildDescription(lastCheck.get(entry.getElement()))
                .setMessage(
                    String.format(
                        "checkNotNull(%s) was called more than once. Did you mean to check"
                            + " something else?",
                        entry.getElement()))
                .build());
      }
    }
    return NO_MATCH;
  }
}
