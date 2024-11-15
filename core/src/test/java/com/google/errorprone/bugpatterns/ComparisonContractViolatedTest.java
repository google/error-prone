/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class ComparisonContractViolatedTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ComparisonContractViolated.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ComparisonContractViolatedPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.Comparator;

            public class ComparisonContractViolatedPositiveCases {

              static final int POSITIVE_CONSTANT = 50;

              static class Struct {
                int intField;
                long longField;

                @Override
                public boolean equals(Object o) {
                  return o instanceof Struct
                      && intField == ((Struct) o).intField
                      && longField == ((Struct) o).longField;
                }

                @Override
                public int hashCode() {
                  return intField + (int) longField;
                }
              }

              static final Comparator<Struct> intComparisonNoZero1 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Integer.compare(left.intField, right.intField)
                      return (left.intField < right.intField) ? -1 : 1;
                    }
                  };

              static final Comparator<Struct> intComparisonNoZero2 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Integer.compare(left.intField, right.intField)
                      return (right.intField < left.intField) ? 1 : -1;
                    }
                  };

              static final Comparator<Struct> intComparisonNoZero3 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Integer.compare(left.intField, right.intField)
                      return (left.intField > right.intField) ? 1 : -1;
                    }
                  };

              static final Comparator<Struct> intComparisonNoZero4 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Integer.compare(left.intField, right.intField)
                      return (left.intField <= right.intField) ? -1 : 1;
                    }
                  };

              static final Comparator<Struct> longComparisonNoZero1 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Long.compare(left.longField, right.longField)
                      return (left.longField < right.longField) ? -1 : 1;
                    }
                  };

              static final Comparator<Struct> longComparisonNoZero2 =
                  new Comparator<Struct>() {
                    @Override
                    public int compare(Struct left, Struct right) {
                      // BUG: Diagnostic contains: Long.compare(left.longField, right.longField)
                      return (left.longField < right.longField) ? -1 : POSITIVE_CONSTANT;
                    }
                  };

              static final Comparator<Struct> zeroOrOneComparator =
                  new Comparator<Struct>() {

                    @Override
                    // BUG: Diagnostic contains: violates the contract
                    public int compare(Struct o1, Struct o2) {
                      return o1.equals(o2) ? 0 : 1;
                    }
                  };

              static final Comparator<Struct> zeroOrNegativeOneComparator =
                  new Comparator<Struct>() {

                    @Override
                    // BUG: Diagnostic contains: violates the contract
                    public int compare(Struct o1, Struct o2) {
                      return o1.equals(o2) ? 0 : -1;
                    }
                  };

              static final Comparator<Struct> zeroOrPositiveConstantComparator =
                  new Comparator<Struct>() {

                    @Override
                    // BUG: Diagnostic contains: violates the contract
                    public int compare(Struct o1, Struct o2) {
                      return o1.equals(o2) ? 0 : POSITIVE_CONSTANT;
                    }
                  };
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ComparisonContractViolatedNegativeCases.java",
            """
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
            }\
            """)
        .doTest();
  }
}
