/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.CONDITIONAL_AND;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Discourages implementing {@code equals} using {@code hashCode}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    summary =
        "Implementing #equals by just comparing hashCodes is fragile. Hashes collide "
            + "frequently, and this will lead to false positives in #equals.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public final class EqualsUsingHashCode extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> HASH_CODE =
      instanceMethod().anyClass().named("hashCode");

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(HASH_CODE, enclosingMethod(equalsMethodDeclaration()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ReturnTree returnTree = state.findEnclosing(ReturnTree.class);
    if (returnTree != null) {
      return matchDirectHashCodeInReturn(tree, returnTree);
    }
    return matchHashCodeExtractedToLocal(tree, state);
  }

  private Description matchDirectHashCodeInReturn(
      MethodInvocationTree tree, ReturnTree returnTree) {
    AtomicBoolean isTerminalCondition = new AtomicBoolean(false);
    returnTree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree methodTree, Void unused) {
            if (methodTree.equals(tree)) {
              isTerminalCondition.set(true);
            }
            return super.visitMethodInvocation(methodTree, null);
          }

          @Override
          public Void visitBinary(BinaryTree binaryTree, Void unused) {
            return scan(binaryTree.getRightOperand(), null);
          }
        },
        null);
    return isTerminalCondition.get() ? describeMatch(tree) : NO_MATCH;
  }

  /**
   * Flags {@code equals} methods that store both {@code hashCode()} results in locals and then
   * return a comparison of those locals as the terminal condition (optionally guarded by {@code
   * &&}).
   */
  private Description matchHashCodeExtractedToLocal(MethodInvocationTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof VariableTree variableTree)
        || !tree.equals(variableTree.getInitializer())) {
      return NO_MATCH;
    }
    MethodTree methodTree = state.findEnclosing(MethodTree.class);
    if (methodTree == null || methodTree.getBody() == null) {
      return NO_MATCH;
    }

    Map<VarSymbol, MethodInvocationTree> hashCodeLocals = new HashMap<>();
    Set<VarSymbol> reassigned = new HashSet<>();
    methodTree
        .getBody()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitVariable(VariableTree node, Void unused) {
                ExpressionTree initializer = node.getInitializer();
                if (initializer != null && HASH_CODE.matches(initializer, state)) {
                  Symbol sym = getSymbol(node);
                  if (sym instanceof VarSymbol varSymbol) {
                    hashCodeLocals.put(varSymbol, (MethodInvocationTree) initializer);
                  }
                }
                return super.visitVariable(node, null);
              }

              @Override
              public Void visitAssignment(AssignmentTree node, Void unused) {
                Symbol sym = getSymbol(node.getVariable());
                if (sym instanceof VarSymbol varSymbol) {
                  reassigned.add(varSymbol);
                }
                return super.visitAssignment(node, null);
              }
            },
            null);
    reassigned.forEach(hashCodeLocals::remove);
    if (hashCodeLocals.size() < 2) {
      return NO_MATCH;
    }

    AtomicReference<ReturnTree> soleReturn = new AtomicReference<>();
    AtomicBoolean multipleReturns = new AtomicBoolean(false);
    methodTree
        .getBody()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitReturn(ReturnTree node, Void unused) {
                if (soleReturn.get() == null) {
                  soleReturn.set(node);
                } else {
                  multipleReturns.set(true);
                }
                return null;
              }
            },
            null);
    if (multipleReturns.get()
        || soleReturn.get() == null
        || soleReturn.get().getExpression() == null) {
      return NO_MATCH;
    }

    ExpressionTree terminal = terminalCondition(soleReturn.get().getExpression());
    if (!(terminal instanceof BinaryTree binaryTree) || binaryTree.getKind() != EQUAL_TO) {
      return NO_MATCH;
    }
    if (!isHashCodeValue(binaryTree.getLeftOperand(), hashCodeLocals, state)
        || !isHashCodeValue(binaryTree.getRightOperand(), hashCodeLocals, state)) {
      return NO_MATCH;
    }

    // Report once, on the hashCode() call that initializes the left-hand local when possible.
    if (terminalLeftInitializedBy(tree, binaryTree.getLeftOperand(), hashCodeLocals)) {
      return describeMatch(tree);
    }
    // Fall back: if the left side is a direct hashCode() call and this is it, report.
    if (binaryTree.getLeftOperand().equals(tree)) {
      return describeMatch(tree);
    }
    return NO_MATCH;
  }

  /** Walks right through {@code &&} chains to match the existing "terminal condition" behavior. */
  private static ExpressionTree terminalCondition(ExpressionTree expression) {
    ExpressionTree current = expression;
    while (current instanceof BinaryTree binaryTree && binaryTree.getKind() == CONDITIONAL_AND) {
      current = binaryTree.getRightOperand();
    }
    return current;
  }

  private static boolean isHashCodeValue(
      ExpressionTree expression,
      Map<VarSymbol, MethodInvocationTree> hashCodeLocals,
      VisitorState state) {
    if (expression.getKind() == METHOD_INVOCATION && HASH_CODE.matches(expression, state)) {
      return true;
    }
    if (expression.getKind() == IDENTIFIER) {
      Symbol sym = getSymbol((IdentifierTree) expression);
      return sym instanceof VarSymbol varSymbol && hashCodeLocals.containsKey(varSymbol);
    }
    return false;
  }

  private static boolean terminalLeftInitializedBy(
      MethodInvocationTree tree,
      ExpressionTree leftOperand,
      Map<VarSymbol, MethodInvocationTree> hashCodeLocals) {
    if (leftOperand.getKind() != IDENTIFIER) {
      return false;
    }
    Symbol sym = getSymbol((IdentifierTree) leftOperand);
    if (!(sym instanceof VarSymbol varSymbol)) {
      return false;
    }
    return tree.equals(hashCodeLocals.get(varSymbol));
  }
}
