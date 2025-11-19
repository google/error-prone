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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AssertSameIncompatibleTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AssertSameIncompatible.class, getClass());

  @Test
  public void assertSame_compatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertSame;

            class Test {
              void f() {
                assertSame(new Object(), "foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertSame_generics_compatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertSame;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              void f(List<Object> list, ArrayList<Integer> arrayList) {
                assertSame(list, arrayList);
                assertSame(arrayList, list);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertSame_incompatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertSame;

            class Test {
              void f() {
                // BUG: Diagnostic contains:
                assertSame("foo", 1L);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertThat_compatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f() {
                assertThat(new Object()).isSameInstanceAs("foo");
                assertThat(new Object()).isNotSameInstanceAs("bar");
                assertThat("foo").isSameInstanceAs(new Object());
                assertThat("foo").isNotSameInstanceAs(new Object());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertThat_incompatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f() {
                // BUG: Diagnostic contains: always fail
                assertThat(1L).isSameInstanceAs("foo");

                // BUG: Diagnostic contains: always pass
                assertThat(1L).isNotSameInstanceAs("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void bothInterfaces_alwaysCompatible() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertSame;

            class Test {
              interface A {}

              interface B {}

              void f(A a, B b) {
                assertSame(a, b);
                assertSame(b, a);
              }
            }
            """)
        .doTest();
  }
}
