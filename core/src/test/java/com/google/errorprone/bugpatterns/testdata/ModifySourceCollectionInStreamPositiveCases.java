/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test cases for {@link com.google.errorprone.bugpatterns.ModifySourceCollectionInStream}.
 *
 * @author deltazulu@google.com (Donald Duo Zhao)
 */
public class ModifySourceCollectionInStreamPositiveCases {

  private final List<Integer> mutableValues = Arrays.asList(1, 2, 3);

  private void mutateStreamSourceMethodReference() {

    mutableValues.stream()
        .map(x -> x + 1)
        .filter(x -> x < 5)
        // BUG: Diagnostic contains:
        .forEach(mutableValues::remove);

    this.mutableValues.stream()
        .map(x -> x + 1)
        .filter(x -> x < 5)
        // BUG: Diagnostic contains:
        .forEach(mutableValues::remove);

    getMutableValues()
        .parallelStream()
        .map(x -> x + 1)
        .filter(x -> x < 5)
        // BUG: Diagnostic contains:
        .forEach(getMutableValues()::add);

    getMutableValues().stream()
        .map(x -> x + 1)
        .filter(x -> x < 5)
        // BUG: Diagnostic contains:
        .forEach(this.getMutableValues()::remove);

    ModifySourceCollectionInStreamPositiveCases[] cases = {
      new ModifySourceCollectionInStreamPositiveCases(),
      new ModifySourceCollectionInStreamPositiveCases()
    };
    cases[0].mutableValues.stream()
        .map(x -> x + 1)
        .filter(x -> x < 5)
        // BUG: Diagnostic contains:
        .forEach(cases[0].mutableValues::add);
  }

  private List<Integer> mutateStreamSourceLambdaExpression(
      ImmutableList<Integer> mutableParamList) {
    Stream<Integer> values1 =
        mutableParamList.stream()
            .map(
                x -> {
                  // BUG: Diagnostic contains:
                  mutableParamList.add(x);
                  return x + 1;
                });

    Stream<Integer> values2 =
        mutableParamList.stream()
            .filter(
                x -> {
                  // BUG: Diagnostic contains:
                  mutableParamList.remove(x);
                  return mutableParamList.size() > 5;
                });

    return Stream.concat(values1, values2).collect(Collectors.toList());
  }

  private List<Integer> getMutableValues() {
    return mutableValues;
  }
}
