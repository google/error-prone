/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class DuplicateAssertionTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DuplicateAssertion.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.List;

            class Test {
              public void test(List<Integer> list) {
                assertThat(list).contains(1);
                // BUG: Diagnostic contains:
                assertThat(list).contains(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_notDuplicated() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.List;

            class Test {
              public void test(List<Integer> list) {
                assertThat(list).contains(1);
                assertThat(list).contains(2);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_notAssertion() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void test() {
                int x = 1;
                x = 2;
                x = 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_impure() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.Iterator;
            import java.util.List;

            class Test {
              public void test(List<Integer> list) {
                Iterator<Integer> it = list.iterator();
                assertThat(it.next()).isEqualTo(1);
                assertThat(it.next()).isEqualTo(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void junit_assertEqualsDuplicated() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;

            import java.util.Iterator;

            class Test {
              public void test(Iterator<Object> o) {
                assertEquals(1, o.next());
                assertEquals(1, o.next());
              }
            }
            """)
        .doTest();
  }
}
