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

import com.google.common.base.Joiner;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link UndefinedEquals}
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@RunWith(JUnitParamsRunner.class)
public final class UndefinedEqualsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UndefinedEquals.class, getClass());

  @Test
  public void positiveInstanceEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Queue;

            class Test {
              void f(Queue a, Queue b) {
                // BUG: Diagnostic contains: Queue does not have well-defined equality semantics
                a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveStaticEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.Objects;

            class Test {
              void f(Collection a, Collection b) {
                // BUG: Diagnostic contains: Collection
                Objects.equals(a, b);
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
            import com.google.common.collect.ImmutableCollection;
            import java.util.Objects;

            class Test {
              void f(ImmutableCollection a, ImmutableCollection b) {
                // BUG: Diagnostic contains: ImmutableCollection
                Objects.equals(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveAssertEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;
            import static org.junit.Assert.assertNotEquals;

            import com.google.common.collect.FluentIterable;
            import com.google.common.collect.Iterables;
            import java.util.List;

            class Test {
              void test(List myList, List otherList) {
                // BUG: Diagnostic contains: Iterable
                assertEquals(FluentIterable.of(1), FluentIterable.of(1));
                // BUG: Diagnostic contains: Iterable
                assertEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));
                // BUG: Diagnostic contains: Iterable
                assertNotEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));
                // BUG: Diagnostic contains: Iterable
                assertEquals("foo", Iterables.skip(myList, 1), Iterables.skip(myList, 2));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveWithGenerics() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Queue;

            class Test {
              <T> void f(Queue<String> a, Queue<T> b) {
                // BUG: Diagnostic contains: Queue
                a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveTruth() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import java.util.Queue;

            class Test {
              <T> void f(Queue<String> a, Queue<T> b) {
                // BUG: Diagnostic contains: Queue
                assertThat(a).isEqualTo(b);
                // BUG: Diagnostic contains: Queue
                assertThat(a).isNotEqualTo(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  @Parameters(method = "truthFixParameters")
  public void truthFixParameterized(String input, String output) {
    BugCheckerRefactoringTestHelper.newInstance(UndefinedEquals.class, getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.Multimap;",
            "import java.lang.Iterable;",
            "import java.util.Collection;",
            "import java.util.Queue;",
            "class Test {",
            input,
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.Multimap;",
            "import java.lang.Iterable;",
            "import java.util.Collection;",
            "import java.util.Queue;",
            "class Test {",
            output,
            "}")
        .doTest();
  }

  private static Object truthFixParameters() {
    return new Object[] {
      new String[] {
        lines(
            "  void f1(Multimap a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f1(Multimap a, Multimap b) {", //
            "    assertThat(a).containsExactlyEntriesIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f2(Multimap a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f2(Multimap a, Collection b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f3(Multimap a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f3(Multimap a, Iterable b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f4(Multimap a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f4(Multimap a, Queue b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f5(Collection a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f5(Collection a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f6(Collection a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f6(Collection a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f7(Collection a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f7(Collection a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f8(Collection a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f8(Collection a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f9(Iterable a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f9(Iterable a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f10(Iterable a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f10(Iterable a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f11(Iterable a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f11(Iterable a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f12(Iterable a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f12(Iterable a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f13(Queue a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f13(Queue a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f14(Queue a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f14(Queue a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f15(Queue a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f15(Queue a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f16(Queue a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f16(Queue a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      }
    };
  }

  private static String lines(String... lines) {
    return Joiner.on('\n').join(lines);
  }

  @Test
  public void truthFixAssertWithMessage() {
    BugCheckerRefactoringTestHelper.newInstance(UndefinedEquals.class, getClass())
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertWithMessage;

            class Test {
              void f(Iterable a, Iterable b) {
                assertWithMessage("message").that(a).isEqualTo(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertWithMessage;

            class Test {
              void f(Iterable a, Iterable b) {
                assertWithMessage("message").that(a).containsExactlyElementsIn(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truthFixDontRewriteIsNotEqualTo() {
    BugCheckerRefactoringTestHelper.newInstance(UndefinedEquals.class, getClass())
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f(Iterable a, Iterable b) {
                assertThat(a).isNotEqualTo(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f(Iterable a, Iterable b) {
                assertThat(a).isNotEqualTo(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truthFixAcrossMultipleLinesAndPoorlyFormatted() {
    BugCheckerRefactoringTestHelper.newInstance(UndefinedEquals.class, getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertThat(a).", // period should be on following line per style guide
            "      isEqualTo(b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f(Iterable a, Iterable b) {
                assertThat(a).containsExactlyElementsIn(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveSparseArray() {
    compilationHelper
        .addSourceLines(
            "SparseArray.java",
            """
            package android.util;

            public class SparseArray<T> {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import android.util.SparseArray;

            class Test {
              <T> boolean f(SparseArray<T> a, SparseArray<T> b) {
                // BUG: Diagnostic contains: SparseArray
                return a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.PriorityQueue;

            class Test {
              void f(PriorityQueue a, PriorityQueue b) {
                a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void charSequenceFix() {
    BugCheckerRefactoringTestHelper.newInstance(UndefinedEquals.class, getClass())
        .addInputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f(CharSequence a, String b) {
                assertThat(a).isEqualTo(b);
                assertThat(b).isEqualTo(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void f(CharSequence a, String b) {
                assertThat(a.toString()).isEqualTo(b);
                assertThat(b).isEqualTo(a.toString());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void futures() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.util.concurrent.ListenableFuture;
            import com.google.common.util.concurrent.SettableFuture;
            import java.util.concurrent.CompletableFuture;
            import java.util.concurrent.Future;

            class Test {
              void listenableFuture(ListenableFuture a, ListenableFuture b) {
                // BUG: Diagnostic contains: Future does not have well-defined equality semantics
                a.equals(b);
              }

              void settableFuture(SettableFuture a, SettableFuture b) {
                // BUG: Diagnostic contains: Future does not have well-defined equality semantics
                a.equals(b);
              }

              void completableFuture(CompletableFuture a, CompletableFuture b) {
                // BUG: Diagnostic contains: Future does not have well-defined equality semantics
                a.equals(b);
              }

              void future(Future a, Future b) {
                // BUG: Diagnostic contains: Future does not have well-defined equality semantics
                a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void collectionOfCollections() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;

            class Test {
              boolean foo(Collection<Collection<Integer>> xs, Collection<Integer> x) {
                // BUG: Diagnostic contains: Collection does not have well-defined equality
                return xs.contains(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void collectionOfLists_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.List;

            class Test {
              boolean foo(Collection<List<Integer>> xs, List<Integer> x) {
                return xs.contains(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void treeMap_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.TreeMap;

            class Test {
              boolean foo(TreeMap<Collection<Integer>, Integer> xs, Collection<Integer> x) {
                return xs.containsKey(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sortedMap_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.SortedMap;

            class Test {
              boolean foo(SortedMap<Collection<Integer>, Integer> xs, Collection<Integer> x) {
                return xs.containsKey(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sortedSet_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.SortedSet;

            class Test {
              boolean foo(SortedSet<Collection<Integer>> xs, Collection<Integer> x) {
                return xs.contains(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayAsKeyPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ArrayListMultimap;
            import com.google.common.collect.HashBiMap;
            import com.google.common.collect.HashMultimap;
            import com.google.common.collect.HashMultiset;
            import com.google.common.collect.LinkedHashMultimap;
            import com.google.common.collect.LinkedHashMultiset;
            import com.google.common.collect.LinkedListMultimap;
            import com.google.common.collect.Maps;
            import com.google.common.collect.Sets;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.LinkedHashMap;
            import java.util.Map;
            import java.util.Set;
            import java.util.concurrent.ConcurrentHashMap;

            class Test {
              public static void main(String[] args) {
                // BUG: Diagnostic contains: Arrays do not override equals()
                Map<String[], Integer> testNewMap = Maps.newHashMap();
                // BUG: Diagnostic contains: Arrays do not override equals()
                Set<String[]> testNewSet = Sets.newHashSet();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashMap<String[], Integer> testNewHashMap = Maps.newHashMap();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashSet<String[]> testNewHashSet = Sets.newHashSet();
                // BUG: Diagnostic contains: Arrays do not override equals()
                Map<String[], Integer> testMap = new HashMap<String[], Integer>();
                // BUG: Diagnostic contains: Arrays do not override equals()
                Set<String[]> testSet = new HashSet<String[]>();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashMap<String[], Integer> testHashMap = new HashMap<String[], Integer>();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashSet<String[]> testHashSet = new HashSet<String[]>();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashMultimap<String[], Integer> testHashMultimap = HashMultimap.create();
                ArrayListMultimap<String[], Integer> testArrayListMultimap =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    ArrayListMultimap.create();
                LinkedHashMultimap<String[], Integer> testLinkedHashMultimap =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    LinkedHashMultimap.create();
                LinkedListMultimap<String[], Integer> testLinkedListMultimap =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    LinkedListMultimap.create();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashBiMap<String[], Integer> testHashBiMap = HashBiMap.create();
                LinkedHashMap<String[], Integer> testLinkedHashMap =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    new LinkedHashMap<String[], Integer>();
                ConcurrentHashMap<String[], Integer> testConcurrentHashMap =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    new ConcurrentHashMap<String[], Integer>();
                // BUG: Diagnostic contains: Arrays do not override equals()
                HashMultiset<String[]> testHashMultiSet = HashMultiset.create();
                // BUG: Diagnostic contains: Arrays do not override equals()
                LinkedHashMultiset<String[]> testLinkedHashMultiSet = LinkedHashMultiset.create();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayAsKeyNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import com.google.common.collect.ArrayListMultimap;
            import com.google.common.collect.HashBiMap;
            import com.google.common.collect.HashMultimap;
            import com.google.common.collect.HashMultiset;
            import com.google.common.collect.LinkedHashMultimap;
            import com.google.common.collect.LinkedHashMultiset;
            import com.google.common.collect.LinkedListMultimap;
            import com.google.common.collect.Maps;
            import com.google.common.collect.Ordering;
            import com.google.common.collect.Sets;
            import java.util.Arrays;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.LinkedHashMap;
            import java.util.Map;
            import java.util.Set;
            import java.util.TreeSet;
            import java.util.concurrent.ConcurrentHashMap;

            class Test {
              public static void main(String[] args) {
                Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
                Set<String> testSet = new HashSet<String>();
                HashMap<Integer, Integer> testHashMap = new HashMap<Integer, Integer>();
                HashSet<String> testHashSet = new HashSet<String>();
                Set testSet2 = new HashSet();
                Map testMap2 = new HashMap();
                Map<Integer, Integer> mapFromMethod = Maps.newHashMap();
                Set<String> setFromMethod = Sets.newHashSet();
                Ordering<String> natural = Ordering.natural();
                Set<String[]> thisShouldWork =
                    new TreeSet<String[]>(natural.lexicographical().onResultOf(Arrays::asList));
                HashMultimap<String, Integer> testHashMultimap = HashMultimap.create();
                ArrayListMultimap<String, Integer> arrayListMultimap = ArrayListMultimap.create();
                LinkedHashMultimap<String, Integer> linkedMultimap = LinkedHashMultimap.create();
                LinkedListMultimap<String, Integer> listMultimap = LinkedListMultimap.create();
                HashBiMap<String, Integer> testHashBiMap = HashBiMap.create();
                LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
                ConcurrentHashMap<String, Integer> testConcurrentHashMap =
                    new ConcurrentHashMap<String, Integer>();
                HashMultiset<String> testHashMultiSet = HashMultiset.create();
                LinkedHashMultiset<String> testLinkedHashMultiSet = LinkedHashMultiset.create();
                assertThat(args).isEqualTo(args);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void collectors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableMap.toImmutableMap;
            import static com.google.common.collect.ImmutableSet.toImmutableSet;
            import static java.util.stream.Collectors.groupingBy;
            import static java.util.stream.Collectors.toList;
            import static java.util.stream.Collectors.toMap;
            import static java.util.stream.Collectors.toSet;

            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableSet;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            import java.util.TreeMap;
            import java.util.stream.Stream;

            class Test {
              void f(Stream<String[]> stream) {
                // BUG: Diagnostic contains: Arrays do not override equals()
                Set<String[]> s1 = stream.collect(toSet());
                // BUG: Diagnostic contains: Arrays do not override equals()
                ImmutableSet<String[]> s2 = stream.collect(toImmutableSet());
                // BUG: Diagnostic contains: Arrays do not override equals()
                Map<String[], String> m1 = stream.collect(toMap(x -> x, x -> ""));
                ImmutableMap<String[], String> m2 =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    stream.collect(toImmutableMap(x -> x, x -> ""));
                // BUG: Diagnostic contains: Arrays do not override equals()
                Map<String[], List<String[]>> m3 = stream.collect(groupingBy(x -> x));
                Map<String[], List<String[]>> m4 =
                    // BUG: Diagnostic contains: Arrays do not override equals()
                    stream.collect(groupingBy(x -> x, HashMap::new, toList()));
                Map<String[], List<String[]>> sorted =
                    stream.collect(groupingBy(x -> x, () -> new TreeMap<>(), toList()));
                List<String[]> list = stream.collect(toList());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hashKeys() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.HashMultimap;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.Multimap;
            import java.util.Collection;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.IdentityHashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            import java.util.TreeMap;

            class Test {
              void f(CharSequence cs) {
                // BUG: Diagnostic contains: CharSequence does not have well-defined equality
                Set<CharSequence> s = new HashSet<>();
                // BUG: Diagnostic contains: Collection does not have well-defined equality
                Map<Collection<String>, String> m = new HashMap<>();
                // BUG: Diagnostic contains: CharSequence does not have well-defined equality
                ImmutableSet<CharSequence> i = ImmutableSet.of(cs);
                // BUG: Diagnostic contains: Iterable does not have well-defined equality
                Multimap<Iterable<String>, String> mm = HashMultimap.create();
                Map<Collection<String>, String> identity = new IdentityHashMap<>();
                Map<Collection<String>, String> sorted = new TreeMap<>();
                Map<String, Collection<String>> values = new HashMap<>();
                Set<List<String>> lists = new HashSet<>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hashKeys_arraysSkipIdentityAndSortedContainers() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;
            import java.util.Map;
            import java.util.TreeMap;
            import java.util.WeakHashMap;

            class Test {
              void f() {
                Map<String[], String> identity = new IdentityHashMap<>();
                Map<String[], String> sorted = new TreeMap<>((a, b) -> 0);
                // BUG: Diagnostic contains: Arrays do not override equals()
                Map<String[], String> weak = new WeakHashMap<>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressionWithOldNames() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;
            import java.util.HashSet;
            import java.util.Set;

            class Test {
              @SuppressWarnings("ArrayAsKeyOfSetOrMap")
              void f() {
                Set<String[]> s = new HashSet<>();
              }

              @SuppressWarnings("CollectionUndefinedEquality")
              boolean g(Collection<Collection<Integer>> xs, Collection<Integer> x) {
                return xs.contains(x);
              }
            }
            """)
        .doTest();
  }
}
