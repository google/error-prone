/*
 * Copyright 2017 The Error Prone Authors.
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

/** {@link PredicateIncompatibleType}Test */
@RunWith(JUnit4.class)
public class PredicateIncompatibleTypeTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(PredicateIncompatibleType.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<Integer> f(List<Integer> lx) {",
            "    // BUG: Diagnostic contains: types String and Integer are incompatible",
            "    return lx.stream().filter(\"\"::equals);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<Integer> f(List<Integer> lx) {",
            "    return lx.stream().filter(Integer.valueOf(42)::equals);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeHierarchy() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "import java.util.LinkedList;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<ArrayList<String>> f(List<ArrayList<String>> a, LinkedList<String> b) {",
            "    return a.stream().filter(b::equals);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_guavaPredicate() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Predicate;",
            "class Test {",
            "  void f(Integer x) {",
            "    // BUG: Diagnostic contains: types Integer and String are incompatible",
            "    Predicate<String> p = x::equals;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notAPredicate() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.function.BiFunction;",
            "class Test {",
            "  void f() {",
            "    BiFunction<Object, Object, Boolean> f = Object::equals;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  Optional<String> f(Optional<String> s) {",
            "    // BUG: Diagnostic contains: types String and Integer are incompatible",
            "    return s.filter(Integer.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOf2() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "class Test {",
            "  Optional<HashMap<String,Integer>> f(Optional<HashMap<String,Integer>> m) {",
            "    // BUG: Diagnostic contains: Predicate will always evaluate to false",
            "    return m.filter(Integer.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOfWithGenerics() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.lang.Number;",
            "class Test {",
            "  <T extends Number> Optional<T> f(Optional<T> t) {",
            "    // BUG: Diagnostic contains: types Number and String are incompatible",
            "    return t.filter(String.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "import java.util.LinkedHashMap;",
            "class Test {",
            "  Optional<HashMap> f(Optional<HashMap> m) {",
            "    return m.filter(LinkedHashMap.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOf2() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "import java.util.LinkedHashMap;",
            "class Test {",
            "  Optional<HashMap<String, Integer>> f(Optional<HashMap<String,Integer>> m) {",
            "    return m.filter(LinkedHashMap.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOfWithGenerics() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  <T> Optional<T> f(Optional<T> t) {",
            "    return t.filter(Object.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  /* String.class::isInstance is used as a Function<T, Boolean>, not a Predicate<T>. */
  @Test
  public void methodReferenceNotPredicate() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  <T> Optional<Boolean> f(Optional<T> t) {",
            "    return t.map(String.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }
}
