/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
            "    // BUG: Diagnostic contains: Using String::equals as Predicate<Integer>;",
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
            "    return a.stream().filter(a::equals);",
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
            "    // BUG: Diagnostic contains: Using Integer::equals as Predicate<String>;",
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
}
