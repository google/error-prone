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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ModifiedButNotUsed} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class ModifiedButNotUsedTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ModifiedButNotUsed.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ModifiedButNotUsed.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {
                // BUG: Diagnostic contains:
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                List<Integer> bar;
                // BUG: Diagnostic contains:
                bar = new ArrayList<>();
                bar.add(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sideEffectFreeRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                List<Integer> bar = new ArrayList<>();
                bar.add(sideEffects());
                List<Integer> baz;
                baz = new ArrayList<>();
                baz.add(sideEffects());
              }

              int sideEffects() {
                return 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {}

              int sideEffects() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sideEffectPreservingRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {
                List<Integer> bar = new ArrayList<>();
                bar.add(sideEffects());
                List<Integer> baz;
                baz = new ArrayList<>();
                baz.add(sideEffects());
              }

              int sideEffects() {
                return 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {

                sideEffects();

                sideEffects();
              }

              int sideEffects() {
                return 1;
              }
            }
            """)
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void negatives() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            abstract class Test {
              void test() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                if (false) {
                  foo = new ArrayList<>();
                }
                int a = foo.get(0);
              }

              void test2() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                incoming(foo);
              }

              abstract void incoming(List<Integer> l);
            }
            """)
        .doTest();
  }

  @Test
  public void usedByAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              private List<Integer> bar;

              void test() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                bar = foo;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void usedDuringAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            abstract class Test {
              private List<Integer> bar;

              void test() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                foo = frobnicate(foo);
              }

              abstract List<Integer> frobnicate(List<Integer> a);
            }
            """)
        .doTest();
  }

  @Test
  public void negativeAfterReassignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {
                List<Integer> foo = new ArrayList<>();
                foo.add(1);
                foo.get(0);
                foo = new ArrayList<>();
                foo.add(1);
                foo.get(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void proto() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              static void foo() {
                // BUG: Diagnostic contains:
                TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();
                proto.setMessage(TestFieldProtoMessage.newBuilder());
                TestProtoMessage.Builder proto2 =
                    // BUG: Diagnostic contains:
                    TestProtoMessage.newBuilder().setMessage(TestFieldProtoMessage.newBuilder());
                TestProtoMessage.Builder proto3 =
                    // BUG: Diagnostic contains:
                    TestProtoMessage.getDefaultInstance().toBuilder().clearMessage();
                TestProtoMessage.Builder proto4 =
                    // BUG: Diagnostic contains:
                    TestProtoMessage.getDefaultInstance().toBuilder().clear();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoSideEffects() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              void foo() {
                TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();
                TestFieldProtoMessage.Builder builder = TestFieldProtoMessage.newBuilder();
                proto.setMessage(builder).setMessage(sideEffects());
              }

              TestFieldProtoMessage sideEffects() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              void foo() {

                TestFieldProtoMessage.Builder builder = TestFieldProtoMessage.newBuilder();
                sideEffects();
              }

              TestFieldProtoMessage sideEffects() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void protoNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              static TestProtoMessage foo() {
                TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();
                return proto.setMessage(TestFieldProtoMessage.newBuilder()).build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void immutableCollection() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static void foo() {
                // BUG: Diagnostic contains:
                ImmutableList.Builder<Integer> a = ImmutableList.builder();
                a.add(1);
                // BUG: Diagnostic contains:
                ImmutableList.Builder<Integer> b = ImmutableList.<Integer>builder().add(1);
                b.add(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoUnusedExpression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              void foo(TestProtoMessage proto) {
                // BUG: Diagnostic contains:
                proto.toBuilder().setMessage(TestFieldProtoMessage.newBuilder()).build();
                // BUG: Diagnostic contains:
                proto.toBuilder().setMessage(TestFieldProtoMessage.newBuilder());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoBuilderMergeFrom() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              void foo(TestProtoMessage proto) throws Exception {
                // BUG: Diagnostic contains:
                TestProtoMessage.newBuilder().mergeFrom(new byte[0]).build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoUnusedExpressionViaBuilderGetter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Test {
              void foo(TestProtoMessage proto) {
                // BUG: Diagnostic contains:
                proto.toBuilder().getMessageBuilder().clearField().build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void collectionUnusedExpression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              void foo() {
                // BUG: Diagnostic contains:
                ImmutableList.<Integer>builder().add(1).build();
                // BUG: Diagnostic contains:
                ImmutableList.<Integer>builder().add(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unusedPrefix_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              void test() {
                List<Integer> unusedIntegers = new ArrayList<>();
                unusedIntegers.add(1);
                List<Integer> ignored = new ArrayList<>();
                ignored.add(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parametersLoopsPrimitivesAndResourceVariables() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
              static class CloseableList extends ArrayList<Integer> implements AutoCloseable {
                @Override
                public void close() {}
              }

              int test(List<Integer> param, final List<Integer> finalParam, List<List<Integer>> all) {
                param.add(1);
                finalParam.add(2);
                int count = param.size();
                int[] array = new int[count];
                for (List<Integer> item : all) {
                  item.add(array.length);
                }
                List<Integer> alias = count > 0 ? param : finalParam;
                alias.add(3);
                // BUG: Diagnostic contains:
                try (CloseableList res = new CloseableList()) {
                  res.add(4);
                }
                return count;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceInitializer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.Iterator;

            class Test {
              /** A collection that is also a functional interface. */
              interface CollectionFactory<E> extends Collection<E> {
                Object create();

                default int size() { return 0; }
                default boolean isEmpty() { return true; }
                default boolean contains(Object o) { return false; }
                default Iterator<E> iterator() { return null; }
                default Object[] toArray() { return null; }
                default <T> T[] toArray(T[] a) { return a; }
                default boolean add(E e) { return false; }
                default boolean remove(Object o) { return false; }
                default boolean containsAll(Collection<?> c) { return false; }
                default boolean addAll(Collection<? extends E> c) { return false; }
                default boolean removeAll(Collection<?> c) { return false; }
                default boolean retainAll(Collection<?> c) { return false; }
                default void clear() {}
              }

              void test() {
                // BUG: Diagnostic contains:
                CollectionFactory<Integer> factory = ArrayList::new;
                factory.add(1);
              }
            }
            """)
        .doTest();
  }
}
