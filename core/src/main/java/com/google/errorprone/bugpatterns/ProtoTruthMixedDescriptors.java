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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;

/**
 * Checks that {@code ProtoTruth}'s {@code ignoringFields} is passed field numbers from the correct
 * proto.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "ProtoTruthMixedDescriptors",
    summary =
        "The arguments passed to `ignoringFields` are inconsistent with the proto which is "
            + "the subject of the assertion.",
    severity = ERROR,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class ProtoTruthMixedDescriptors extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> IGNORING =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.extensions.proto.ProtoFluentAssertion")
          .named("ignoringFields");

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      staticMethod()
          .onClass("com.google.common.truth.extensions.proto.ProtoTruth")
          .named("assertThat");

  private static final Supplier<Type> MESSAGE =
      Suppliers.typeFromString("com.google.protobuf.Message");
  private static final Supplier<Type> GENERATED_MESSAGE =
      Suppliers.typeFromString("com.google.protobuf.GeneratedMessage");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!IGNORING.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    ImmutableSet<TypeSymbol> types =
        arguments.stream()
            .map(t -> protoType(t, state))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toImmutableSet());
    if (types.size() > 1) {
      return describeMatch(tree);
    }
    if (types.size() != 1) {
      return Description.NO_MATCH;
    }
    TypeSymbol type = getOnlyElement(types);
    for (ExpressionTree receiver = getReceiver(tree);
        receiver instanceof MethodInvocationTree;
        receiver = getReceiver(receiver)) {
      if (ASSERT_THAT.matches(receiver, state)) {
        return validateReceiver(tree, (MethodInvocationTree) receiver, type, state);
      }
    }
    return Description.NO_MATCH;
  }

  // Tries to resolve the proto which owns the symbol at `tree`, or absent if there isn't one.
  private static Optional<TypeSymbol> protoType(ExpressionTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol != null
        && symbol.owner != null
        && isSubtype(symbol.owner.type, MESSAGE.get(state), state)) {
      return Optional.of(symbol.owner.type.tsym);
    }
    return Optional.empty();
  }

  // Given an `assertThat()` call (`receiver`), checks that the proto matches the type `type` if it
  // can be resolved.
  private Description validateReceiver(
      MethodInvocationTree tree,
      MethodInvocationTree receiver,
      TypeSymbol type,
      VisitorState state) {
    if (receiver.getArguments().size() != 1) {
      return Description.NO_MATCH;
    }
    ExpressionTree argument = getOnlyElement(receiver.getArguments());
    Type subjectType = getType(argument);
    if (isSubtype(subjectType, state.getSymtab().iterableType, state)) {
      if (subjectType.getTypeArguments().isEmpty()) {
        return Description.NO_MATCH;
      }
      subjectType = getOnlyElement(subjectType.getTypeArguments());
    }

    return subjectType == null
            || subjectType.tsym.equals(type)
            || !isSubtype(subjectType, GENERATED_MESSAGE.get(state), state)
        ? Description.NO_MATCH
        : describeMatch(tree);
  }
}
