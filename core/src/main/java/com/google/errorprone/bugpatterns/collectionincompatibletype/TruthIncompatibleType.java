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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.extractTypeArgAsMemberOfSupertype;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCastable;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils.TypeCompatibilityReport;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.stream.Stream;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "TruthIncompatibleType",
    summary = "Argument is not compatible with the subject's type.",
    severity = WARNING)
public class TruthIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> START_OF_ASSERTION =
      anyOf(
          staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> IS_EQUAL_TO =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isEqualTo", "isNotEqualTo");

  private static final Matcher<ExpressionTree> SCALAR_CONTAINS =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.IterableSubject")
          .namedAnyOf(
              "contains", "containsExactly", "doesNotContain", "containsAnyOf", "containsNoneOf");

  private static final Matcher<ExpressionTree> VECTOR_CONTAINS =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.IterableSubject")
          .namedAnyOf(
              "containsExactlyElementsIn",
              "containsAnyIn",
              "containsAtLeastElementsIn",
              "containsNoneIn")
          .withParameters("java.lang.Iterable");

  private static final Matcher<ExpressionTree> COMPARING_ELEMENTS_USING =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.IterableSubject")
          .named("comparingElementsUsing");

  private static final Matcher<ExpressionTree> ARRAY_CONTAINS =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.IterableSubject")
              .namedAnyOf(
                  "containsExactlyElementsIn",
                  "containsAnyIn",
                  "containsAtLeastElementsIn",
                  "containsNoneIn"),
          not(VECTOR_CONTAINS));

  private static final Supplier<Type> CORRESPONDENCE =
      typeFromString("com.google.common.truth.Correspondence");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Streams.concat(
            matchEquality(tree, state),
            matchVectorContains(tree, state),
            matchCorrespondence(tree, state),
            matchArrayContains(tree, state),
            matchScalarContains(tree, state))
        .forEach(state::reportMatch);
    return NO_MATCH;
  }

  private Stream<Description> matchEquality(MethodInvocationTree tree, VisitorState state) {
    if (!IS_EQUAL_TO.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getType(ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments())));
    Type sourceType = getType(getOnlyElement(tree.getArguments()));
    if (isNumericType(sourceType, state) && isNumericType(targetType, state)) {
      return Stream.of();
    }
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchVectorContains(MethodInvocationTree tree, VisitorState state) {
    if (!VECTOR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments())), state);
    Type sourceType = getIterableTypeArg(getOnlyElement(tree.getArguments()), state);
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchCorrespondence(MethodInvocationTree tree, VisitorState state) {
    if (!COMPARING_ELEMENTS_USING.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments())), state);
    ExpressionTree argument = getOnlyElement(tree.getArguments());
    Type sourceType = getCorrespondenceTypeArg(argument, state);
    // This is different to the others: we're checking for castability, not possible equality.
    if (isCastable(targetType, sourceType, state)) {
      return Stream.empty();
    }
    String sourceTypeName = Signatures.prettyType(sourceType);
    String targetTypeName = Signatures.prettyType(targetType);
    if (sourceTypeName.equals(targetTypeName)) {
      sourceTypeName = sourceType.toString();
      targetTypeName = targetType.toString();
    }

    return Stream.of(
        buildDescription(argument)
            .setMessage(
                String.format(
                    "Argument '%s' should not be passed to this method: its type `%s` is"
                        + " not compatible with `%s`",
                    state.getSourceForNode(argument), sourceTypeName, targetTypeName))
            .build());
  }

  private Stream<Description> matchArrayContains(MethodInvocationTree tree, VisitorState state) {
    if (!ARRAY_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments())), state);
    Type sourceType =
        ((ArrayType) getType(ignoringCasts(getOnlyElement(tree.getArguments())))).elemtype;
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchScalarContains(MethodInvocationTree tree, VisitorState state) {
    if (!SCALAR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Tree argument = ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()));
    Type targetType = getIterableTypeArg(argument, state);
    MethodSymbol methodSymbol = getSymbol(tree);
    return Streams.mapWithIndex(
            tree.getArguments().stream(),
            (arg, index) -> {
              Type argumentType = getType(arg);
              return isNonVarargsCall(methodSymbol, index, argumentType)
                  ? checkCompatibility(arg, targetType, ((ArrayType) argumentType).elemtype, state)
                  : checkCompatibility(arg, targetType, argumentType, state);
            })
        .flatMap(x -> x);
  }

  /** Whether this is a varargs method being invoked in a non-varargs way. */
  private static boolean isNonVarargsCall(
      MethodSymbol methodSymbol, long index, Type argumentType) {
    return methodSymbol.getParameters().size() - 1 == index
        && methodSymbol.isVarArgs()
        && argumentType instanceof ArrayType
        && !((ArrayType) argumentType).elemtype.isPrimitive();
  }

  private Stream<Description> checkCompatibility(
      ExpressionTree tree, Type targetType, Type sourceType, VisitorState state) {
    TypeCompatibilityReport compatibilityReport =
        TypeCompatibilityUtils.compatibilityOfTypes(targetType, sourceType, state);
    if (compatibilityReport.compatible()) {
      return Stream.empty();
    }
    String sourceTypeName = Signatures.prettyType(sourceType);
    String targetTypeName = Signatures.prettyType(targetType);
    if (sourceTypeName.equals(targetTypeName)) {
      sourceTypeName = sourceType.toString();
      targetTypeName = targetType.toString();
    }

    return Stream.of(
        buildDescription(tree)
            .setMessage(
                String.format(
                    "Argument '%s' should not be passed to this method: its type `%s` is"
                        + " not compatible with `%s`",
                    state.getSourceForNode(tree), sourceTypeName, targetTypeName))
            .build());
  }

  private Tree ignoringCasts(Tree tree) {
    return tree.accept(
        new SimpleTreeVisitor<Tree, Void>() {
          @Override
          protected Tree defaultAction(Tree node, Void unused) {
            return node;
          }

          @Override
          public Tree visitTypeCast(TypeCastTree node, Void unused) {
            return node.getExpression().accept(this, null);
          }

          @Override
          public Tree visitParenthesized(ParenthesizedTree node, Void unused) {
            return node.getExpression().accept(this, null);
          }
        },
        null);
  }

  private static Type getIterableTypeArg(Tree onlyElement, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(onlyElement),
        state.getSymtab().iterableType.tsym,
        /* typeArgIndex= */ 0,
        state.getTypes());
  }

  private static Type getCorrespondenceTypeArg(Tree onlyElement, VisitorState state) {
    return extractTypeArgAsMemberOfSupertype(
        getType(onlyElement),
        CORRESPONDENCE.get(state).tsym,
        /* typeArgIndex= */ 0,
        state.getTypes());
  }

  private static boolean isNumericType(Type parameter, VisitorState state) {
    return parameter.isNumeric()
        || isSubtype(parameter, state.getTypeFromString("java.lang.Number"), state);
  }
}
