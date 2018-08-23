/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.common.io.ByteStreams;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.junit.Ignore;

/** {@link ReferenceEquality}Test */
@RunWith(JUnit4.class)
public class ReferenceEqualityTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReferenceEquality.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ReferenceEquality(), getClass());

  @Ignore("b/74365407 test proto sources are broken")
  @Test
  public void protoGetter_nonnull() {
    compilationHelper
        .addSourceLines(
            "in/Foo.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Foo {",
            "  void something(TestProtoMessage f1, TestProtoMessage f2) {",
            "     // BUG: Diagnostic contains: boolean b = Objects.equals(f1, f2);",
            "     boolean b = f1 == f2;",
            "     // BUG: Diagnostic contains: b = f1.getMessage().equals(f2.getMessage())",
            "     b = f1.getMessage() == f2.getMessage();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_const() {
    compilationHelper
        .addSourceLines(
            "Foo.java", //
            "class Foo {",
            "}")
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
  public void negative_extends_equalsObject() {
    compilationHelper
        .addSourceLines(
            "Sup.java", //
            "class Sup {",
            "  public boolean equals(Object o) {",
            "    return false; ",
            "  }",
            "}")
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
  public void positive_extendsAbstract_equals() {
    compilationHelper
        .addSourceLines(
            "Sup.java", //
            "abstract class Sup { ",
            "  public abstract boolean equals(Object o); ",
            "}")
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
  public void negative_implementsInterface_equals() {
    compilationHelper
        .addSourceLines(
            "Sup.java", //
            "interface Sup {",
            "  public boolean equals(Object o); ",
            "}")
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
  public void negative_noEquals() {
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
  public void positive_equal() {
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
  public void positive_equalWithOr() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    return a == b || (a.equals(b));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    return a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_equalWithOr_objectsEquals() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.base.Optional;",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    boolean eq = a == b || Objects.equal(a, b);",
            "    return a == b || (java.util.Objects.equals(a, b));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Optional;",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  boolean f(Optional<Integer> a, Optional<Integer> b) {",
            "    boolean eq = Objects.equal(a, b);",
            "    return java.util.Objects.equals(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_notEqual() {
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
  public void negative_impl() {
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
  public void negative_enum() {
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
  public void test_customEnum() {
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
  public void negative_null() {
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
  public void negative_abstractEq() {
    compilationHelper
        .addSourceLines(
            "Sup.java", //
            "interface Sup {",
            "  public abstract boolean equals(Object o);",
            "}")
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
  public void negativeCase_class() {
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

  @Test
  public void transitiveEquals() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "public class Super {",
            "  public boolean equals(Object o) {",
            "    return false;",
            "  }",
            "}")
        .addSourceLines("Mid.java", "public class Mid extends Super {", "}")
        .addSourceLines("Sub.java", "public class Sub extends Mid {", "}")
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  boolean f(Sub a, Sub b) {",
            "    // BUG: Diagnostic contains: a.equals(b)",
            "    return a == b;",
            "  }",
            "}")
        .doTest();
  }

  public static class Missing {}

  public static class MayImplementEquals {

    public void f(Missing m) {}

    public void g(Missing m) {}
  }

  @Test
  public void testErroneous() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, MayImplementEquals.class);
      addClassToJar(jos, ReferenceEqualityTest.class);
    }
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import " + MayImplementEquals.class.getCanonicalName() + ";",
            "abstract class Test {",
            "  abstract MayImplementEquals getter();",
            "  boolean f(MayImplementEquals b) {",
            "    return getter() == b;",
            "  }",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  // regression test for #423
  @Test
  public void typaram() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test<T extends String, X> {",
            "  boolean f(T t) {",
            "    return t == null;",
            "  }",
            "  boolean g(T t1, T t2) {",
            "    // BUG: Diagnostic contains:",
            "    return t1 == t2;",
            "  }",
            "  boolean g(X x1, X x2) {",
            "    return x1 == x2;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_compareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test implements Comparable<Test> {",
            "  public int compareTo(Test o) {",
            "    return this == o ? 0 : -1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_compareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test implements Comparable<String> {",
            "  String f;",
            "  public int compareTo(String o) {",
            "    // BUG: Diagnostic contains:",
            "    return f == o ? 0 : -1;",
            "  }",
            "}")
        .doTest();
  }
}
