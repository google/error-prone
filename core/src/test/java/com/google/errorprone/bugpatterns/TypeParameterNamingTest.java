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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.BooleanSubject;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TypeParameterNaming} */
@RunWith(JUnit4.class)
public class TypeParameterNamingTest {

  private CompilationTestHelper compilationHelper;
  private BugCheckerRefactoringTestHelper refactoring;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(TypeParameterNaming.class, getClass());
    refactoring =
        BugCheckerRefactoringTestHelper.newInstance(new TypeParameterNaming(), getClass());
  }

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains: TypeParameterNaming",
            "class Test<BadName> {",
            "  // BUG: Diagnostic contains: TypeParameterNaming",
            "  public <T, Foo> void method(Exception e) {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_trailing() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<BadName> {",
            "  public <T, Foo> void method(Foo f) {",
            "    BadName bad = null;",
            "    Foo d = f;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<BadNameT> {",
            "  public <T, FooT> void method(FooT f) {",
            "    BadNameT bad = null;",
            "    FooT d = f;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void refactoring_single() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<BadName> {",
            "  public <T, Foo> void method(Foo f) {",
            "    BadName bad = null;",
            "    Foo d = f;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<B> {",
            "  public <T, F> void method(F f) {",
            "    B bad = null;",
            "    F d = f;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_single_number() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<Bar> {",
            "  public <T, Baz> void method(Baz f) {",
            "    Bar bad = null;",
            "    Baz d = f;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<B> {",
            "  public <T, B2> void method(B2 f) {",
            "    B bad = null;",
            "    B2 d = f;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_single_number_enclosing() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<Bar> {",
            "  public <T, Baz, Boo> void method(Baz f) {",
            "    Bar bad = null;",
            "    Baz d = f;",
            "    Boo wow = null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<B> {",
            "  public <T, B2, B3> void method(B2 f) {",
            "    B bad = null;",
            "    B2 d = f;",
            "    B3 wow = null;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_single_number_within_scope() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  public <T, Baz, Boo> void method(Baz f) {",
            "    Baz d = f;",
            "    Boo wow = null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  public <T, B, B2> void method(B f) {",
            "    B d = f;",
            "    B2 wow = null;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_single_number_many_ok() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  public <B, B2, B3, B4, Bad> void method(Bad f) {",
            "    Bad d = f;",
            "    B2 wow = null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  public <B, B2, B3, B4, B5> void method(B5 f) {",
            "    B5 d = f;",
            "    B2 wow = null;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_single_number_ok_after() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  public <B, Bad, B2> void method(Bad f) {",
            "    Bad d = f;",
            "    B2 wow = null;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  public <B, B3, B2> void method(B3 f) {",
            "    B3 d = f;",
            "    B2 wow = null;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoring_newNames() throws Exception {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<RESP> {",
            "  public <TBaz, Foo> void method(Foo f) {",
            "    TBaz bad = null;",
            "    Foo d = f;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<RespT> {",
            "  public <BazT, FooT> void method(FooT f) {",
            "    BazT bad = null;",
            "    FooT d = f;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void negativeCases() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "class Test<MyClassVarT> {",
            "  public <T, T3, SomeOtherT> void method(Exception e) {",
            "    ArrayList<String> dontCheckTypeArguments = new ArrayList<String>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void validate_className_positive() {
    assertNameIsClassNameWithT("T").isTrue();
    assertNameIsClassNameWithT("FooT").isTrue();
    assertNameIsClassNameWithT("FooBarT").isTrue();
    assertNameIsClassNameWithT("FoobarT").isTrue();
  }

  @Test
  public void validate_className_negative() {
    assertNameIsClassNameWithT("D").isFalse();
    assertNameIsClassNameWithT("FooD").isFalse();
    assertNameIsClassNameWithT("somethingT").isFalse();
    assertNameIsClassNameWithT("Some_TokenT").isFalse();

    // Here, we don't tokenize LOUDT as L_O_U_D_T, but as one token
    assertNameIsClassNameWithT("LOUDT").isFalse();
    // Per google style guide, acronymns should be lowercase
    assertNameIsClassNameWithT("HTTPHeaderT").isFalse();

    // This is unfortunate, the first 'word' is a single character. Seems unlikely to be a
    // problem though.
    assertNameIsClassNameWithT("ACanalPanamaT").isFalse();
  }

  private static BooleanSubject assertNameIsClassNameWithT(String s) {
    return assertThat(TypeParameterNaming.matchesClassWithT(s)).named(s);
  }
}
