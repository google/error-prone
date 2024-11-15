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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link SelfAssertion} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class SelfAssertionTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(SelfAssertion.class, getClass());
  }

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "SelfAssertionPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;
            import static com.google.common.truth.Truth.assertWithMessage;

            /**
             * Positive test cases for SelfAssertion check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfAssertionPositiveCases {

              public void testAssertThatEq() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertThat(test).isEqualTo(test);
              }

              public void testAssertWithMessageEq() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").that(test).isEqualTo(test);
              }

              public void testAssertThatSame() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertThat(test).isSameInstanceAs(test);
              }

              public void testAssertWithMessageSame() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").that(test).isSameInstanceAs(test);
              }

              public void testAssertThatNeq() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertThat(test).isNotEqualTo(test);
              }

              public void testAssertThatNotSame() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertThat(test).isNotSameInstanceAs(test);
              }

              public void testAssertWithMessageNeq() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").that(test).isNotEqualTo(test);
              }

              public void testAssertWithMessageNotSame() {
                String test = Boolean.TRUE.toString();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").that(test).isNotSameInstanceAs(test);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "SelfAssertionNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            /**
             * Negative test cases for SelfAssertion check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfAssertionNegativeCases {

              public void testEq() {
                assertThat(Boolean.TRUE.toString()).isEqualTo(Boolean.FALSE.toString());
              }

              public void testNeq() {
                assertThat(Boolean.TRUE.toString()).isNotEqualTo(Boolean.FALSE.toString());
              }
            }\
            """)
        .doTest();
  }

  // regression test for b/32107126
  @Test
  public void customReceiver() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.truth.IntegerSubject;
            import java.util.Arrays;

            abstract class Test {
              abstract IntegerSubject f(int i);

              abstract IntegerSubject g();

              void test(int x) {
                f(x).isEqualTo(x);
                g().isEqualTo(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void iterables() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import java.util.List;

            abstract class Test {
              void test(List<String> xs) {
                // BUG: Diagnostic contains:
                assertThat(xs).containsExactlyElementsIn(xs);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sameIdentifierWhenNotFinal_stillFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import java.time.Duration;

            abstract class Test {
              void test(int x) {
                x = 2;
                // BUG: Diagnostic contains:
                assertThat(x).isEqualTo(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constantExpressions() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import java.time.Duration;

            abstract class Test {
              void test(int x) {
                // BUG: Diagnostic contains:
                assertThat(Duration.ofMillis(x)).isEqualTo(Duration.ofMillis(x));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void junitPositiveAssertion() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;

            abstract class Test {
              void test(int x) {
                // BUG: Diagnostic contains: pass
                assertEquals(x, x);
                // BUG: Diagnostic contains: pass
                assertEquals("foo", x, x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void junitNegativeAssertion() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertNotEquals;

            abstract class Test {
              void test(int x) {
                // BUG: Diagnostic contains: fail
                assertNotEquals(x, x);
                // BUG: Diagnostic contains: fail
                assertNotEquals("foo", x, x);
              }
            }
            """)
        .doTest();
  }
}
