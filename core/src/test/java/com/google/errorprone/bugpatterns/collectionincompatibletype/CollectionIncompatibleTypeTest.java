/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class CollectionIncompatibleTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CollectionIncompatibleType.class, getClass());

  private final BugCheckerRefactoringTestHelper refactorTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(CollectionIncompatibleType.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeNegativeCases.java").doTest();
  }

  @Test
  public void testOutOfBounds() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeOutOfBounds.java").doTest();
  }

  @Test
  public void testClassCast() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeClassCast.java").doTest();
  }

  @Test
  public void testCastFixes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    // BUG: Diagnostic contains: c1.contains((Object) 1);",
            "    c1.contains(1);",
            "    // BUG: Diagnostic contains: c1.containsAll((Collection<?>) c2);",
            "    c1.containsAll(c2);",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:CollectionIncompatibleType:FixType=CAST"))
        .doTest();
  }

  @Test
  public void testSuppressWarningsFix() {
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    c1.contains(1);",
            "    c1.containsAll(c2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  @SuppressWarnings(\"CollectionIncompatibleType\")",
            // In this test environment, the fix doesn't include formatting
            "public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    c1.contains(/* expected: String, actual: int */ 1);",
            "    c1.containsAll(/* expected: String, actual: Integer */ c2);",
            "  }",
            "}")
        .setArgs("-XepOpt:CollectionIncompatibleType:FixType=SUPPRESS_WARNINGS")
        .doTest(TestMode.TEXT_MATCH);
  }

  // This test is disabled because calling Types#asSuper in the check removes the upper bound on K.
  @Test
  @Ignore
  public void testBoundedTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.HashMap;",
            "public class Test {",
            "  private static class MyHashMap<K extends Integer, V extends String>",
            "      extends HashMap<K, V> {}",
            "  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {",
            "    // BUG: Diagnostic contains:",
            "    return myHashMap.containsKey(\"bad\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void disjoint() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "public class Test {",
            "  void f(List<String> a, List<String> b) {",
            "    Collections.disjoint(a, b);",
            "  }",
            "  void g(List<String> a, List<Integer> b) {",
            "    // BUG: Diagnostic contains: not compatible",
            "    Collections.disjoint(a, b);",
            "  }",
            "  void h(List<?> a, List<Integer> b) {",
            "    Collections.disjoint(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void difference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Set;",
            "import com.google.common.collect.Sets;",
            "public class Test {",
            "  void f(Set<String> a, Set<String> b) {",
            "    Sets.difference(a, b);",
            "  }",
            "  void g(Set<String> a, Set<Integer> b) {",
            "    // BUG: Diagnostic contains: not compatible",
            "    Sets.difference(a, b);",
            "  }",
            "  void h(Set<?> a, Set<Integer> b) {",
            "    Sets.difference(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  java.util.stream.Stream filter(List<Integer> xs, List<String> ss) {",
            "    // BUG: Diagnostic contains:",
            "    return xs.stream().filter(ss::contains);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReferenceBinOp() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  void removeAll(List<List<Integer>> xs, List<String> ss) {",
            "    // BUG: Diagnostic contains:",
            "    xs.forEach(ss::removeAll);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReference_compatibleType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  java.util.stream.Stream filter(List<Integer> xs, List<Object> ss) {",
            "    return xs.stream().filter(ss::contains);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memberReferenceWithBoundedGenerics() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.function.BiFunction;",
            "import java.util.Set;",
            "public class Test {",
            "  <T extends String, M extends Integer> void a(",
            "    BiFunction<Set<T>, Set<M>, Set<T>> b) {}",
            "  void b() {",
            "    // BUG: Diagnostic contains:",
            "    a(Sets::difference);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memberReferenceWithBoundedGenericsDependentOnEachOther() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.function.BiFunction;",
            "import java.util.Set;",
            "public class Test {",
            "  <T extends String, M extends T> void a(",
            "    BiFunction<Set<T>, Set<M>, Set<T>> b) {}",
            "  void b() {",
            "    a(Sets::difference);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memberReferenceWithConcreteIncompatibleTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.function.BiFunction;",
            "import java.util.Set;",
            "public class Test {",
            "  void a(BiFunction<Set<Integer>, Set<String>, Set<Integer>> b) {}",
            "  void b() {",
            "    // BUG: Diagnostic contains:",
            "    a(Sets::difference);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memberReferenceWithConcreteCompatibleTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.function.BiFunction;",
            "import java.util.Set;",
            "public class Test {",
            "  void a(BiFunction<Set<Integer>, Set<Number>, Set<Integer>> b) {}",
            "  void b() {",
            "    a(Sets::difference);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void memberReferenceWithCustomFunctionalInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.function.BiFunction;",
            "import java.util.Set;",
            "public interface Test {",
            "  Set<Integer> test(Set<Integer> a, Set<String> b);",
            "  static void a(Test b) {}",
            "  static void b() {",
            "    // BUG: Diagnostic contains: Integer is not compatible with String",
            "    a(Sets::difference);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wildcardBoundedCollectionTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.Set;",
            "public interface Test {",
            "  static void test(Set<? extends List<Integer>> xs, Set<? extends Set<Integer>> ys) {",
            "    // BUG: Diagnostic contains:",
            "    xs.containsAll(ys);",
            "  }",
            "}")
        .doTest();
  }
}
