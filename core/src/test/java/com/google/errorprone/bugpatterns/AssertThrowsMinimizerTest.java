/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AssertThrowsMultipleStatements}Test */
@RunWith(JUnit4.class)
public class AssertThrowsMinimizerTest {

  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(AssertThrowsMinimizer.class, getClass())
          .addInputLines(
              "Foo.java",
              """
              interface Foo {
                static Builder builder() {
                  return null;
                }
                interface Builder {
                  Builder setBar(Bar bar);
                  Foo build();
                }
              }
              """)
          .expectUnchanged()
          .addInputLines(
              "Bar.java",
              """
              class Bar {
              }
              """)
          .expectUnchanged();

  @Test
  public void refactor() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      Foo.builder().setBar(new Bar());
                    });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Foo.Builder builder = Foo.builder();
                Bar bar = new Bar();
                assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void variable() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                IllegalStateException ise =
                    assertThrows(
                        IllegalStateException.class,
                        () -> {
                          Foo.builder().setBar(new Bar());
                        });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Foo.Builder builder = Foo.builder();
                Bar bar = new Bar();
                IllegalStateException ise = assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import com.google.common.collect.ImmutableList;

            class Test {
              void f() {
                ImmutableList.Builder<Integer> builder =
                    ImmutableList.<Integer>builder().add(1).add(Integer.valueOf(2));
                assertThrows(IllegalStateException.class, () -> builder.add(2));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeNull() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              void f() {
                List<Integer> list = new ArrayList<>();
                assertThrows(IllegalStateException.class, () -> list.add(null));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeField() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              List<Integer> list = new ArrayList<>();

              void f() {
                assertThrows(IllegalStateException.class, () -> list.add(42));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeFieldQualifiedWithThis() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              List<Integer> list = new ArrayList<>();

              void f() {
                assertThrows(IllegalStateException.class, () -> this.list.add(42));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void twoAssertions() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Bar bar = new Bar();
                assertThrows(IllegalStateException.class, () -> Foo.builder().setBar(bar));
                assertThrows(IllegalStateException.class, () -> Foo.builder().setBar(bar));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Bar bar = new Bar();
                Foo.Builder builder = Foo.builder();
                assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
                Foo.Builder builder2 = Foo.builder();
                assertThrows(IllegalStateException.class, () -> builder2.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }
}
