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
import static com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification.CLASS_NAME_WITH_T;
import static com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification.LETTER_WITH_MAYBE_NUMERAL;
import static com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification.NON_CLASS_NAME_WITH_T_SUFFIX;
import static com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification.UNCLASSIFIED;

import com.google.common.truth.Subject;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.TypeParameterNaming.TypeParameterNamingClassification;
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
  public void refactoring_trailing() {
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
  public void refactoring_single() {
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
  public void refactoring_single_number() {
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
  public void refactoring_single_number_enclosing() {
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
  public void refactoring_single_number_within_scope() {
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
  public void refactoring_single_number_many_ok() {
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
  public void refactoring_single_number_ok_after() {
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
  public void refactoring_newNames() {
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
  public void refactoring_TSuffixes() {
    refactoring
        .addInputLines(
            "in/Test.java",
            "class Test<RESP> {",
            "  public <FOOT, BART> void method(FOOT f) {",
            "    BART bad = null;",
            "    FOOT d = f;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test<RespT> {",
            "  public <F, B> void method(F f) {",
            "    B bad = null;",
            "    F d = f;",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void negativeCases() {
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
  public void classifyTypeName_singleLetter() {
    assertKindOfName("T").isEqualTo(LETTER_WITH_MAYBE_NUMERAL);
    assertKindOfName("D").isEqualTo(LETTER_WITH_MAYBE_NUMERAL);
    assertKindOfName("T9").isEqualTo(LETTER_WITH_MAYBE_NUMERAL);
    assertKindOfName("X").isEqualTo(LETTER_WITH_MAYBE_NUMERAL);
  }

  @Test
  public void classifyTypeName_classT() {
    assertKindOfName("FooT").isEqualTo(CLASS_NAME_WITH_T);
    assertKindOfName("FooBarT").isEqualTo(CLASS_NAME_WITH_T);
    assertKindOfName("FoobarT").isEqualTo(CLASS_NAME_WITH_T);
  }

  @Test
  public void classifyTypeName_invalidTypeParameters() {
    assertKindOfName("FooD").isEqualTo(UNCLASSIFIED);
    assertKindOfName("somethingT").isEqualTo(NON_CLASS_NAME_WITH_T_SUFFIX);
    assertKindOfName("Some_TokenT").isEqualTo(NON_CLASS_NAME_WITH_T_SUFFIX);

    // Here, we don't tokenize LOUDT as L_O_U_D_T, but as one token
    assertKindOfName("LOUDT").isEqualTo(NON_CLASS_NAME_WITH_T_SUFFIX);
    // Per Google style guide, acronyms should be lowercase
    assertKindOfName("HTTPHeaderT").isEqualTo(NON_CLASS_NAME_WITH_T_SUFFIX);

    // This is unfortunate, the first 'word' is a single character, but falls into the same bin
    // as above.
    assertKindOfName("ACanalPanamaT").isEqualTo(NON_CLASS_NAME_WITH_T_SUFFIX);
  }

  private static Subject<?, TypeParameterNamingClassification> assertKindOfName(String s) {
    return assertThat(TypeParameterNamingClassification.classify(s)).named(s);
  }
}
