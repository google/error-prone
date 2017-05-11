/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** The state that is propagated across a match operation. */
@AutoValue
abstract class MatchState {
  /** The type of the class in which a member method or constructor is declared. */
  abstract Type ownerType();

  /** The method being matched. */
  abstract MethodSymbol sym();

  /** The method's formal parameter types. */
  abstract ImmutableList<Type> paramTypes();

  static MatchState create(Type ownerType, MethodSymbol methodSymbol) {
    return new AutoValue_MatchState(
        ownerType, methodSymbol, ImmutableList.copyOf(methodSymbol.type.getParameterTypes()));
  }
}
