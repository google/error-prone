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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.CompilationTestHelper;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author yulissa@google.com (Yulissa Arroyo-Paredes) */
@RunWith(JUnit4.class)
public class ArgumentParameterSwapTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ArgumentParameterSwap.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("ArgumentParameterSwapPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("ArgumentParameterSwapNegativeCases.java").doTest();
  }

  @Test
  public void findBestMatch_original() {
    assertThat(ArgumentParameterSwap.findBestMatch(new String[] {"baz", "other"}, "foo", "fooBar"))
        .isEqualTo(-1);
  }

  @Test
  public void findBestMatch_first() {
    assertThat(ArgumentParameterSwap.findBestMatch(new String[] {"bar", "other"}, "baz", "fooBar"))
        .isEqualTo(0);
  }

  @Test
  public void findBestMatch_last() {
    assertThat(ArgumentParameterSwap.findBestMatch(new String[] {"baz", "foo"}, "baz", "fooBar"))
        .isEqualTo(1);
  }

  @Test
  public void findBestMatch_same() {
    assertThat(ArgumentParameterSwap.findBestMatch(new String[] {"bar", "other"}, "foo", "fooBar"))
        .isEqualTo(-1);
  }

  @Test
  public void calculateSimilarity_sameBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSwap.calculateSimilarity("foo", "foo"))
        .isGreaterThan(ArgumentParameterSwap.calculateSimilarity("foo", "bar"));
  }

  @Test
  public void calculateSimilarity_partialSmallerBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSwap.calculateSimilarity("foo", "barFoo"))
        .isGreaterThan(ArgumentParameterSwap.calculateSimilarity("foo", "bar"));
  }

  @Test
  public void calculateSimilarity_partialLargerBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSwap.calculateSimilarity("fooBar", "bar"))
        .isGreaterThan(ArgumentParameterSwap.calculateSimilarity("foo", "bar"));
  }

  @Test
  public void calculateSimilarity_partialRepeatedBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSwap.calculateSimilarity("fooBarFoo", "foo"))
        .isGreaterThan(ArgumentParameterSwap.calculateSimilarity("foo", "bar"));
  }

  @Test
  public void calculateSimilarity_partialSameBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSwap.calculateSimilarity("fooBar", "barBaz"))
        .isGreaterThan(ArgumentParameterSwap.calculateSimilarity("foo", "bar"));
  }

  @Test
  public void splitStringTerms_lower() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("foo");
    assertThat(terms).containsExactly("foo");
  }

  @Test
  public void splitStringTerms_camel() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("fooBar");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_upper() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("FOO_BAR");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_repeated() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("fooBarFoo");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_single() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("fooBarID");
    assertThat(terms).containsExactly("foo", "bar", "id");
  }

  @Test
  public void splitStringTerms_mixed() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("foo_barBaz");
    assertThat(terms).containsExactly("foo", "bar", "baz");
  }

  @Test
  public void ignoreDisallowedParams() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String index, String value) {}",
            "  public void testMethod(String indexThing, String valueThing) {",
            "    doIt(valueThing, indexThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreSmallParams() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String to, String from) {}",
            "  public void testMethod(String toThing, String fromThing) {",
            "    doIt(fromThing, toThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void equalMatchStays() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod(String stringOne, String stringFoo) {",
            "    // BUG: Diagnostic contains: 'doIt(stringFoo, stringFoo);'",
            "    doIt(stringOne, stringFoo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Ignore
  // TODO(ciera): Add support for this test
  public void betterLocalVariable() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod(String stringOne, String stringFoo) {",
            "    String stringBar = \"\";",
            "    // BUG: Diagnostic contains: 'doIt(stringFoo, stringBar);'",
            "    doIt(stringOne, stringFoo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void twoSwapOneInPlace() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooString, String bazString, String barString) {}",
            "  public void testMethod(String barThing, String bazThing, String fooThing) {",
            "    // BUG: Diagnostic contains: 'doIt(fooThing, bazThing, barThing);'",
            "    doIt(barThing, bazThing, fooThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void swappedNotExact() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod(String barThing, String fooThing) {",
            "    // BUG: Diagnostic contains: 'doIt(fooThing, barThing);'",
            "    doIt(barThing, fooThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void swappedNotExactConstants() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public final static String FOO = \"foo\";",
            "  public final static String BAR_THING = \"bar\";",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod() {",
            "    // BUG: Diagnostic contains: 'doIt(FOO, BAR_THING);'",
            "    doIt(BAR_THING, FOO);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void incompatibleTypes() {
    compilationHelper
        .addSourceLines(
            "IncompatibleParamterizedTypes.java",
            "class IncompatibleParameterizedTypes {",
            "  public void doIt(String foo, Integer bar) {}",
            "  public void testMethod(String barThing, Integer fooThing) {",
            "    doIt(barThing, fooThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void incompatibleParameterizedTypes() {
    compilationHelper
        .addSourceLines(
            "IncompatibleParamterizedTypes.java",
            "import java.util.List;",
            "class IncompatibleParameterizedTypes {",
            "  public void doIt(List<String> foo, List<Integer> bar) {}",
            "  public void testMethod(List<String> barList, List<Integer> fooList) {",
            "    doIt(barList, fooList);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Ignore
  // TODO(ciera): Add support for this test
  public void parameterizedTypes() {
    compilationHelper
        .addSourceLines(
            "ParamterizedTypes.java",
            "import java.util.List;",
            "class ParameterizedTypes {",
            "  public <T> void doIt(List<T> foo, List<T> bar) {}",
            "  public void testMethod(List<String> barList, List<String> fooList) {",
            "    // BUG: Diagnostic contains: 'doIt(fooList, barList);'",
            "    doIt(barList, fooList);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notSimpleArgument() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooString, String barString, String bazString) {}",
            "  public void testMethod(String barThing, String fooThing) {",
            "    // BUG: Diagnostic contains: 'doIt(fooThing, barThing, \"hello\");'",
            "    doIt(barThing, fooThing, \"hello\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldUsage() {
    compilationHelper
        .addSourceLines(
            "FieldUsage.java",
            "class FieldUsage {",
            "  public String foo;",
            "  public String bar;",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod(FieldUsage usage) {",
            "    // BUG: Diagnostic contains: 'doIt(usage.foo, usage.bar);'",
            "    doIt(usage.bar, usage.foo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodCall() {
    compilationHelper
        .addSourceLines(
            "MethodCall.java",
            "class MethodCall {",
            "  public String getFoo() {return \"\";}",
            "  public String getBar() {return \"\";}",
            "  public void doIt(String fooString, String barString) {}",
            "  public void testMethod(MethodCall usage) {",
            "    // BUG: Diagnostic contains: 'doIt(usage.getFoo(), usage.getBar());'",
            "    doIt(usage.getBar(), usage.getFoo());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void substitutesClassNameForThis() {
    compilationHelper
        .addSourceLines(
            "FooBarBaz.java",
            "class FooBarBaz {",
            "  public void test(Object fooBarBaz,Object quxQuuCor) {}",
            "  public void testMethod(Object quxQuuCor) {",
            "    // BUG: Diagnostic contains: 'test(this, quxQuuCor);'",
            "    test(quxQuuCor,this);",
            "  }",
            "}")
        .doTest();
  }
}
