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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Checks when ByteBuffer.array() is used without calling .arrayOffset() or .hasArray() to ensure
 * safe access to the backing array, or when the buffer wasn't initialized using ByteBuffer.wrap()
 * or ByteBuffer.allocate().
 */
@BugPattern(
    summary =
        "ByteBuffer.array() shouldn't be called unless ByteBuffer.arrayOffset() or ByteBuffer.hasArray() is used or "
            + "if the ByteBuffer was initialized using ByteBuffer.wrap() or ByteBuffer.allocate().",
    severity = WARNING)
public class ByteBufferBackingArray extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> BYTE_BUFFER_ARRAY_MATCHER =
      anyOf(instanceMethod().onDescendantOf(ByteBuffer.class.getName()).named("array"));

  private static final Matcher<ExpressionTree> BYTE_BUFFER_ARRAY_OFFSET_MATCHER =
      anyOf(instanceMethod().onDescendantOf(ByteBuffer.class.getName()).named("arrayOffset"));

  private static final Matcher<ExpressionTree> BYTE_BUFFER_HAS_ARRAY_MATCHER =
      anyOf(instanceMethod().onDescendantOf(ByteBuffer.class.getName()).named("hasArray"));

  private static final Matcher<ExpressionTree> BYTE_BUFFER_ALLOWED_INITIALIZERS_MATCHER =
      staticMethod().onClass(ByteBuffer.class.getName()).namedAnyOf("allocate", "wrap");

  private static final Matcher<ExpressionTree> BYTE_BUFFER_MATCHER = isSameType(ByteBuffer.class);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!BYTE_BUFFER_ARRAY_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // Checks for validating use on method call chain.
    ExpressionTree receiver = tree;
    do {
      receiver = ASTHelpers.getReceiver(receiver);
      if (isValidInitializerOrNotAByteBuffer(receiver, state)) {
        return Description.NO_MATCH;
      }
    } while (receiver instanceof MethodInvocationTree);

    Symbol bufferSymbol = ASTHelpers.getSymbol(receiver);
    if (bufferSymbol == null) {
      return Description.NO_MATCH;
    }

    if (isGuardedByHasArrayTrueBranch(tree, bufferSymbol, state)) {
      return Description.NO_MATCH;
    }

    // Checks for validating use on method scope.
    if (bufferSymbol.owner instanceof MethodSymbol methodSymbol) {
      MethodTree enclosingMethod = ASTHelpers.findMethod(methodSymbol, state);
      if (enclosingMethod == null
          || ValidByteBufferArrayScanner.scan(enclosingMethod, state, bufferSymbol)) {
        return Description.NO_MATCH;
      }
    }

    // Checks for validating use on fields.
    if (bufferSymbol.owner instanceof ClassSymbol classSymbol) {
      ClassTree enclosingClass = ASTHelpers.findClass(classSymbol, state);
      if (enclosingClass == null) {
        return Description.NO_MATCH;
      }
      Optional<? extends Tree> validMemberTree =
          enclosingClass.getMembers().stream()
              .filter(
                  memberTree -> ValidByteBufferArrayScanner.scan(memberTree, state, bufferSymbol))
              .findFirst();
      if (validMemberTree.isPresent()) {
        return Description.NO_MATCH;
      }
    }

    return describeMatch(tree);
  }

  private static boolean isValidInitializerOrNotAByteBuffer(
      ExpressionTree receiver, VisitorState state) {
    return BYTE_BUFFER_ALLOWED_INITIALIZERS_MATCHER.matches(receiver, state)
        || !BYTE_BUFFER_MATCHER.matches(receiver, state);
  }

  /**
   * Scan for a call to ByteBuffer.arrayOffset() or ByteBuffer.hasArray(), or check if buffer was
   * initialized with either ByteBuffer.wrap() or ByteBuffer.allocate().
   */
  private static class ValidByteBufferArrayScanner extends TreeScanner<Void, VisitorState> {

    private final Symbol searchedBufferSymbol;
    private boolean visited;
    private boolean valid;
    private boolean guardActive;

    static boolean scan(Tree tree, VisitorState state, Symbol searchedBufferSymbol) {
      ValidByteBufferArrayScanner visitor = new ValidByteBufferArrayScanner(searchedBufferSymbol);
      tree.accept(visitor, state);
      return visitor.valid;
    }

    private ValidByteBufferArrayScanner(Symbol searchedBufferSymbol) {
      this.searchedBufferSymbol = searchedBufferSymbol;
    }

    @Override
    public Void visitVariable(VariableTree tree, VisitorState state) {
      checkForInitializer(ASTHelpers.getSymbol(tree), tree.getInitializer(), state);
      return super.visitVariable(tree, state);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, VisitorState state) {
      checkForInitializer(ASTHelpers.getSymbol(tree.getVariable()), tree.getExpression(), state);
      return super.visitAssignment(tree, state);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (valid) {
        return null;
      }
      Symbol bufferSymbol = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
      if (searchedBufferSymbol.equals(bufferSymbol)) {
        if (BYTE_BUFFER_ARRAY_MATCHER.matches(tree, state)) {
          visited = true;
          if (guardActive) {
            valid = true;
          }
        } else if (BYTE_BUFFER_ARRAY_OFFSET_MATCHER.matches(tree, state)) {
          valid = true;
        }
      }
      return super.visitMethodInvocation(tree, state);
    }

    @Override
    public Void visitIf(IfTree tree, VisitorState state) {
      if (valid) {
        return null;
      }
      boolean thenGuard =
          conditionIsConjunctWithPositiveHasArray(tree.getCondition(), searchedBufferSymbol, state);
      boolean elseGuard =
          conditionIsNegatedConjunctWithPositiveHasArray(
              tree.getCondition(), searchedBufferSymbol, state);

      // Scan THEN, optionally under guard
      boolean oldValid = valid;
      boolean oldGuard = guardActive;
      guardActive = guardActive || thenGuard;
      scan(tree.getThenStatement(), state);
      valid = oldValid || valid; // preserve any success found in THEN
      guardActive = oldGuard;

      // Scan ELSE, optionally under guard
      if (tree.getElseStatement() != null) {
        oldValid = valid;
        oldGuard = guardActive;
        guardActive = guardActive || elseGuard;
        scan(tree.getElseStatement(), state);
        valid = oldValid || valid; // preserve any success found in ELSE
        guardActive = oldGuard;
      }
      return null;
    }

    private void checkForInitializer(
        Symbol foundSymbol, ExpressionTree expression, VisitorState state) {
      if (visited || valid) {
        return;
      }
      if (!searchedBufferSymbol.equals(foundSymbol)) {
        return;
      }
      if (expression == null) {
        return;
      }
      if (ValidByteBufferInitializerScanner.scan(expression, state)) {
        valid = true;
      }
    }
  }

  private static boolean isGuardedByHasArrayTrueBranch(
      MethodInvocationTree arrayCall, Symbol bufferSymbol, VisitorState state) {
    var path = state.getPath();
    while (path != null) {
      Tree leaf = path.getLeaf();
      if (leaf instanceof IfTree ifTree) {
        // array() is inside THEN branch guarded by a conjunction that includes hasArray()
        if (containsTree(ifTree.getThenStatement(), arrayCall)
            && conditionIsConjunctWithPositiveHasArray(ifTree.getCondition(), bufferSymbol, state)) {
          return true;
        }
        // array() is inside ELSE branch and condition is negation of a conjunction including hasArray()
        if (ifTree.getElseStatement() != null
            && containsTree(ifTree.getElseStatement(), arrayCall)
            && conditionIsNegatedConjunctWithPositiveHasArray(
                ifTree.getCondition(), bufferSymbol, state)) {
          return true;
        }
      }
      path = path.getParentPath();
    }
    return false;
  }

  private static boolean containsTree(Tree container, Tree target) {
    if (container == null) {
      return false;
    }
    class Finder extends TreeScanner<Boolean, Void> {
      @Override
      public Boolean reduce(Boolean r1, Boolean r2) {
        return firstNonNull(r1, false) || firstNonNull(r2, false);
      }

      @Override
      public Boolean scan(Tree node, Void unused) {
        if (node == null) {
          return false;
        }
        if (node == target) {
          return true;
        }
        return super.scan(node, unused);
      }
    }
    return firstNonNull(container.accept(new Finder(), null), false);
  }

  private static boolean conditionIsConjunctWithPositiveHasArray(
      ExpressionTree condition, Symbol bufferSymbol, VisitorState state) {
    return containsPositiveHasArray(condition, /*negated=*/ false, bufferSymbol, state)
        && !containsLogicalOr(condition);
  }

  private static boolean conditionIsNegatedConjunctWithPositiveHasArray(
      ExpressionTree condition, Symbol bufferSymbol, VisitorState state) {
    return containsPositiveHasArray(condition, /*negated=*/ true, bufferSymbol, state)
        && !containsLogicalOr(condition);
  }

  private static boolean containsPositiveHasArray(
      ExpressionTree tree, boolean negated, Symbol bufferSymbol, VisitorState state) {
      return switch (tree.getKind()) {
          case PARENTHESIZED -> containsPositiveHasArray(
                  ((ParenthesizedTree) tree).getExpression(), negated, bufferSymbol, state);
          case LOGICAL_COMPLEMENT -> containsPositiveHasArray(
                  ((UnaryTree) tree).getExpression(), !negated, bufferSymbol, state);
          case CONDITIONAL_AND, CONDITIONAL_OR -> {
              BinaryTree bt = (BinaryTree) tree;
              yield containsPositiveHasArray(bt.getLeftOperand(), negated, bufferSymbol, state)
                      || containsPositiveHasArray(bt.getRightOperand(), negated, bufferSymbol, state);
          }
          default -> {
              if (tree instanceof MethodInvocationTree mit
                      && BYTE_BUFFER_HAS_ARRAY_MATCHER.matches(mit, state)) {
                  Symbol recv = ASTHelpers.getSymbol(ASTHelpers.getReceiver(mit));
                  yield !negated && bufferSymbol.equals(recv);
              }
              yield false;
          }
      };
  }

  private static boolean containsLogicalOr(ExpressionTree tree) {
    class OrFinder extends TreeScanner<Boolean, Void> {
      @Override
      public Boolean reduce(Boolean r1, Boolean r2) {
        return firstNonNull(r1, false) || firstNonNull(r2, false);
      }

      @Override
      public Boolean visitBinary(BinaryTree node, Void unused) {
        if (node.getKind() == Kind.CONDITIONAL_OR) {
          return true;
        }
        return super.visitBinary(node, unused);
      }
    }
    return firstNonNull(tree.accept(new OrFinder(), null), false);
  }

  /** Scan for a call to ByteBuffer.wrap() or ByteBuffer.allocate(). */
  private static class ValidByteBufferInitializerScanner
      extends TreeScanner<Boolean, VisitorState> {

    static Boolean scan(ExpressionTree tree, VisitorState state) {
      ValidByteBufferInitializerScanner visitor = new ValidByteBufferInitializerScanner();
      return firstNonNull(tree.accept(visitor, state), false);
    }

    private ValidByteBufferInitializerScanner() {}

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      boolean b1 = BYTE_BUFFER_ALLOWED_INITIALIZERS_MATCHER.matches(tree, state);
      boolean b2 = super.visitMethodInvocation(tree, state);
      return b1 || b2;
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
      return firstNonNull(r1, false) || firstNonNull(r2, false);
    }
  }
}
