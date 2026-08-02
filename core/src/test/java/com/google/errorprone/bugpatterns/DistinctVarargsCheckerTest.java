/*
 * Copyright 2021 The Error Prone Authors.
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

import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DistinctVarargsChecker}Test */
@RunWith(JUnit4.class)
public class DistinctVarargsCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DistinctVarargsChecker.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(DistinctVarargsChecker.class, getClass());

  @Test
  public void distinctVarargsChecker_sameVariableInFuturesVaragsMethods_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;

            public class Test {
              void testFunction() {
                ListenableFuture firstFuture = null, secondFuture = null;
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Futures.whenAllSucceed(firstFuture, firstFuture);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Futures.whenAllSucceed(firstFuture, firstFuture, secondFuture);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Futures.whenAllComplete(firstFuture, firstFuture);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Futures.whenAllComplete(firstFuture, firstFuture, secondFuture);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void distinctVarargsCheckerdifferentVariableInFuturesVaragsMethods_shouldNotFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;

            public class Test {
              void testFunction() {
                ListenableFuture firstFuture = null, secondFuture = null;
                Futures.whenAllComplete(firstFuture);
                Futures.whenAllSucceed(firstFuture, secondFuture);
                Futures.whenAllComplete(firstFuture);
                Futures.whenAllComplete(firstFuture, secondFuture);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_sameVariableInVarargMethods_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import static com.google.errorprone.matchers.Matchers.anyOf;
            import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

            import com.google.common.base.MoreObjects;
            import com.google.common.base.Predicate;
            import com.google.common.base.Predicates;
            import com.google.common.collect.ImmutableBiMap;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedMap;
            import com.google.common.collect.ImmutableSortedSet;
            import com.google.common.collect.Iterables;
            import com.google.common.collect.Maps;
            import com.google.common.collect.Ordering;
            import com.google.common.collect.Sets;
            import com.google.common.primitives.Ints;
            import com.google.errorprone.matchers.Matcher;
            import com.google.protobuf.FieldMask;
            import com.google.protobuf.util.FieldMaskUtil;
            import com.sun.source.tree.ExpressionTree;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.nio.file.StandardCopyOption;
            import java.util.Collections;
            import java.util.EnumSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.Set;
            import java.util.concurrent.CompletableFuture;
            import java.util.stream.IntStream;
            import java.util.stream.Stream;
            import org.mockito.Mockito;

            public class Test {
              enum TestEnum { A, B }

              void testFunction() throws Exception {
                int first = 1, second = 2;
                CompletableFuture f1 = null, f2 = null;
                Predicate p1 = null, p2 = null;
                Path path1 = null, path2 = null;
                Set set1 = null, set2 = null;
                Stream str1 = null, str2 = null;
                IntStream istr1 = null, istr2 = null;
                List list1 = null, list2 = null;
                Map map1 = null, map2 = null;
                FieldMask mask1 = null, mask2 = null;
                Matcher<ExpressionTree> m1 = null, m2 = null;
                String s1 = "a", s2 = "b";

                // BUG: Diagnostic contains: DistinctVarargsChecker
                Ordering.explicit(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Ordering.explicit(first, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Map.of(first, second, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                ImmutableSortedMap.of(first, second, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Set.of(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                ImmutableSet.of(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                ImmutableSet.of(first, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                ImmutableSortedSet.of(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                ImmutableSortedSet.of(first, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Objects.hash(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Objects.hash(first, second, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                MoreObjects.firstNonNull(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Objects.requireNonNullElse(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                com.google.common.base.Objects.hashCode(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                CompletableFuture.allOf(f1, f1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                CompletableFuture.anyOf(f1, f1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Mockito.verifyNoInteractions(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Mockito.verifyNoMoreInteractions(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Mockito.inOrder(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Mockito.ignoreStubs(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                assertThat(map1).containsExactly(first, second, first, second);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                EnumSet.of(TestEnum.A, TestEnum.A);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Predicates.and(p1, p1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Predicates.or(p1, p1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Files.copy(path1, path1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.REPLACE_EXISTING);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Sets.intersection(set1, set1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Sets.union(set1, set1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Sets.difference(set1, set1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Sets.symmetricDifference(set1, set1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Stream.concat(str1, str1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                IntStream.concat(istr1, istr1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Iterables.concat(list1, list1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Math.max(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Math.min(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                StrictMath.max(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Integer.max(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Ints.max(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Maps.difference(map1, map1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                Collections.disjoint(list1, list1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                FieldMaskUtil.union(mask1, mask1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                FieldMaskUtil.intersection(mask1, mask1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                FieldMaskUtil.subtract(mask1, mask1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                assertThat(first).isAnyOf(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                assertThat(first).isNoneOf(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                assertThat(list1).containsAnyOf(first, first);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                anyOf(m1, m1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                staticMethod().onClass("Foo").namedAnyOf(s1, s1);
                // BUG: Diagnostic contains: DistinctVarargsChecker
                staticMethod().onClassAny(s1, s1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_differentVariableInVarargMethods_shouldNotFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;
            import static com.google.errorprone.matchers.Matchers.anyOf;
            import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

            import com.google.common.base.MoreObjects;
            import com.google.common.base.Predicate;
            import com.google.common.base.Predicates;
            import com.google.common.collect.ImmutableBiMap;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedMap;
            import com.google.common.collect.ImmutableSortedSet;
            import com.google.common.collect.Iterables;
            import com.google.common.collect.Maps;
            import com.google.common.collect.Ordering;
            import com.google.common.collect.Sets;
            import com.google.common.primitives.Ints;
            import com.google.errorprone.matchers.Matcher;
            import com.google.protobuf.FieldMask;
            import com.google.protobuf.util.FieldMaskUtil;
            import com.sun.source.tree.ExpressionTree;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.nio.file.StandardCopyOption;
            import java.util.Collections;
            import java.util.EnumSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.Set;
            import java.util.concurrent.CompletableFuture;
            import java.util.stream.IntStream;
            import java.util.stream.Stream;
            import org.mockito.Mockito;

            public class Test {
              enum TestEnum { A, B }

              void testFunction() throws Exception {
                int first = 1, second = 2, third = 3, fourth = 4;
                CompletableFuture f1 = null, f2 = null;
                Predicate p1 = null, p2 = null;
                Path path1 = null, path2 = null;
                Set set1 = null, set2 = null;
                Stream str1 = null, str2 = null;
                IntStream istr1 = null, istr2 = null;
                List list1 = null, list2 = null;
                Map map1 = null, map2 = null;
                FieldMask mask1 = null, mask2 = null;
                Matcher<ExpressionTree> m1 = null, m2 = null;
                String s1 = "a", s2 = "b";

                Ordering.explicit(first);
                Ordering.explicit(first, second);
                Map.of(first, second);
                ImmutableSortedMap.of(first, second);
                ImmutableBiMap.of(first, second, third, fourth);
                Set.of(first, second);
                ImmutableSet.of(first);
                ImmutableSet.of(first, second);
                ImmutableSortedSet.of(first);
                ImmutableSortedSet.of(first, second);
                Objects.hash(first, second);
                MoreObjects.firstNonNull(first, second);
                Objects.requireNonNullElse(first, second);
                com.google.common.base.Objects.hashCode(first, second);
                CompletableFuture.allOf(f1, f2);
                CompletableFuture.anyOf(f1, f2);
                Mockito.verifyNoInteractions(first, second);
                Mockito.verifyNoMoreInteractions(first, second);
                Mockito.inOrder(first, second);
                Mockito.ignoreStubs(first, second);
                assertThat(map1).containsExactly(first, second, third, fourth);
                EnumSet.of(TestEnum.A, TestEnum.B);
                Predicates.and(p1, p2);
                Predicates.or(p1, p2);
                Files.copy(path1, path2);
                Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING);
                Sets.intersection(set1, set2);
                Sets.union(set1, set2);
                Sets.difference(set1, set2);
                Sets.symmetricDifference(set1, set2);
                Stream.concat(str1, str2);
                IntStream.concat(istr1, istr2);
                Iterables.concat(list1, list2);
                Math.max(first, second);
                Math.min(first, second);
                StrictMath.max(first, second);
                Integer.max(first, second);
                Ints.max(first, second);
                Maps.difference(map1, map2);
                Collections.disjoint(list1, list2);
                FieldMaskUtil.union(mask1, mask2);
                FieldMaskUtil.intersection(mask1, mask2);
                FieldMaskUtil.subtract(mask1, mask2);
                assertThat(first).isAnyOf(first, second);
                assertThat(first).isNoneOf(first, second);
                assertThat(list1).containsAnyOf(first, second);
                assertThat(list1).containsAtLeast(first, first);
                anyOf(m1, m2);
                staticMethod().onClass("Foo").namedAnyOf(s1, s2);
                staticMethod().onClassAny(s1, s2);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_sameVariableInImmutableSetVarargsMethod_shouldRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedSet;
            import java.util.Set;

            public class Test {
              void testFunction() {
                int first = 1, second = 2;
                Set.of(first, first);
                ImmutableSet.of(first, first);
                ImmutableSet.of(first, first, second);
                ImmutableSortedSet.of(first, first);
                ImmutableSortedSet.of(first, first, second);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedSet;
            import java.util.Set;

            public class Test {
              void testFunction() {
                int first = 1, second = 2;
                Set.of(first);
                ImmutableSet.of(first);
                ImmutableSet.of(first, second);
                ImmutableSortedSet.of(first);
                ImmutableSortedSet.of(first, second);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_differentVarsInImmutableSetVarargsMethod_shouldNotRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedSet;

            public class Test {
              void testFunction() {
                int first = 1, second = 2;
                ImmutableSet.of(first);
                ImmutableSet.of(first, second);
                ImmutableSortedSet.of(first);
                ImmutableSortedSet.of(first, second);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedSet;

            public class Test {
              void testFunction() {
                int first = 1, second = 2;
                ImmutableSet.of(first);
                ImmutableSet.of(first, second);
                ImmutableSortedSet.of(first);
                ImmutableSortedSet.of(first, second);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_quadratic() {

    String large =
        IntStream.range(0, 7000)
            .mapToObj(x -> String.format("\"%s\"", x))
            .collect(joining(", ", "ImmutableSet.of(", ", \"0\");"));

    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "public class Test {",
            "  void testFunction() {",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            large,
            "  }",
            "}")
        .doTest();
  }
}
