/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import blogspot.software_and_algorithms.stern_library.optimization.HungarianAlgorithm;
import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;

/**
 * Accumulates the various costs of using existing arguments or alternatives. These are modelled as
 * edge weights in a bipartite graph mapping parameters on to potential arguments. The {@link
 * #computeAssignments()} function can then be used to find the optimal assignment of parameters to
 * arguments.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
class Costs {

  /** Formal parameters for the method being called. */
  private final ImmutableList<Parameter> formals;

  /** Actual parameters (argments) for the method call. */
  private final ImmutableList<Parameter> actuals;

  /**
   * The cost matrix of distances: Element (i,j) is the distance between the ith formal parameter
   * and the name of the jth actual parameter. The next stages assign costs to the elements of this
   * matrix using Infinity to indicate that this alternative should not be considered. We will refer
   * to combinations of formal parameter and (potential) actual parameter as pairs.
   */
  private final double[][] costMatrix;

  Costs(ImmutableList<Parameter> formals, ImmutableList<Parameter> actuals) {
    this.formals = formals;
    this.actuals = actuals;
    this.costMatrix = new double[formals.size()][actuals.size()];
  }

  Changes computeAssignments() {
    int[] assignments = new HungarianAlgorithm(costMatrix).execute();
    ImmutableList<Parameter> formalsWithChange =
        formals.stream()
            .filter(f -> assignments[f.index()] != f.index())
            .collect(toImmutableList());

    if (formalsWithChange.isEmpty()) {
      return Changes.empty();
    }

    ImmutableList<Double> originalCost =
        formalsWithChange.stream()
            .map(f2 -> costMatrix[f2.index()][f2.index()])
            .collect(toImmutableList());

    ImmutableList<Double> assignmentCost =
        formalsWithChange.stream()
            .map(f1 -> costMatrix[f1.index()][assignments[f1.index()]])
            .collect(toImmutableList());

    ImmutableList<ParameterPair> changes =
        formalsWithChange.stream()
            .map(f -> ParameterPair.create(f, actuals.get(assignments[f.index()])))
            .collect(toImmutableList());

    return Changes.create(originalCost, assignmentCost, changes);
  }

  /**
   * Constructs a stream for every element of formals paired with every element of actuals (cross
   * product). Each item contains the formal and the actual, which in turn contain their index into
   * the cost matrix. Elements whose cost is Inf in the cost matrix are filtered out so only viable
   * pairings remain.
   */
  Stream<ParameterPair> viablePairs() {
    return formals.stream()
        .flatMap(f -> actuals.stream().map(a -> ParameterPair.create(f, a)))
        .filter(
            p -> costMatrix[p.formal().index()][p.actual().index()] != Double.POSITIVE_INFINITY);
  }

  /** Set the cost of all the alternatives for this formal parameter to be Inf. */
  void invalidateAllAlternatives(Parameter formal) {
    for (int actualIndex = 0; actualIndex < costMatrix[formal.index()].length; actualIndex++) {
      if (actualIndex != formal.index()) {
        costMatrix[formal.index()][actualIndex] = Double.POSITIVE_INFINITY;
      }
    }
  }

  /** Update the cost of the given pairing. */
  void updatePair(ParameterPair p, double cost) {
    costMatrix[p.formal().index()][p.actual().index()] = cost;
  }

  /** Set the cost of this pairing to be Inf. */
  void invalidatePair(ParameterPair p) {
    updatePair(p, Double.POSITIVE_INFINITY);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Costs:\n");
    builder.append("formals=").append(formals).append("\n");
    builder.append("actuals=").append(actuals).append("\n");
    builder.append("costMatrix=\n");
    builder.append(String.format("%20s", ""));
    for (int j = 0; j < costMatrix[0].length; j++) {
      builder.append(String.format("%20s", actuals.get(j).name()));
    }
    builder.append("\n");
    for (int i = 0; i < costMatrix.length; i++) {
      builder.append(String.format("%20s", formals.get(i).name()));
      for (int j = 0; j < costMatrix[i].length; j++) {
        builder.append(String.format("%20.1f", costMatrix[i][j]));
      }
      builder.append("\n");
    }
    return builder.toString();
  }
}
