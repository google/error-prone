/*
 * Copyright 2025 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class NullNeedsCastForVarargsTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(NullNeedsCastForVarargs.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NullNeedsCastForVarargs.class, getClass());

  @Test
  public void containsExactly_bareNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly(null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly((Object) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsExactly_objectArrayCastNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly((Object[]) null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly((Object) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsExactly_stringArrayCastNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly((String[]) null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly((Object) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void streamSubject_containsExactly_bareNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import java.util.stream.Stream;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                assertThat(Stream.of("a")).containsExactly(null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsAnyOf_bareNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                assertThat(ImmutableList.of("a")).containsAnyOf("b", "c", null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsAnyOf_objectArrayCastNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                assertThat(ImmutableList.of("a")).containsAnyOf("b", "c", (Object[]) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysAsList_bareNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<String> list = Arrays.asList(null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<String> list = Arrays.asList((String) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysAsList_integerTargetType_withArrayCast() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<String> list = Arrays.asList((String[]) null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<String> list = Arrays.asList((String) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysAsList_arrayTargetType_withArrayCast() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                List<Integer[]> list = Arrays.asList((Integer[][]) null);
              }
            }
            """)
        // The suggested replacement, `(Integer[]) null`, actually doesn't compile!
        // The next test shows that it works with an explicit type argument.
        // This is an edge case that I'm comfortable not handling.
        .doTest();
  }

  @Test
  public void arraysAsList_arrayTargetType_withArrayCastAndTypeArgument() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<Integer[]> list = Arrays.<Integer[]>asList((Integer[][]) null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                List<Integer[]> list = Arrays.<Integer[]>asList((Integer[]) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysAsList_arrayTargetType_withArrayCastAndTypeArgumentAndVar() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                var list = Arrays.<Integer[]>asList((Integer[][]) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysAsList_noTargetType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Arrays;

            class Test {
              void test() {
                var list = Arrays.asList(null);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;

            class Test {
              void test() {
                var list = Arrays.asList((Object) null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void streamOf() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.stream.Stream;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                var list = Stream.of(null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void moreGuavaMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Iterables;
            import com.google.common.collect.Iterators;
            import com.google.common.collect.Lists;
            import com.google.common.collect.Sets;
            import com.google.common.reflect.Invokable;
            import java.util.Iterator;
            import java.util.List;
            import java.util.Set;

            class Test {
              void test(Invokable<String, Boolean> invokable) throws Exception {
                // BUG: Diagnostic contains:
                List<String> arrayList = Lists.newArrayList((String[]) null);
                // BUG: Diagnostic contains:
                Set<String> hashSet = Sets.newHashSet((String[]) null);
                // BUG: Diagnostic contains:
                Iterable<String> cycle = Iterables.cycle((String[]) null);
                // BUG: Diagnostic contains:
                Iterator<String> forArray = Iterators.forArray(null);
                // BUG: Diagnostic contains:
                invokable.invoke("", null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.Lists;
            import com.google.common.collect.Sets;
            import java.util.Arrays;
            import java.util.List;
            import java.util.Set;
            import java.util.stream.Stream;

            class Test {
              void test() {
                assertThat(ImmutableList.of("a")).containsExactly("a");
                assertThat(ImmutableList.of("a")).containsExactly((Object) null);
                assertThat(ImmutableList.of("a")).containsExactly(null, null);

                assertThat(ImmutableList.of("a")).containsAnyOf("a", "b");
                assertThat(ImmutableList.of("a")).containsAnyOf("a", "b", (Object) null);
                assertThat(ImmutableList.of("a")).containsAnyOf("a", "b", null, null);

                List<Object> listOfObjects = Arrays.asList((Object) null);
                List<String> listOfStrings = Arrays.asList((String) null);
                List<Integer[]> listOfIntegerArrays = Arrays.<Integer[]>asList((Integer[]) null);

                List<Object> someNewArrayList = Lists.newArrayList(listOfObjects);
                Set<Object> someNewHashSet = Sets.newHashSet(listOfObjects);
                Stream<Object[]> streamOfArrays = Stream.<Object[]>of((Object[]) null);
              }
            }
            """)
        .doTest();
  }
}
