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
package com.google.errorprone.names;

import blogspot.software_and_algorithms.stern_library.optimization.HungarianAlgorithm;
import com.google.common.collect.ImmutableList;
import java.util.function.BiFunction;
import java.util.stream.DoubleStream;

/**
 * A utility class for finding the distance between two identifiers. Each identifier is split into
 * its constituent terms (based on camel case or underscore naming conventions). Then the edit
 * distance between each term is computed and the minimum cost assignment is found.
 */
public class TermEditDistance {

  private final BiFunction<String, String, Double> editDistanceFn;
  private final BiFunction<Integer, Integer, Double> maxDistanceFn;

  /**
   * Creates a TermEditDistance Object
   *
   * @param editDistanceFn function to compute the distance between two terms
   * @param maxDistanceFn function to compute the worst case distance between two terms
   */
  public TermEditDistance(
      BiFunction<String, String, Double> editDistanceFn,
      BiFunction<Integer, Integer, Double> maxDistanceFn) {
    this.editDistanceFn = editDistanceFn;
    this.maxDistanceFn = maxDistanceFn;
  }

  public TermEditDistance() {
    this(
        (s, t) ->
            (double) LevenshteinEditDistance.getEditDistance(s, t, /* caseSensitive= */ false),
        (s, t) -> (double) LevenshteinEditDistance.getWorstCaseEditDistance(s, t));
  }

  public double getNormalizedEditDistance(String source, String target) {

    ImmutableList<String> sourceTerms = NamingConventions.splitToLowercaseTerms(source);
    ImmutableList<String> targetTerms = NamingConventions.splitToLowercaseTerms(target);

    // costMatrix[s][t] is the edit distance between source term s and target term t
    double[][] costMatrix =
        sourceTerms.stream()
            .map(s -> targetTerms.stream().mapToDouble(t -> editDistanceFn.apply(s, t)).toArray())
            .toArray(double[][]::new);

    // worstCaseMatrix[s][t] is the worst case distance between source term s and target term t
    double[][] worstCaseMatrix =
        sourceTerms.stream()
            .map(s -> s.length())
            .map(
                s ->
                    targetTerms.stream()
                        .map(t -> t.length())
                        .mapToDouble(t -> maxDistanceFn.apply(s, t))
                        .toArray())
            .toArray(double[][]::new);

    double[] sourceTermDeletionCosts =
        sourceTerms.stream().mapToDouble(s -> maxDistanceFn.apply(s.length(), 0)).toArray();

    double[] targetTermAdditionCosts =
        targetTerms.stream().mapToDouble(s -> maxDistanceFn.apply(0, s.length())).toArray();

    // this is an array of assignments of source terms to target terms. If assignments[i] contains
    // the value j this means that source term i has been assigned to target term j
    // There will be one entry in cost for each source term:
    // - If there are more source terms than target terms then some will be unassigned - value -1
    // - If there are a fewer source terms than target terms then some target terms will not be
    //    referenced in the array
    int[] assignments = new HungarianAlgorithm(costMatrix).execute();
    double assignmentCost =
        computeCost(assignments, costMatrix, sourceTermDeletionCosts, targetTermAdditionCosts);

    double maxCost =
        computeCost(assignments, worstCaseMatrix, sourceTermDeletionCosts, targetTermAdditionCosts);

    return assignmentCost / maxCost;
  }

  /**
   * Compute the total cost of this assignment including the costs of unassigned source and target
   * terms.
   */
  private static double computeCost(
      int[] assignments,
      double[][] costMatrix,
      double[] sourceTermDeletionCosts,
      double[] targetTermDeletionCosts) {

    // We need to sum the costs of each assigned pair, each unassigned source term, and each
    // unassigned target term.

    // Start with the total cost of _not_ using all the target terms, then when we use one we'll
    // remove it from this total.
    double totalCost = DoubleStream.of(targetTermDeletionCosts).sum();
    for (int sourceTermIndex = 0; sourceTermIndex < assignments.length; sourceTermIndex++) {
      int targetTermIndex = assignments[sourceTermIndex];
      if (targetTermIndex == -1) {
        // not using this source term
        totalCost += sourceTermDeletionCosts[sourceTermIndex];
      } else {
        // add the cost of the assignments
        totalCost += costMatrix[sourceTermIndex][targetTermIndex];

        // we are using this target term and so we should remove the cost of deleting it
        totalCost -= targetTermDeletionCosts[targetTermIndex];
      }
    }
    return totalCost;
  }
}
