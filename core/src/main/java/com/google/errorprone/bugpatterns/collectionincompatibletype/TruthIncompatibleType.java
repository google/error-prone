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
import static com.google.errorprone.bugpatterns.collectionincompatibletype.IgnoringCasts.ignoringCasts;
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
import static java.util.stream.Stream.concat;

import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibility;
import com.google.errorprone.bugpatterns.TypeCompatibility.TypeCompatibilityReport;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.stream.Stream;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Argument is not compatible with the subject's type.", severity = WARNING)
public class TruthIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> START_OF_ASSERTION =
      anyOf(
          staticMethod()
              .onClassAny("com.google.common.truth.Truth", "com.google.common.truth.Truth8")
              .named("assertThat"),
          staticMethod()
              .onClass("com.google.common.truth.extensions.proto.ProtoTruth")
              .named("assertThat"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> IS_EQUAL_TO =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .namedAnyOf("isEqualTo", "isNotEqualTo"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.extensions.proto.ProtoFluentAssertion")
              .namedAnyOf("isEqualTo", "isNotEqualTo"));

  private static final Matcher<ExpressionTree> FLUENT_PROTO_CHAIN =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.extensions.proto.ProtoFluentAssertion"),
          instanceMethod().onDescendantOf("com.google.common.truth.extensions.proto.ProtoSubject"));

  private static final Matcher<ExpressionTree> SCALAR_CONTAINS =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.IterableSubject", "com.google.common.truth.StreamSubject")
          .namedAnyOf(
              "contains",
              "containsExactly",
              "doesNotContain",
              "containsAnyOf",
              "containsNoneOf",
              "containsAtLeast");

  private static final Matcher<ExpressionTree> IS_ANY_OF =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isAnyOf", "isNoneOf");

  private static final Matcher<ExpressionTree> IS_IN =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isIn", "isNotIn");

  private static final Matcher<ExpressionTree> VECTOR_CONTAINS =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.IterableSubject", "com.google.common.truth.StreamSubject")
          .namedAnyOf(
              "containsExactlyElementsIn",
              "containsAnyIn",
              "containsAtLeastElementsIn",
              "containsNoneIn")
          .withParameters("java.lang.Iterable");

  private static final Matcher<ExpressionTree> MAP_SCALAR_CONTAINS =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.MapSubject", "com.google.common.truth.MultimapSubject")
          .namedAnyOf("containsEntry", "doesNotContainEntry", "containsExactly", "containsAtLeast");

  private static final Matcher<ExpressionTree> MAP_SCALAR_KEYS =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.MapSubject", "com.google.common.truth.MultimapSubject")
          .namedAnyOf("containsKey", "doesNotContainKey");

  private static final Matcher<ExpressionTree> HAS_COUNT =
      instanceMethod()
          .onDescendantOfAny("com.google.common.truth.MultisetSubject")
          .named("hasCount");

  private static final Matcher<ExpressionTree> MAP_VECTOR_CONTAINS =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.MapSubject", "com.google.common.truth.MultimapSubject")
          .namedAnyOf("containsExactlyEntriesIn", "containsAtLeastEntriesIn");

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

  private final TypeCompatibility typeCompatibility;

  @Inject
  TruthIncompatibleType(TypeCompatibility typeCompatibility) {
    this.typeCompatibility = typeCompatibility;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    boolean useCapture = typeCompatibility.useCapture();
    Streams.concat(
            matchEquality(tree, state),
            matchIsAnyOf(tree, state),
            matchIsIn(tree, state, useCapture),
            matchVectorContains(tree, state, useCapture),
            matchArrayContains(tree, state, useCapture),
            matchScalarContains(tree, state, useCapture),
            matchCorrespondence(tree, state, useCapture),
            matchMapVectorContains(tree, state, useCapture),
            matchMapScalarContains(tree, state, useCapture),
            matchMapContainsKey(tree, state, useCapture),
            matchHasCount(tree, state, useCapture))
        .forEach(state::reportMatch);
    return NO_MATCH;
  }

  private Stream<Description> matchEquality(MethodInvocationTree tree, VisitorState state) {
    if (!IS_EQUAL_TO.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    while (true) {
      if (!(receiver instanceof MethodInvocationTree)) {
        return Stream.empty();
      }
      if (START_OF_ASSERTION.matches(receiver, state)) {
        break;
      }
      // TODO(b/190357148): Handle fluent methods which change the expected target type.
      if (!FLUENT_PROTO_CHAIN.matches(receiver, state)) {
        return Stream.empty();
      }
      receiver = getReceiver(receiver);
    }

    Type targetType =
        ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state);
    Type sourceType = getType(getOnlyElement(tree.getArguments()));
    if (isNumericType(sourceType, state) && isNumericType(targetType, state)) {
      return Stream.of();
    }
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchIsAnyOf(MethodInvocationTree tree, VisitorState state) {
    if (!IS_ANY_OF.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }
    Type targetType =
        ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state);
    return matchScalarContains(tree, targetType, state);
  }

  private Stream<Description> matchIsIn(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!IS_IN.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state);
    Type sourceType =
        getIterableTypeArg(
            getType(getOnlyElement(tree.getArguments())),
            getOnlyElement(tree.getArguments()),
            state,
            useCapture);
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchVectorContains(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!VECTOR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type,
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state),
            state,
            useCapture);
    Type sourceType =
        getIterableTypeArg(
            getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type,
            getOnlyElement(tree.getArguments()),
            state,
            useCapture);
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchArrayContains(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!ARRAY_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type,
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state),
            state,
            useCapture);
    Type sourceType = ((ArrayType) getType(getOnlyElement(tree.getArguments()))).elemtype;
    return checkCompatibility(getOnlyElement(tree.getArguments()), targetType, sourceType, state);
  }

  private Stream<Description> matchScalarContains(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!SCALAR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }
    Type targetType =
        getIterableTypeArg(
            getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type,
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state),
            state,
            useCapture);
    return matchScalarContains(tree, targetType, state);
  }

  private Stream<Description> matchScalarContains(
      MethodInvocationTree tree, Type targetType, VisitorState state) {
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

  private Stream<Description> matchCorrespondence(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!COMPARING_ELEMENTS_USING.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    Type targetType =
        getIterableTypeArg(
            getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type,
            ignoringCasts(getOnlyElement(((MethodInvocationTree) receiver).getArguments()), state),
            state,
            useCapture);
    if (targetType == null) {
      // The target collection may be raw.
      return Stream.empty();
    }
    ExpressionTree argument = getOnlyElement(tree.getArguments());
    Type sourceType = getCorrespondenceTypeArg(argument, state, useCapture);
    // This is different to the others: we're checking for castability, not possible equality.
    if (sourceType == null || isCastable(targetType, sourceType, state)) {
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

  private Stream<Description> matchMapVectorContains(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!MAP_VECTOR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    ExpressionTree assertee = getOnlyElement(((MethodInvocationTree) receiver).getArguments());
    TypeSymbol assertionType =
        getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type.tsym;
    Type targetKeyType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 0,
            state.getTypes(),
            useCapture);
    Type targetValueType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 1,
            state.getTypes(),
            useCapture);
    Type sourceKeyType =
        extractTypeArgAsMemberOfSupertype(
            getType(getOnlyElement(tree.getArguments())),
            assertionType,
            /* typeArgIndex= */ 0,
            state.getTypes(),
            useCapture);
    Type sourceValueType =
        extractTypeArgAsMemberOfSupertype(
            getType(getOnlyElement(tree.getArguments())),
            assertionType,
            /* typeArgIndex= */ 1,
            state.getTypes(),
            useCapture);
    return concat(
        checkCompatibility(
            getOnlyElement(tree.getArguments()), targetKeyType, sourceKeyType, state),
        checkCompatibility(
            getOnlyElement(tree.getArguments()), targetValueType, sourceValueType, state));
  }

  private Stream<Description> matchMapContainsKey(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!MAP_SCALAR_KEYS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    ExpressionTree assertee = getOnlyElement(((MethodInvocationTree) receiver).getArguments());
    TypeSymbol assertionType =
        getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type.tsym;
    Type targetKeyType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 0,
            state.getTypes(),
            useCapture);
    return checkCompatibility(
        getOnlyElement(tree.getArguments()),
        targetKeyType,
        getType(getOnlyElement(tree.getArguments())),
        state);
  }

  private Stream<Description> matchMapScalarContains(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!MAP_SCALAR_CONTAINS.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    ExpressionTree assertee = getOnlyElement(((MethodInvocationTree) receiver).getArguments());
    TypeSymbol assertionType =
        getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type.tsym;
    Type targetKeyType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 0,
            state.getTypes(),
            useCapture);
    Type targetValueType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 1,
            state.getTypes(),
            useCapture);
    MethodSymbol methodSymbol = getSymbol(tree);
    return Streams.mapWithIndex(
            tree.getArguments().stream(),
            (arg, index) ->
                isNonVarargsCall(methodSymbol, index, getType(arg))
                    ? Stream.<Description>empty()
                    : checkCompatibility(
                        arg, index % 2 == 0 ? targetKeyType : targetValueType, getType(arg), state))
        .flatMap(x -> x);
  }

  private Stream<Description> matchHasCount(
      MethodInvocationTree tree, VisitorState state, boolean useCapture) {
    if (!HAS_COUNT.matches(tree, state)) {
      return Stream.empty();
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!START_OF_ASSERTION.matches(receiver, state)) {
      return Stream.empty();
    }

    ExpressionTree assertee = getOnlyElement(((MethodInvocationTree) receiver).getArguments());
    TypeSymbol assertionType =
        getOnlyElement(getSymbol((MethodInvocationTree) receiver).getParameters()).type.tsym;
    Type targetKeyType =
        extractTypeArgAsMemberOfSupertype(
            ignoringCasts(assertee, state),
            assertionType,
            /* typeArgIndex= */ 0,
            state.getTypes(),
            useCapture);
    var argument = tree.getArguments().getFirst();
    return checkCompatibility(argument, targetKeyType, getType(argument), state);
  }

  /** Whether this is a varargs method being invoked in a non-varargs way. */
  private static boolean isNonVarargsCall(
      MethodSymbol methodSymbol, long index, Type argumentType) {
    return methodSymbol.getParameters().size() - 1 == index
        && methodSymbol.isVarArgs()
        && argumentType instanceof ArrayType arrayType
        && !arrayType.elemtype.isPrimitive();
  }

  private Stream<Description> checkCompatibility(
      ExpressionTree tree, Type targetType, Type sourceType, VisitorState state) {
    TypeCompatibilityReport compatibilityReport =
        typeCompatibility.compatibilityOfTypes(targetType, sourceType, state);
    if (compatibilityReport.isCompatible()) {
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
                        + " not compatible with `%s`%s",
                    state.getSourceForNode(tree),
                    sourceTypeName,
                    targetTypeName,
                    compatibilityReport.extraReason()))
            .build());
  }

  private static Type getIterableTypeArg(
      Type type, Tree onlyElement, VisitorState state, boolean useCapture) {
    return extractTypeArgAsMemberOfSupertype(
        getType(onlyElement), type.tsym, /* typeArgIndex= */ 0, state.getTypes(), useCapture);
  }

  private static Type getIterableTypeArg(
      Type type, Type onlyElement, VisitorState state, boolean useCapture) {
    return extractTypeArgAsMemberOfSupertype(
        onlyElement, type.tsym, /* typeArgIndex= */ 0, state.getTypes(), useCapture);
  }

  private static Type getCorrespondenceTypeArg(
      Tree onlyElement, VisitorState state, boolean useCapture) {
    return extractTypeArgAsMemberOfSupertype(
        getType(onlyElement),
        CORRESPONDENCE.get(state).tsym,
        /* typeArgIndex= */ 0,
        state.getTypes(),
        useCapture);
  }

  private static boolean isNumericType(Type parameter, VisitorState state) {
    return parameter.isNumeric() || isSubtype(parameter, JAVA_LANG_NUMBER.get(state), state);
  }

  private static final Supplier<Type> JAVA_LANG_NUMBER =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.Number"));
}
