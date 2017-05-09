/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.function.Function;

/**
 * Instances of this class are used to find whether there should be argument swaps on a method
 * invocation. It's general operation is through using the provided distance function to compute the
 * similarity of a parameter name and an argument name. A change is a permutation of the original
 * arguments which has the lowest total distance. This is computed as the minimum cost distance
 * match on the bi-partite graph mapping parameters to assignable arguments.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@AutoValue
abstract class ArgumentChangeFinder {

  /**
   * The distance function to use when comparing formal and actual parameters. The function should
   * return 0 for highly similar names and larger positive values as names are more different.
   */
  abstract Function<ParameterPair, Double> distanceFunction();

  /** List of heuristics to apply to eliminate spurious suggestions. */
  abstract ImmutableList<Heuristic> heuristics();

  static Builder builder() {
    return new AutoValue_ArgumentChangeFinder.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    /** Set the distance function that {@code ArgumentChangeFinder} should use. */
    abstract Builder setDistanceFunction(Function<ParameterPair, Double> distanceFunction);

    abstract ImmutableList.Builder<Heuristic> heuristicsBuilder();

    /**
     * Add the given heuristic to the list to be considered by {@code ArugmentChangeFinder} for
     * eliminating spurious findings. Heuristics are applied in order so add more expensive checks
     * last.
     */
    Builder addHeuristic(Heuristic heuristic) {
      heuristicsBuilder().add(heuristic);
      return this;
    }

    abstract ArgumentChangeFinder build();
  }

  /**
   * Find the optimal permutation of arguments for this method invocation or return {@code
   * Changes.empty()} if no good permutations were found.
   */
  Changes findChanges(InvocationInfo invocationInfo) {
    /* Methods with one or fewer parameters cannot possibly have a swap */
    if (invocationInfo.formalParameters().size() <= 1) {
      return Changes.empty();
    }

    /* Sometimes we don't have enough actual parameters. This seems to happen sometimes with calls
     * to super and javac finds two parameters arg0 and arg1 and no arguments */
    if (invocationInfo.actualParameters().size() < invocationInfo.formalParameters().size()) {
      return Changes.empty();
    }

    ImmutableList<Parameter> formals =
        Parameter.createListFromVarSymbols(invocationInfo.formalParameters());
    ImmutableList<Parameter> actuals =
        Parameter.createListFromExpressionTrees(
            invocationInfo.actualParameters().subList(0, invocationInfo.formalParameters().size()));

    Costs costs = new Costs(formals, actuals);

    /* Set the distance between a pair to Inf if not assignable */
    costs
        .viablePairs()
        .filter(ParameterPair::isAlternativePairing)
        .filter(p -> !p.actual().isAssignableTo(p.formal(), invocationInfo.state()))
        .forEach(p -> costs.invalidatePair(p));

    /* If there are no formal parameters which are assignable to any alternative actual parameters
    then we can stop without trying to look for permutations */
    if (costs.viablePairs().noneMatch(ParameterPair::isAlternativePairing)) {
      return Changes.empty();
    }

    /* Set the lexical distance between pairs */
    costs.viablePairs().forEach(p -> costs.updatePair(p, distanceFunction().apply(p)));

    Changes changes = costs.computeAssignments();

    if (changes.isEmpty()) {
      return changes;
    }

    /* Only keep this change if all of the heuristics match */
    for (Heuristic heuristic : heuristics()) {
      if (!heuristic.isAcceptableChange(
          changes, invocationInfo.tree(), invocationInfo.symbol(), invocationInfo.state())) {
        return Changes.empty();
      }
    }
    return changes;
  }
}
