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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;

/**
 * Checks for calls to {@code Descriptor#findFieldByNumber} with field numbers from a different
 * proto.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "MixedDescriptors",
    summary =
        "The field number passed into #getFieldByNumber belongs to a different proto"
            + " to the Descriptor.",
    severity = ERROR)
public final class MixedDescriptors extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_DESCRIPTOR =
      staticMethod().onClass(isDescendantOf("com.google.protobuf.Message")).named("getDescriptor");

  private static final Matcher<ExpressionTree> FIND_FIELD =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.Descriptors.Descriptor")
          .named("findFieldByNumber");

  private static final Supplier<Type> MESSAGE =
      Suppliers.typeFromString("com.google.protobuf.Message");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!FIND_FIELD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree receiver = getReceiver(tree);
    if (!GET_DESCRIPTOR.matches(receiver, state)) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() != 1) {
      return Description.NO_MATCH;
    }
    Symbol argumentSymbol = getSymbol(getOnlyElement(arguments));
    if (!(argumentSymbol instanceof VarSymbol)
        || !argumentSymbol.getSimpleName().toString().endsWith("_FIELD_NUMBER")) {
      return Description.NO_MATCH;
    }
    Optional<TypeSymbol> descriptorType = protoType(getOnlyElement(arguments), state);
    Optional<TypeSymbol> receiverType = protoType(receiver, state);
    return typesDiffer(
            descriptorType.filter(MixedDescriptors::shouldConsider),
            receiverType.filter(MixedDescriptors::shouldConsider))
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  /** Ignore packages specifically qualified as proto1 or proto2. */
  private static boolean shouldConsider(TypeSymbol symbol) {
    String packge = symbol.packge().toString();
    return !(packge.contains(".proto1api") || packge.contains(".proto2api"));
  }

  private static boolean typesDiffer(Optional<TypeSymbol> a, Optional<TypeSymbol> b) {
    return a.isPresent() && b.isPresent() && !a.get().equals(b.get());
  }

  private static Optional<TypeSymbol> protoType(Tree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol != null
        && symbol.owner instanceof ClassSymbol
        && isSubtype(symbol.owner.type, MESSAGE.get(state), state)) {
      return Optional.of(symbol.owner.type.tsym);
    }
    return Optional.empty();
  }
}
