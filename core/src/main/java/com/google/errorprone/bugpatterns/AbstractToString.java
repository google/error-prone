/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
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
 * An abstract matcher for implicit and explicit calls to {@code Object.toString()}, for use on
 * types that do not have a human-readable {@code toString()} implementation.
 *
 * <p>See examples in {@link StreamToString} and {@link ArrayToString}.
 */
public abstract class AbstractToString extends BugChecker
    implements MethodInvocationTreeMatcher, IdentifierTreeMatcher, MemberSelectTreeMatcher {

  /** The type to match on. */
  protected abstract TypePredicate typePredicate();

  /**
   * Constructs a fix for an implicit toString call, e.g. from string concatenation or from passing
   * an argument to {@code println} or {@code StringBuilder.append}.
   *
   * @param tree the tree node for the expression being converted to a String
   */
  protected abstract Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state);

  /** Adds the description message for match on the type without fixes. */
  protected Optional<String> descriptionMessageForDefaultMatch(Type type, VisitorState state) {
    return Optional.absent();
  }

  /**
   * Constructs a fix for an explicit toString call, e.g. from {@code Object.toString()} or {@code
   * String.valueOf()}.
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
    Type receiverType = ASTHelpers.getType(receiver);
    if (TO_STRING.matches(tree, state) && typePredicate().apply(receiverType, state)) {
      return maybeFix(tree, state, receiverType, toStringFix(tree, receiver, state));
    }
    return checkToString(tree, state);
  }

  /**
   * Tests if the given expression is converted to a String by its parent (i.e. its parent is a
   * string concat expression, {@code String.format}, or {@code println(Object)}).
   */
  private Description checkToString(ExpressionTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (!(sym instanceof VarSymbol || sym instanceof MethodSymbol)) {
      return NO_MATCH;
    }
    Type type = ASTHelpers.getType(tree);
    if (type instanceof MethodType) {
      type = type.getReturnType();
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    ToStringKind toStringKind = isToString(parent, tree, state);
    if (toStringKind == ToStringKind.NONE) {
      return NO_MATCH;
    }
    if (!typePredicate().apply(type, state)) {
      return NO_MATCH;
    }
    Optional<Fix> fix;
    switch (toStringKind) {
      case IMPLICIT:
        fix = implicitToStringFix(tree, state);
        break;
      case EXPLICIT:
        fix = toStringFix(parent, tree, state);
        break;
      default:
        throw new AssertionError(toStringKind);
    }
    return maybeFix(tree, state, type, fix);
  }

  enum ToStringKind {
    /** String concatenation, or an enclosing print method. */
    IMPLICIT,
    /** {@code String.valueOf()} or {@code #toString()}. */
    EXPLICIT,
    NONE
  }

  /** Classifies expressions that are converted to strings by their enclosing expression. */
  ToStringKind isToString(Tree parent, ExpressionTree tree, VisitorState state) {
    // is the enclosing expression string concat?
    if (isStringConcat(parent, state)) {
      return ToStringKind.IMPLICIT;
    }
    if (parent instanceof ExpressionTree) {
      ExpressionTree parentExpression = (ExpressionTree) parent;
      // the enclosing method is print() or println()
      if (PRINT_STRING.matches(parentExpression, state)) {
        return ToStringKind.IMPLICIT;
      }
      // the enclosing method is String.valueOf()
      if (VALUE_OF.matches(parentExpression, state)) {
        return ToStringKind.EXPLICIT;
      }
    }
    return ToStringKind.NONE;
  }

  private boolean isStringConcat(Tree tree, VisitorState state) {
    return (tree.getKind() == Kind.PLUS || tree.getKind() == Kind.PLUS_ASSIGNMENT)
        && state.getTypes().isSameType(ASTHelpers.getType(tree), state.getSymtab().stringType);
  }

  private Description maybeFix(Tree tree, VisitorState state, Type matchedType, Optional<Fix> fix) {
    Description.Builder description = buildDescription(tree);
    if (fix.isPresent()) {
      description.addFix(fix.get());
    }
    Optional<String> summary = descriptionMessageForDefaultMatch(matchedType, state);
    if (summary.isPresent()) {
      description.setMessage(summary.get());
    }
    return description.build();
  }
}
