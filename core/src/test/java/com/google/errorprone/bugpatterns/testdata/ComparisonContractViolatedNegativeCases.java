/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

public class ComparisonContractViolatedNegativeCases {
  abstract static class IntOrInfinity implements Comparable<IntOrInfinity> {}

  static class IntOrInfinityInt extends IntOrInfinity {
    private final int value;

    IntOrInfinityInt(int value) {
      this.value = value;
    }

    @Override
    public int compareTo(IntOrInfinity o) {
      return (o instanceof IntOrInfinityInt)
          ? Integer.compare(value, ((IntOrInfinityInt) o).value)
          : 1;
    }
  }

  static class NegativeInfinity extends IntOrInfinity {
    @Override
    public int compareTo(IntOrInfinity o) {
      return (o instanceof NegativeInfinity) ? 0 : -1;
    }
  }
}
