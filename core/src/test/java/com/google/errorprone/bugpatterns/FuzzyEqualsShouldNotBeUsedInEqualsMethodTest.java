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
package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sulku@google.com (Marsela Sulku)
 */
@RunWith(JUnit4.class)
public class FuzzyEqualsShouldNotBeUsedInEqualsMethodTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FuzzyEqualsShouldNotBeUsedInEqualsMethod.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "FuzzyEqualsShouldNotBeUsedInEqualsMethodPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.math.DoubleMath;

            /**
             * @author sulku@google.com (Marsela Sulku)
             */
            public class FuzzyEqualsShouldNotBeUsedInEqualsMethodPositiveCases {

              public boolean equals(Object o) {
                // BUG: Diagnostic contains: DoubleMath.fuzzyEquals should never
                DoubleMath.fuzzyEquals(0.2, 9.3, 2.0);
                return true;
              }

              private class TestClass {

                public boolean equals(Object other) {
                  double x = 0, y = 0, z = 0;
                  // BUG: Diagnostic contains: DoubleMath.fuzzyEquals should never
                  return DoubleMath.fuzzyEquals(x, y, z);
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "FuzzyEqualsShouldNotBeUsedInEqualsMethodNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.math.DoubleMath;

            /**
             * @author sulku@google.com (Marsela Sulku)
             */
            public class FuzzyEqualsShouldNotBeUsedInEqualsMethodNegativeCases {
              public boolean equals() {
                return true;
              }

              private static class TestClass {
                public void test() {
                  boolean t = DoubleMath.fuzzyEquals(0, 2, 0.3);
                }

                public boolean equals(Object other) {
                  return true;
                }

                public boolean equals(Object other, double a) {
                  return DoubleMath.fuzzyEquals(0, 1, 0.2);
                }
              }
            }\
            """)
        .doTest();
  }
}
