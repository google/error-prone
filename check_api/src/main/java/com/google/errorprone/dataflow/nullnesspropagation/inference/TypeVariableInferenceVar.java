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

package com.google.errorprone.dataflow.nullnesspropagation.inference;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;

/**
 * Type Variable inference variables correspond to type application sites, where type-polymorphic
 * methods are instantiated with a particular concrete type.
 */
@AutoValue
abstract class TypeVariableInferenceVar implements InferenceVariable {
  static TypeVariableInferenceVar create(
      TypeVariableSymbol typeVar, MethodInvocationTree typeAppSite) {
    return create(typeVar, typeAppSite, ImmutableList.of());
  }

  static TypeVariableInferenceVar create(
      TypeVariableSymbol typeVar,
      MethodInvocationTree typeAppSite,
      ImmutableList<Integer> typeArgSelector) {
    return new AutoValue_TypeVariableInferenceVar(typeVar, typeAppSite, typeArgSelector);
  }

  public final TypeVariableInferenceVar withSelector(ImmutableList<Integer> newSelector) {
    return create(typeVar(), typeApplicationSite(), newSelector);
  }

  abstract TypeVariableSymbol typeVar();

  /** AST Node for a method invocation whose type is parameterized by the given type var. */
  abstract MethodInvocationTree typeApplicationSite();

  /**
   * An empty list selects the type variable itself, while non-empty lists select type variables
   * within the actual type the type variable was instantiated with at the application site, using
   * the format as described for {@link TypeArgInferenceVar}.
   *
   * <p>As a simple example, consider a method declared to return its only type variable, {@code T}.
   * For a given invocation of that method, let's say the type variable is instantiated as {@code
   * Map&lt;String, Integer&gt;}. Then, an empty selector here selects {@code T} itself, while a
   * selector [0] selects {@code String} and [1] selects {@code Integer}.
   */
  abstract ImmutableList<Integer> typeArgSelector();
}
