/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;

/**
 * An abstract matcher for implicit and explicit calls to {@code Object.toString()}, for use
 * on types that do not have a human-readable {@code toString()} implementation.
 *
 * <p>See examples in {@link StreamToString} and {@link ArrayToString}.
 */
public abstract class AbstractToString extends BugChecker
    implements MethodInvocationTreeMatcher, IdentifierTreeMatcher, MemberSelectTreeMatcher {

  /** The type to match on. */
  protected abstract TypePredicate typePredicate();

  /**
   * Constructs a fix for an implicit toString call, e.g. from string concatenation or from
   * passing an argument to {@code println} or {@code StringBuilder.append}.
   *
   * @param tree the tree node for the expression being converted to a String
   */
  protected abstract Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state);

  /**
   * Constructs a fix for an explicit toString call, e.g. from {@code Object.toString()} or
   * {@code String.valueOf()}.
   *
   * @param parent the expression's parent (e.g. {@code String.valueOf(expression)})
   */
  protected abstract Optional<Fix> toStringFix(
      Tree parent, ExpressionTree expression, VisitorState state);

  private static final Matcher<ExpressionTree> TO_STRING =
      instanceMethod().onDescendantOf("java.lang.Object").withSignature("toString()");

  private static final Matcher<ExpressionTree> PRINT_STRING =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.io.PrintStream")
              .withSignature("print(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.io.PrintStream")
              .withSignature("println(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.lang.StringBuilder")
              .withSignature("append(java.lang.Object)"));

  private static final Matcher<ExpressionTree> VALUE_OF =
      staticMethod().onClass("java.lang.String").withSignature("valueOf(java.lang.Object)");

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return checkToString(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return checkToString(tree, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    if (TO_STRING.matches(tree, state)
        && typePredicate().apply(ASTHelpers.getType(receiver), state)) {
      return maybeFix(tree, toStringFix(tree, receiver, state));
    }
    return checkToString(tree, state);
  }

  /**
   * Tests if the given expression is converted to a String by its parent (i.e. its parent
   * is a string concat expression, {@code String.format}, or {@code println(Object)}).
   */
  private Description checkToString(ExpressionTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (!(sym instanceof VarSymbol || sym instanceof MethodSymbol)) {
      return Description.NO_MATCH;
    }
    Type type = ASTHelpers.getType(tree);
    if (type instanceof MethodType) {
      type = type.getReturnType();
    }
    if (!typePredicate().apply(type, state)) {
      return Description.NO_MATCH;
    }
    // is the enclosing expression string concat?
    Tree parent = state.getPath().getParentPath().getLeaf();
    if ((parent.getKind() == Kind.PLUS || parent.getKind() == Kind.PLUS_ASSIGNMENT)
        && state.getTypes().isSameType(ASTHelpers.getType(parent), state.getSymtab().stringType)) {
      return maybeFix(tree, implicitToStringFix(tree, state));
    }
    if (parent instanceof ExpressionTree) {
      ExpressionTree parentExpression = (ExpressionTree) parent;
      // the enclosing method is print() or println()
      if (PRINT_STRING.matches(parentExpression, state)) {
        return maybeFix(tree, implicitToStringFix(tree, state));
      }
      // the enclosing method is String.valueOf()
      if (VALUE_OF.matches(parentExpression, state)) {
        return maybeFix(tree, toStringFix(parentExpression, tree, state));
      }
    }
    return Description.NO_MATCH;
  }

  private Description maybeFix(Tree tree, Optional<Fix> fix) {
    return fix.isPresent() ? describeMatch(tree, fix.get()) : describeMatch(tree);
  }
}
