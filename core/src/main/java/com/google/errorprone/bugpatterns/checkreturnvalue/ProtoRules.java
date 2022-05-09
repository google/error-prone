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

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.MethodRule;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
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
    return new ProtoBuilder();
  }

  private static final Supplier<Type> MESSAGE_LITE_BUILDER =
      supplier("com.google.protobuf.MessageLite.Builder");

  // TODO(cgdecker): Move proto rules from IgnoredPureGetter and ReturnValueIgnored here

  /** Rules for methods on proto builders. */
  private static final class ProtoBuilder extends MethodRule {
    private static final Pattern SETTERS = Pattern.compile("(add|clear|remove|set|put).+");

    @Override
    public String id() {
      return "PROTO_BUILDER";
    }

    @Override
    public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
      if (isProtoBuilderType(state, method.owner.type)) {
        String methodName = method.name.toString();
        if (SETTERS.matcher(methodName).matches()) {
          return Optional.of(ResultUsePolicy.OPTIONAL);
        }
        if (isGetterOfSubmessageBuilder(methodName)
            && isProtoBuilderType(state, method.getReturnType())) {
          return Optional.of(ResultUsePolicy.OPTIONAL);
        }
      }
      return Optional.empty();
    }

    private static boolean isProtoBuilderType(VisitorState state, Type ownerType) {
      return ASTHelpers.isSubtype(ownerType, MESSAGE_LITE_BUILDER.get(state), state);
    }

    // fooBuilder.getBarBuilder() mutates the builder such that foo.hasBar() is now true.
    private static boolean isGetterOfSubmessageBuilder(String name) {
      // TODO(glorioso): Any other naming conventions to check?
      return name.startsWith("get") && name.endsWith("Builder") && !name.endsWith("OrBuilder");
    }
  }

  private static Supplier<Type> supplier(String name) {
    return VisitorState.memoize(s -> s.getTypeFromString(name));
  }
}
