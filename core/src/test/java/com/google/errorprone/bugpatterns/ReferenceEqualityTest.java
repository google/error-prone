/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ReferenceEquality}Test */
@RunWith(JUnit4.class)
public class ReferenceEqualityTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ReferenceEquality.class, getClass());
  }

  @Test
  public void negative_const() throws Exception {
    compilationHelper
        .addSourceLines("Foo.java", "class Foo {", "}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  public static final Foo CONST = new Foo();",
            "  boolean f(Foo a) {",
            "    return a == CONST;",
            "  }",
            "  boolean f(Object o, Foo a) {",
            "    return o == a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_extends_equalsObject() throws Exception {
    compilationHelper
        .addSourceLines(
            "Sup.java", "class Sup {", "  public boolean equals(Object o) { return false; }", "}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test extends Sup {",
            "  boolean f(Object a, Test b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_extendsAbstract_equals() throws Exception {
    compilationHelper
        .addSourceLines(
            "Sup.java", "abstract class Sup { public abstract boolean equals(Object o); }")
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "abstract class Test extends Sup {",
            "  boolean f(Test a, Test b) {",
            "    // BUG: Diagnostic contains: a.equals(b)",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_implementsInterface_equals() throws Exception {
    compilationHelper
        .addSourceLines("Sup.java", "interface Sup { public boolean equals(Object o); }")
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test implements Sup {",
            "  boolean f(Test a, Test b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_noEquals() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Test a, Test b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_equal() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    // BUG: Diagnostic contains: a.equals(b)",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_notEqual() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    // BUG: Diagnostic contains: !a.equals(b)",
            "    return a != b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_impl() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public boolean equals(Object o) {",
            "    return this == o;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_enum() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.element.ElementKind;",
            "class Test {",
            "  boolean f(ElementKind a, ElementKind b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void test_customEnum() throws Exception {
    compilationHelper
        .addSourceLines(
            "Kind.java",
            "enum Kind {",
            "  FOO(42);",
            "  private final int x;",
            "  Kind(int x) { this.x = x; }",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(Kind a, Kind b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_null() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> b) {",
            "    return b == null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_abstractEq() throws Exception {
    compilationHelper
        .addSourceLines(
            "Sup.java", "interface Sup {", "  public abstract boolean equals(Object o);", "}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test implements Sup {",
            "  boolean f(Object a, Test b) {",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCase_class() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  boolean f(String s) {",
            "    return s.getClass() == String.class;",
            "  }",
            "}")
        .doTest();
  }
}
