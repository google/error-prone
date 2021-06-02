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
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeKind;

/**
 * An abstract matcher for implicit and explicit calls to {@code Object.toString()}, for use on
 * types that do not have a human-readable {@code toString()} implementation.
 *
 * <p>See examples in {@link StreamToString} and {@link ArrayToString}.
 */
public abstract class AbstractToString extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher, CompoundAssignmentTreeMatcher {

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
    return Optional.empty();
  }

  /** Whether this kind of toString call is allowable for this check. */
  protected boolean allowableToStringKind(ToStringKind toStringKind) {
    return false;
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
      instanceMethod().anyClass().named("toString").withNoParameters();

  private static final Matcher<ExpressionTree> FLOGGER_LOG =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  private static final Matcher<ExpressionTree> FORMAT_METHOD =
      symbolHasAnnotation(FormatMethod.class);

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod().onClass("java.lang.String").named("format");

  private static final Matcher<ExpressionTree> VALUE_OF =
      staticMethod()
          .onClass("java.lang.String")
          .named("valueOf")
          .withParameters("java.lang.Object");

  private static final Matcher<ExpressionTree> PRINT_STRING =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.io.PrintStream")
              .namedAnyOf("print", "println")
              .withParameters("java.lang.Object"),
          instanceMethod()
              .onExactClass("java.lang.StringBuilder")
              .named("append")
              .withParameters("java.lang.Object"));

  private static boolean isInVarargsPosition(
      ExpressionTree argTree, MethodInvocationTree methodInvocationTree, VisitorState state) {
    int parameterCount = getSymbol(methodInvocationTree).getParameters().size();
    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    // Don't match if we're passing an array into a varargs parameter, but do match if there are
    // other parameters along with it.
    return (arguments.size() > parameterCount || !state.getTypes().isArray(getType(argTree)))
        && arguments.indexOf(argTree) >= parameterCount - 1;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (PRINT_STRING.matches(tree, state)) {
      for (ExpressionTree argTree : tree.getArguments()) {
        handleStringifiedTree(argTree, ToStringKind.IMPLICIT, state);
      }
    }
    if (VALUE_OF.matches(tree, state)) {
      for (ExpressionTree argTree : tree.getArguments()) {
        handleStringifiedTree(
            tree,
            argTree,
            ToStringKind.EXPLICIT,
            state.withPath(new TreePath(state.getPath(), argTree)));
      }
    }
    if (TO_STRING.matches(tree, state)) {
      ExpressionTree receiver = getReceiver(tree);
      if (receiver != null) {
        handleStringifiedTree(tree, receiver, ToStringKind.EXPLICIT, state);
      }
    }
    if (FORMAT_METHOD.matches(tree, state)) {
      for (ExpressionTree argTree : tree.getArguments()) {
        if (isInVarargsPosition(argTree, tree, state)) {
          handleStringifiedTree(argTree, ToStringKind.FORMAT_METHOD, state);
        }
      }
    }
    if (STRING_FORMAT.matches(tree, state)) {
      for (ExpressionTree argTree : tree.getArguments()) {
        if (isInVarargsPosition(argTree, tree, state)) {
          handleStringifiedTree(argTree, ToStringKind.IMPLICIT, state);
        }
      }
    }
    if (FLOGGER_LOG.matches(tree, state)) {
      for (ExpressionTree argTree : tree.getArguments()) {
        handleStringifiedTree(argTree, ToStringKind.FLOGGER, state);
      }
    }
    return NO_MATCH;
  }

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!state.getTypes().isSameType(getType(tree), state.getSymtab().stringType)) {
      return NO_MATCH;
    }
    if (tree.getKind() == Kind.PLUS) {
      handleStringifiedTree(tree.getLeftOperand(), ToStringKind.IMPLICIT, state);
      handleStringifiedTree(tree.getRightOperand(), ToStringKind.IMPLICIT, state);
    }
    if (tree.getKind() == Kind.PLUS_ASSIGNMENT) {
      handleStringifiedTree(tree.getRightOperand(), ToStringKind.IMPLICIT, state);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    if (state.getTypes().isSameType(getType(tree.getVariable()), state.getSymtab().stringType)
        && tree.getKind() == Kind.PLUS_ASSIGNMENT) {
      handleStringifiedTree(tree.getExpression(), ToStringKind.IMPLICIT, state);
    }
    return NO_MATCH;
  }

  private void handleStringifiedTree(
      ExpressionTree tree, ToStringKind toStringKind, VisitorState state) {
    handleStringifiedTree(tree, tree, toStringKind, state);
  }

  private void handleStringifiedTree(
      Tree parent, ExpressionTree tree, ToStringKind toStringKind, VisitorState state) {
    Type type = type(tree);
    if (type.getKind() == TypeKind.NULL
        || !typePredicate().apply(type, state)
        || allowableToStringKind(toStringKind)) {
      return;
    }
    state.reportMatch(maybeFix(tree, state, type, getFix(tree, state, parent, toStringKind)));
  }

  private static Type type(ExpressionTree tree) {
    Type type = getType(tree);
    if (type instanceof MethodType) {
      return type.getReturnType();
    }
    return type;
  }

  private Optional<Fix> getFix(
      ExpressionTree tree, VisitorState state, Tree parent, ToStringKind toStringKind) {
    switch (toStringKind) {
      case IMPLICIT:
      case FLOGGER:
      case FORMAT_METHOD:
        return implicitToStringFix(tree, state);
      case EXPLICIT:
        return toStringFix(parent, tree, state);
      case NONE:
        // fall out
    }
    throw new AssertionError();
  }

  private Description maybeFix(Tree tree, VisitorState state, Type matchedType, Optional<Fix> fix) {
    Description.Builder description = buildDescription(tree);
    fix.ifPresent(description::addFix);
    descriptionMessageForDefaultMatch(matchedType, state).ifPresent(description::setMessage);
    return description.build();
  }

  enum ToStringKind {
    /** String concatenation, or an enclosing print method. */
    IMPLICIT,
    /** {@code String.valueOf()} or {@code #toString()}. */
    EXPLICIT,
    FORMAT_METHOD,
    FLOGGER,
    NONE,
  }
}
