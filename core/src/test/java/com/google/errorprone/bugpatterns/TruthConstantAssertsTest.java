/*
 * Copyright 2016 The Error Prone Authors.
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
 * Unit tests for {@link TruthConstantAsserts} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public final class TruthConstantAssertsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TruthConstantAsserts.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "TruthConstantAssertsPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.truth.Truth.assertThat;

/**
 * Positive test cases for TruthConstantAsserts check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthConstantAssertsPositiveCases {

  public void testAssertThat() {
    // BUG: Diagnostic contains: assertThat(new TruthConstantAssertsPositiveCases()).isEqualTo(1);
    assertThat(1).isEqualTo(new TruthConstantAssertsPositiveCases());

    // BUG: Diagnostic contains: assertThat(someStaticMethod()).isEqualTo("my string");
    assertThat("my string").isEqualTo(someStaticMethod());

    // BUG: Diagnostic contains: assertThat(memberMethod()).isEqualTo(42);
    assertThat(42).isEqualTo(memberMethod());

    // BUG: Diagnostic contains: assertThat(someStaticMethod()).isEqualTo(42L);
    assertThat(42L).isEqualTo(someStaticMethod());

    // BUG: Diagnostic contains: assertThat(new Object()).isEqualTo(4.2);
    assertThat(4.2).isEqualTo(new Object());
  }

  private static TruthConstantAssertsPositiveCases someStaticMethod() {
    return new TruthConstantAssertsPositiveCases();
  }

  private TruthConstantAssertsPositiveCases memberMethod() {
    return new TruthConstantAssertsPositiveCases();
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "TruthConstantAssertsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            /**
             * Negative test cases for TruthConstantAsserts check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class TruthConstantAssertsNegativeCases {

              public void testNegativeCases() {
                assertThat(new TruthConstantAssertsNegativeCases()).isEqualTo(Boolean.TRUE);
                assertThat(getObject()).isEqualTo(Boolean.TRUE);

                // assertion called on constant with constant expectation is ignored.
                assertThat(Boolean.FALSE).isEqualTo(4.2);
              }

              private static TruthConstantAssertsNegativeCases getObject() {
                return new TruthConstantAssertsNegativeCases();
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCase_viaImmutableList() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import com.google.common.collect.ImmutableList;

            public class Test {
              public void test(Object a) {
                // BUG: Diagnostic contains:
                assertThat(ImmutableList.of(1, 2, 3)).isEqualTo(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveWithEnumOnLeftHandSide() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            public class Test {
              enum TestEnum {
                A,
                B
              }

              public void test(Object a) {
                // BUG: Diagnostic contains:
                assertThat(TestEnum.A).isEqualTo(a);
              }
            }
            """)
        .doTest();
  }
}
