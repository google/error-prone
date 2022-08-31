/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOfAny;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.MethodRule;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.regex.Pattern;

/** Rules for methods on proto messages and builders. */
public final class ProtoRules {

  private ProtoRules() {}

  /**
   * Returns a rule that handles proto builders, making their fluent setter methods' results
   * ignorable.
   */
  public static ResultUseRule protoBuilders() {
    return new ProtoRule(
        isDescendantOfAny(ImmutableSet.of("com.google.protobuf.MessageLite.Builder")),
        "PROTO_BUILDER");
  }

  /**
   * Returns a rule that handles mutable protos, making their fluent setter methods' results
   * ignorable.
   */
  public static ResultUseRule mutableProtos() {
    return new ProtoRule(
        isDescendantOfAny(ImmutableSet.of("com.google.protobuf.AbstractMutableMessageLite")),
        "MUTABLE_PROTO");
  }

  // TODO(cgdecker): Move proto rules from IgnoredPureGetter and ReturnValueIgnored here

  /** Rules for methods on protos. */
  private static final class ProtoRule extends MethodRule {
    // Methods that start this way produce a modification to the proto, and either return this
    // or return the parameter given, for chaining purposes.
    private static final Pattern NAMED_MUTATOR_METHOD =
        Pattern.compile("(add|clear|insert|merge|remove|set|put).*");

    private final TypePredicate typePredicate;
    private final String id;

    ProtoRule(TypePredicate typePredicate, String id) {
      this.typePredicate = checkNotNull(typePredicate);
      this.id = checkNotNull(id);
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
      /*
       * TODO(cpovirk): Would it be faster to check the method name before checking the type
       * hierarchy, at least if we could do so without converting from Name to String and without
       * using regex matching?
       */
      if (isProtoSubtype(method.owner.type, state)) {
        String methodName = method.name.toString();
        if (NAMED_MUTATOR_METHOD.matcher(methodName).matches()) {
          return Optional.of(ResultUsePolicy.OPTIONAL);
        }
        if (isMutatingAccessorMethod(methodName) && isProtoSubtype(method.getReturnType(), state)) {
          return Optional.of(ResultUsePolicy.OPTIONAL);
        }
      }
      return Optional.empty();
    }

    private boolean isProtoSubtype(Type ownerType, VisitorState state) {
      return typePredicate.apply(ownerType, state);
    }

    private static boolean isMutatingAccessorMethod(String name) {
      // TODO(glorioso): Any other naming conventions to check?
      // TODO(glorioso): Maybe worth making this a regex instead? But think about performance
      if (name.startsWith("get")) {
        // fooBuilder.getBarBuilder() mutates the builder such that foo.hasBar() is now true.
        return (name.endsWith("Builder") && !name.endsWith("OrBuilder"))
            // mutableFoo.getMutableBar() mutates Foo so that mutableFoo.hasBar() is now true
            || name.startsWith("getMutable");
      }
      return false;
    }
  }
}
