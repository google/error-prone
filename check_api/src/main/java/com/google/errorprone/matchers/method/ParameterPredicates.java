/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.matchers.method;

import com.google.errorprone.predicates.TypePredicate;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.List;

/** Utility methods for creating {@link ParameterPredicate} instances. */
public final class ParameterPredicates {

  public static ParameterPredicate of(TypePredicate predicate) {
    return (parameter, type, state) -> predicate.apply(type, state);
  }

  public static ParameterPredicate varargsOf(ParameterPredicate predicate) {
    return (parameter, type, state) -> {
      MethodSymbol method = (MethodSymbol) parameter.owner;
      if (!method.isVarArgs()) {
        return false;
      }
      if (method.getParameters().getLast() != parameter) {
        return false;
      }
      Type componentType = state.getTypes().elemtype(type);
      return predicate.matches(parameter, componentType, state);
    };
  }

  public static ParameterPredicate arrayOf(ParameterPredicate predicate) {
    return (parameter, type, state) -> {
      if (!type.hasTag(TypeTag.ARRAY)) {
        return false;
      }
      Type componentType = state.getTypes().elemtype(type);
      return predicate.matches(parameter, componentType, state);
    };
  }

  // TODO: cushon - add methods like nthTypeParameter(2) or typeParameterNamed("X") as needed
  public static ParameterPredicate onlyTypeParameter() {
    return (parameter, type, state) -> {
      MethodSymbol method = (MethodSymbol) parameter.owner;
      List<TypeVariableSymbol> typeParameters = method.getTypeParameters();
      return !typeParameters.isEmpty()
          && type.hasTag(TypeTag.TYPEVAR)
          && type.tsym == typeParameters.getFirst();
    };
  }

  private ParameterPredicates() {}
}
