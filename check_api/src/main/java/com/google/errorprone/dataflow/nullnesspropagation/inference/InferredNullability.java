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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.graph.Graph;
import com.google.common.graph.ImmutableGraph;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.Map;
import java.util.Optional;

/**
 * Results of running {@code NullnessQualifierInference} over a method. The {@code constraintGraph}
 * represents qualifier constraints as a directed graph, where graph reachability encodes a
 * less-than-or-equal-to relationship.
 */
public class InferredNullability {
  private final ImmutableGraph<InferenceVariable> constraintGraph;

  private final Map<InferenceVariable, Optional<Nullness>> lowerBoundMemoTable = Maps.newHashMap();
  private final Map<InferenceVariable, Optional<Nullness>> upperBoundMemoTable = Maps.newHashMap();

  InferredNullability(Graph<InferenceVariable> constraints) {
    this.constraintGraph = ImmutableGraph.copyOf(constraints);
  }

  /**
   * Get inferred nullness qualifiers for method-generic type variables at a callsite. When
   * inference is not possible for a given type variable, that type variable is not included in the
   * resulting map.
   */
  public ImmutableMap<TypeVariableSymbol, Nullness> getNullnessGenerics(
      MethodInvocationTree callsite) {
    ImmutableMap.Builder<TypeVariableSymbol, Nullness> result = ImmutableMap.builder();
    for (TypeVariableSymbol tvs :
        TreeInfo.symbol((JCTree) callsite.getMethodSelect()).getTypeParameters()) {
      InferenceVariable iv = TypeVariableInferenceVar.create(tvs, callsite);
      getUpperBound(iv).ifPresent(nullness -> result.put(tvs, nullness));
    }
    return result.build();
  }

  /** Get inferred nullness qualifier for an expression, if possible. */
  public Optional<Nullness> getExprNullness(ExpressionTree exprTree) {
    InferenceVariable iv = TypeArgInferenceVar.create(ImmutableList.of(), exprTree);
    return constraintGraph.nodes().contains(iv) ? getUpperBound(iv) : Optional.empty();
  }

  private Optional<Nullness> getLowerBound(InferenceVariable iv) {
    Optional<Nullness> result;
    // short-circuit and return if...
    // ...this inference variable is a `proper` bound, i.e. a concrete nullness lattice element
    if (iv instanceof ProperInferenceVar) {
      return Optional.of(((ProperInferenceVar) iv).nullness());
      // ...we've already computed and memoized a nullness value for it.
    } else if ((result = lowerBoundMemoTable.get(iv)) != null) {
      return result;
    } else {
      // In case of cycles in constraint graph, ensures base case to recursion
      lowerBoundMemoTable.put(iv, Optional.empty());

      result =
          constraintGraph.predecessors(iv).stream()
              .map(this::getLowerBound)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .reduce(Nullness::leastUpperBound);
      lowerBoundMemoTable.put(iv, result);
      return result;
    }
  }

  private Optional<Nullness> getUpperBound(InferenceVariable iv) {
    // short-circuit and return if...
    // ...this inference variable is a `proper` bound, i.e. a concrete nullness lattice element
    if (iv instanceof ProperInferenceVar) {
      return Optional.of(((ProperInferenceVar) iv).nullness());
      // ...we've already computed and memoized a nullness value for it.
    }
    Optional<Nullness> result = upperBoundMemoTable.get(iv);
    if (result != null) {
      return result;
    }

    // In case of cycles in constraint graph, ensures base case to recursion
    upperBoundMemoTable.put(iv, Optional.empty());

    result =
        constraintGraph.successors(iv).stream()
            .map(this::getUpperBound)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Nullness::greatestLowerBound);

    // A lower bound of NULLABLE implies an upper bound of NULLABLE, since NULLABLE is top
    if (!result.isPresent() && getLowerBound(iv).equals(Optional.of(Nullness.NULLABLE))) {
      result = Optional.of(Nullness.NULLABLE);
    }

    upperBoundMemoTable.put(iv, result);
    return result;
  }
}
