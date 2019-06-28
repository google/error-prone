/*
 * Copyright 2019 The Error Prone Authors.
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
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** The state that is propagated across a match operation for constructors. */
@AutoValue
public abstract class ConstructorMatchState implements MatchState {
  /** The type of the class in which a member method or constructor is declared. */
  @Override
  public Type ownerType() {
    return sym().owner.type;
  }

  /** The method being matched. */
  @Override
  public abstract MethodSymbol sym();

  static MatchState create(MethodSymbol methodSymbol) {
    return new AutoValue_ConstructorMatchState(methodSymbol);
  }
}
