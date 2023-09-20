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
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
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
    return escapes.get() ? NO_MATCH : describeMatch(tree, attemptFix(tree, state));
  }

  private SuggestedFix attemptFix(VariableTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    if (!symbol.type.toString().startsWith("java.util.concurrent.atomic")) {
      return SuggestedFix.emptyFix();
    }

    AtomicBoolean fixable = new AtomicBoolean(true);
    SuggestedFix.Builder fix = SuggestedFix.builder();

    var constructor = (NewClassTree) tree.getInitializer();

    fix.replace(
        tree,
        format(
            "%s %s = %s;",
            getPrimitiveType(symbol.type, state.getTypes()),
            symbol.getSimpleName(),
            constructor.getArguments().isEmpty()
                ? getDefaultInitializer(symbol, state.getTypes())
                : state.getSourceForNode(constructor.getArguments().get(0))));

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (!getSymbol(tree).equals(symbol)) {
          return super.visitIdentifier(tree, null);
        }
        var parentTree = getCurrentPath().getParentPath().getLeaf();
        if (isVariableDeclarationItself(parentTree)) {
          return super.visitIdentifier(tree, null);
        }
        if (parentTree instanceof MemberSelectTree) {
          var grandparent =
              (MethodInvocationTree) getCurrentPath().getParentPath().getParentPath().getLeaf();
          if (((MemberSelectTree) parentTree).getIdentifier().contentEquals("set")) {
            fix.replace(
                grandparent,
                format(
                    "%s = %s",
                    state.getSourceForNode(tree),
                    state.getSourceForNode(grandparent.getArguments().get(0))));
          } else if (((MemberSelectTree) parentTree).getIdentifier().contentEquals("get")) {
            fix.replace(grandparent, state.getSourceForNode(tree));
          } else if (((MemberSelectTree) parentTree)
              .getIdentifier()
              .contentEquals("getAndIncrement")) {
            fix.replace(grandparent, format("%s++", state.getSourceForNode(tree)));
          } else if (((MemberSelectTree) parentTree)
              .getIdentifier()
              .contentEquals("getAndDecrement")) {
            fix.replace(grandparent, format("%s--", state.getSourceForNode(tree)));
          } else if (((MemberSelectTree) parentTree)
              .getIdentifier()
              .contentEquals("incrementAndGet")) {
            fix.replace(grandparent, format("++%s", state.getSourceForNode(tree)));
          } else if (((MemberSelectTree) parentTree)
              .getIdentifier()
              .contentEquals("decrementAndGet")) {
            fix.replace(grandparent, format("--%s", state.getSourceForNode(tree)));
          } else if (((MemberSelectTree) parentTree)
              .getIdentifier()
              .contentEquals("compareAndSet")) {
            fix.replace(
                grandparent,
                format(
                    "%s = %s",
                    state.getSourceForNode(tree),
                    state.getSourceForNode(grandparent.getArguments().get(1))));
          } else if (((MemberSelectTree) parentTree).getIdentifier().contentEquals("addAndGet")) {
            fix.replace(
                grandparent,
                format(
                    "%s += %s",
                    state.getSourceForNode(tree),
                    state.getSourceForNode(grandparent.getArguments().get(0))));
          } else {
            fixable.set(false);
          }

        } else {
          fixable.set(false);
        }
        return super.visitIdentifier(tree, null);
      }

      private boolean isVariableDeclarationItself(Tree parentTree) {
        return parentTree instanceof VariableTree && getSymbol(parentTree).equals(symbol);
      }
    }.scan(state.getPath().getParentPath(), null);
    return fixable.get() ? fix.build() : SuggestedFix.emptyFix();
  }

  private static String getPrimitiveType(Type type, Types types) {
    String name = types.erasure(type).toString();
    switch (name) {
      case "java.util.concurrent.atomic.AtomicBoolean":
        return "boolean";
      case "java.util.concurrent.atomic.AtomicReference":
        return type.allparams().isEmpty()
            ? "Object"
            : type.allparams().get(0).tsym.getSimpleName().toString();
      case "java.util.concurrent.atomic.AtomicInteger":
        return "int";
      case "java.util.concurrent.atomic.AtomicLong":
        return "long";
      default:
        throw new AssertionError(name);
    }
  }

  private static String getDefaultInitializer(VarSymbol symbol, Types types) {
    String name = types.erasure(symbol.type).toString();
    switch (name) {
      case "java.util.concurrent.atomic.AtomicBoolean":
        return "false";
      case "java.util.concurrent.atomic.AtomicReference":
        return "null";
      case "java.util.concurrent.atomic.AtomicInteger":
      case "java.util.concurrent.atomic.AtomicLong":
        return "0";
      default:
        throw new AssertionError(name);
    }
  }
}
