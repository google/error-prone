/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = SeverityLevel.WARNING,
    summary =
        "Variables which are initialized and do not escape the current scope do not need to worry"
            + " about concurrency. Using the non-concurrent type will reduce overhead and"
            + " verbosity.")
public final class UnnecessaryAsync extends BugChecker implements VariableTreeMatcher {
  private static final Matcher<ExpressionTree> NEW_SYNCHRONIZED_THING =
      anyOf(
          Stream.of(
                  "java.util.concurrent.atomic.AtomicBoolean",
                  "java.util.concurrent.atomic.AtomicReference",
                  "java.util.concurrent.atomic.AtomicInteger",
                  "java.util.concurrent.atomic.AtomicLong",
                  "java.util.concurrent.ConcurrentHashMap")
              .map(x -> constructor().forClass(x))
              .collect(toImmutableList()));

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    if (!symbol.getKind().equals(ElementKind.LOCAL_VARIABLE) || !isConsideredFinal(symbol)) {
      return NO_MATCH;
    }
    var initializer = tree.getInitializer();
    if (initializer == null || !NEW_SYNCHRONIZED_THING.matches(initializer, state)) {
      return NO_MATCH;
    }
    AtomicBoolean escapes = new AtomicBoolean(false);
    new TreePathScanner<Void, Void>() {
      int lambdaDepth = 0;

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        lambdaDepth++;
        var ret = super.visitMethod(tree, null);
        lambdaDepth--;
        return ret;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
        lambdaDepth++;
        var ret = super.visitLambdaExpression(tree, null);
        lambdaDepth--;
        return ret;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (!getSymbol(tree).equals(symbol)) {
          return super.visitIdentifier(tree, null);
        }
        // We're in a lambda, so our symbol implicitly escapes.
        if (lambdaDepth > 0) {
          escapes.set(true);
          return super.visitIdentifier(tree, null);
        }
        var parentTree = getCurrentPath().getParentPath().getLeaf();
        // Anything other than a method invocation on our symbol constitutes a reference to it
        // escaping.
        if (isVariableDeclarationItself(parentTree) || parentTree instanceof MemberSelectTree) {
          return super.visitIdentifier(tree, null);
        }
        escapes.set(true);
        return super.visitIdentifier(tree, null);
      }

      private boolean isVariableDeclarationItself(Tree parentTree) {
        return parentTree instanceof VariableTree && getSymbol(parentTree).equals(symbol);
      }
    }.scan(state.getPath().getParentPath(), null);
    // TODO(ghm): Include an attempted fix, if possible.
    return escapes.get() ? NO_MATCH : describeMatch(tree);
  }
}
