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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Checks when ByteBuffer.array() is used without calling .arrayOffset() to know the offset of the
 * array, or when the buffer wasn't initialized using ByteBuffer.wrap() or ByteBuffer.allocate().
 */
@BugPattern(
    name = "ByteBufferBackingArray",
    summary =
        "ByteBuffer.array() shouldn't be called unless ByteBuffer.arrayOffset() is used or "
            + "if the ByteBuffer was initialized using ByteBuffer.wrap() or ByteBuffer.allocate().",
    severity = WARNING)
public class ByteBufferBackingArray extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> BYTE_BUFFER_ARRAY_MATCHER =
      anyOf(instanceMethod().onDescendantOf(ByteBuffer.class.getName()).named("array"));

  private static final Matcher<ExpressionTree> BYTE_BUFFER_ARRAY_OFFSET_MATCHER =
      anyOf(instanceMethod().onDescendantOf(ByteBuffer.class.getName()).named("arrayOffset"));

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

    // Checks for validating use on method scope.
    if (bufferSymbol.owner instanceof MethodSymbol) {
      MethodTree enclosingMethod = ASTHelpers.findMethod((MethodSymbol) bufferSymbol.owner, state);
      if (enclosingMethod == null
          || ValidByteBufferArrayScanner.scan(enclosingMethod, state, bufferSymbol)) {
        return Description.NO_MATCH;
      }
    }

    // Checks for validating use on fields.
    if (bufferSymbol.owner instanceof ClassSymbol) {
      ClassTree enclosingClass = ASTHelpers.findClass((ClassSymbol) bufferSymbol.owner, state);
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
   * Scan for a call to ByteBuffer.arrayOffset() or check if buffer was initialized with either
   * ByteBuffer.wrap() or ByteBuffer.allocate().
   */
  private static class ValidByteBufferArrayScanner extends TreeScanner<Void, VisitorState> {

    private final Symbol searchedBufferSymbol;
    private boolean visited;
    private boolean valid;

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
        } else if (BYTE_BUFFER_ARRAY_OFFSET_MATCHER.matches(tree, state)) {
          valid = true;
        }
      }
      return super.visitMethodInvocation(tree, state);
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
