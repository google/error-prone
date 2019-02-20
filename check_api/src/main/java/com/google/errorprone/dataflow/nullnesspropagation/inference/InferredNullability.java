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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import com.google.common.graph.ImmutableGraph;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Results of running {@code NullnessQualifierInference} over a method. The {@code constraintGraph}
 * represents qualifier constraints as a directed graph, where graph reachability encodes a
 * less-than-or-equal-to relationship.
 */
public class InferredNullability {
  private final ImmutableGraph<InferenceVariable> constraintGraph;

  private final Map<InferenceVariable, Optional<Nullness>> inferredMemoTable = new HashMap<>();

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
      if (constraintGraph.nodes().contains(iv)) {
        getNullness(iv).ifPresent(nullness -> result.put(tvs, nullness));
      }
    }
    return result.build();
  }

  /** Get inferred nullness qualifier for an expression, if possible. */
  public Optional<Nullness> getExprNullness(ExpressionTree exprTree) {
    InferenceVariable iv = TypeArgInferenceVar.create(ImmutableList.of(), exprTree);
    return constraintGraph.nodes().contains(iv) ? getNullness(iv) : Optional.empty();
  }

  private Optional<Nullness> getNullness(InferenceVariable iv) {
    Optional<Nullness> result;
    // short-circuit and return if...
    // ...this inference variable is a `proper` bound, i.e. a concrete nullness lattice element
    if (iv instanceof ProperInferenceVar) {
      return Optional.of(((ProperInferenceVar) iv).nullness());
      // ...we've already computed and memoized a nullness value for it.
    } else if ((result = inferredMemoTable.get(iv)) != null) {
      return result;
    } else {
      // In case of cycles in constraint graph, ensures base case to recursion
      inferredMemoTable.put(iv, Optional.empty());

      // Resolution per JLS 18.4:
      // 1. resolve predecessors to see if there are lower bounds we can use
      result =
          constraintGraph.predecessors(iv).stream()
              .map(this::getNullness)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .reduce(Nullness::leastUpperBound); // use least upper bound (lub) to combine
      // 2. If not, resolve successors and use them as upper bounds
      if (!result.isPresent()) {
        result =
            constraintGraph.successors(iv).stream()
                .map(this::getNullness)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(Nullness::greatestLowerBound); // use greatest lower bound (glb) to combine
      }

      checkState(!inferredMemoTable.put(iv, result).isPresent());
      return result;
    }
  }
}
