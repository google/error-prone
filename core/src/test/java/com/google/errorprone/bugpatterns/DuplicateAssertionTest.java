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

  @Test
  public void twoStringParameters() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.io.IOException;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import junit.framework.Assert;

            class Test {
              public final void checkContents(String relativePath, String expectedContents) throws IOException {
                String actualContents = Files.readString(Path.of(relativePath));
                Assert.assertEquals(expectedContents, actualContents);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditions() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;

            class Test {
              public void test(Object o) {
                Preconditions.checkNotNull(o);
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void requireNonNull() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              public void test(Object o) {
                Objects.requireNonNull(o);
                // BUG: Diagnostic contains:
                Objects.requireNonNull(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditionsWithImpureExpression_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;
            import java.util.Iterator;

            class Test {
              public void test(Iterator<Object> it) {
                Preconditions.checkNotNull(it.next());
                Preconditions.checkNotNull(it.next());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditionsWithMessages() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;

            class Test {
              public void test(Object o, boolean b) {
                Preconditions.checkNotNull(o, "msg");
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg");
                Preconditions.checkArgument(b, "foo %s", o);
                // BUG: Diagnostic contains:
                Preconditions.checkArgument(b, "foo %s", o);
                Preconditions.checkState(b, "bar %s", 1);
                // BUG: Diagnostic contains:
                Preconditions.checkState(b, "bar %s", 1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void verifyWithMessages() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Verify;

            class Test {
              public void test(Object o, boolean b) {
                Verify.verify(b);
                // BUG: Diagnostic contains:
                Verify.verify(b);
                // BUG: Diagnostic contains:
                Verify.verify(b, "msg %s", o);
                // BUG: Diagnostic contains:
                Verify.verify(b, "msg %s", o);
                Verify.verifyNotNull(o);
                // BUG: Diagnostic contains:
                Verify.verifyNotNull(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditionsWithDifferentMessages_findings() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;

            class Test {
              public void test(Object o) {
                Preconditions.checkNotNull(o, "msg1");
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg2");
                Preconditions.checkArgument(o != null, "msg3");
                // BUG: Diagnostic contains:
                Preconditions.checkArgument(o != null, "msg4");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditions_mixedOverloads() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;

            class Test {
              public void test(Object o) {
                Preconditions.checkNotNull(o);
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg1");
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg %s", "arg");
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg %s", "other");
                // BUG: Diagnostic contains:
                Preconditions.checkNotNull(o, "msg %s", "other");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preconditionThenDereferenced_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkNotNull;

            class Test {
              void test(Object foo) {
                checkNotNull(foo).toString();
                checkNotNull(foo).hashCode();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void checkElementIndex_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;

            class Test {
              void test(int i, int size1, int size2) {
                Preconditions.checkElementIndex(i, size1);
                Preconditions.checkElementIndex(i, size2);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truth_variousStyles() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.OptionalSubject.optionals;
            import static com.google.common.truth.Truth.assertAbout;
            import static com.google.common.truth.Truth.assertWithMessage;
            import static com.google.common.truth.Truth.assert_;

            import java.util.Optional;

            class Test {
              public void test(Optional<String> s) {
                assert_().that(s).isPresent();
                // BUG: Diagnostic contains:
                assert_().that(s).isPresent();

                assertWithMessage("msg").that(s).isPresent();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").that(s).isPresent();

                assertAbout(optionals()).that(s).isPresent();
                // BUG: Diagnostic contains:
                assertAbout(optionals()).that(s).isPresent();

                assertWithMessage("msg").about(optionals()).that(s).isPresent();
                // BUG: Diagnostic contains:
                assertWithMessage("msg").about(optionals()).that(s).isPresent();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truth_expectAndAssume() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.OptionalSubject.optionals;
            import static com.google.common.truth.TruthJUnit.assume;

            import com.google.common.truth.Expect;
            import java.util.Optional;
            import org.junit.Rule;

            class Test {
              @Rule public final Expect expect = Expect.create();

              public void test(Optional<String> s) {
                expect.that(s).isPresent();
                // BUG: Diagnostic contains:
                expect.that(s).isPresent();

                assume().that(s).isPresent();
                // BUG: Diagnostic contains:
                assume().that(s).isPresent();

                expect.about(optionals()).that(s).isPresent();
                // BUG: Diagnostic contains:
                expect.about(optionals()).that(s).isPresent();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truth_withArguments() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.List;

            class Test {
              public void test(List<Integer> list) {
                assertThat(list).containsAtLeast(1, 2);
                // BUG: Diagnostic contains:
                assertThat(list).containsAtLeast(1, 2);

                assertThat(list).containsAtLeast(1, 2).inOrder();
                // BUG: Diagnostic contains:
                assertThat(list).containsAtLeast(1, 2).inOrder();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truth_withDifferentArguments_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.List;

            class Test {
              public void test(List<Integer> list) {
                assertThat(list).containsAtLeast(1, 2);
                assertThat(list).containsAtLeast(1, 3);

                assertThat(list).containsAtLeast(1, 2).inOrder();
                assertThat(list).containsAtLeast(2, 1).inOrder();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoTruth() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

            import com.google.protobuf.Message;

            class Test {
              public void test(Message message) {
                assertThat(message).hasAllRequiredFields();
                // BUG: Diagnostic contains:
                assertThat(message).hasAllRequiredFields();
              }
            }
            """)
        .doTest();
  }
}
