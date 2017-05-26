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

package com.google.errorprone.bugpatterns.overloading;

import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * A class used to represent ordering violations within a {@link MethodTree}.
 *
 * <p>It is a triplet of three things: an original {@link MethodTree}, List<{@link VariableTree}> of
 * expected parameters, and a List<{@link VariableTree}> of actual parameters. Violations are
 * reported by using {@link ParameterTrie#extendAndComputeViolation(MethodTree)}.
 *
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@AutoValue
abstract class ParameterOrderingViolation {

  public abstract MethodTree methodTree();

  public abstract ImmutableList<ParameterTree> actual();

  public abstract ImmutableList<ParameterTree> expected();

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setMethodTree(MethodTree methodTree);

    abstract Builder setActual(ImmutableList<ParameterTree> actual);

    abstract Builder setExpected(ImmutableList<ParameterTree> expected);

    abstract ParameterOrderingViolation autoBuild();

    public ParameterOrderingViolation build() {
      ParameterOrderingViolation orderingViolation = autoBuild();

      int actualParametersCount = orderingViolation.actual().size();
      int expectedParameterCount = orderingViolation.expected().size();
      Preconditions.checkState(actualParametersCount == expectedParameterCount);

      return orderingViolation;
    }
  }

  public static ParameterOrderingViolation.Builder builder() {
    return new AutoValue_ParameterOrderingViolation.Builder();
  }

  /**
   * Provides a violation description with suggested parameter ordering.
   *
   * <p>An automated {@link SuggestedFix} is not returned with the description because it is not
   * safe: reordering the parameters can break the build (if we are lucky) or - in case of
   * reordering parameters with the same types - break runtime behaviour of the program.
   */
  public String getDescription() {
    return "The parameters of this method are inconsistent with other overloaded versions."
        + " A consistent order would be: "
        + getSuggestedSignature();
  }

  private String getSuggestedSignature() {
    return String.format("%s(%s)", methodTree().getName(), getSuggestedParameters());
  }

  private String getSuggestedParameters() {
    return expected().stream().map(ParameterTree::toString).collect(joining(", "));
  }
}
