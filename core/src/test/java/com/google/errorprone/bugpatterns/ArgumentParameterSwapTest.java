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
  public void equalSimilarityMatchLeavesOriginalInPlace() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String to, String barString) {}",
            "  public void testMethod(String stringOne, String stringFoo) {",
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
  public void shouldSwapFirstWithThird() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooBarBazString, ",
            "                   String quxQuuCorString, ",
            "                   String graGarWalString) {}",
            "  public void testMethod(String fooBarBazThing, ",
            "                         String quxQuuCorThing, ",
            "                         String graGarWalThing) {",
            " // BUG: Diagnostic contains: 'doIt(fooBarBazThing, quxQuuCorThing, graGarWalThing);'",
            "    doIt(graGarWalThing, quxQuuCorThing, fooBarBazThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void shouldSwapNearlyMatchingArguments() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooBarBazString, String quxQuuCorString) {}",
            "  public void testMethod(String quxQuuCorThing, String fooBarBazThing) {",
            "    // BUG: Diagnostic contains: 'doIt(fooBarBazThing, quxQuuCorThing);'",
            "    doIt(quxQuuCorThing, fooBarBazThing);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void shouldSwapNearlyMatchingConstants() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public final static String FOO_BAR_BAZ_THING = \"foo\";",
            "  public final static String QUX_QUU_COR_THING = \"bar\";",
            "  public void doIt(String fooBarBazString, String quxQuuCorString) {}",
            "  public void testMethod() {",
            "    // BUG: Diagnostic contains: 'doIt(FOO_BAR_BAZ_THING, QUX_QUU_COR_THING);'",
            "    doIt(QUX_QUU_COR_THING, FOO_BAR_BAZ_THING);",
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
  public void shouldNotSwapLiteralArgument() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String fooBarBazString,",
            "                   String quxQuuCorString,",
            "                   String forBarBazThing) {}",
            "  public void testMethod(String quxQuuCorThing, String fooBarBazThing) {",
            "    // BUG: Diagnostic contains: 'doIt(fooBarBazThing, quxQuuCorThing, \"hello\");'",
            "    doIt(quxQuuCorThing, fooBarBazThing, \"hello\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctlySwapsFieldParameters() {
    compilationHelper
        .addSourceLines(
            "FieldUsage.java",
            "class FieldUsage {",
            "  public String fooBarBaz;",
            "  public String quxQuuCor;",
            "  public void doIt(String fooBarBazString, String quxQuuCorString) {}",
            "  public void testMethod(FieldUsage usage) {",
            "    // BUG: Diagnostic contains: 'doIt(usage.fooBarBaz, usage.quxQuuCor);'",
            "    doIt(usage.quxQuuCor, usage.fooBarBaz);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctlySwapsMethodCallParameters() {
    compilationHelper
        .addSourceLines(
            "MethodCall.java",
            "class MethodCall {",
            "  public String getFooBarBaz() {return \"\";}",
            "  public String getQuxQuuCor() {return \"\";}",
            "  public void doIt(String fooBarBazString, String quxQuuCorString) {}",
            "  public void testMethod(MethodCall usage) {",
            "    // BUG: Diagnostic contains: 'doIt(usage.getFooBarBaz(), usage.getQuxQuuCor());'",
            "    doIt(usage.getQuxQuuCor(), usage.getFooBarBaz());",
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
            "    test(quxQuuCor, this);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void shouldNotSwapBecauseCandidateNotSignificantlyBetter() {
    compilationHelper
        .addSourceLines(
            "DontSwap.java",
            "class DoneSwap {",
            "  public void doIt(String fooString,String barString) {}",
            "  public void testMethod(String fooNotBest, String fooBetter) {",
            "    doIt(fooNotBest,fooBetter);",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for b/32477667
  @Test
  public void shouldNotCrashWithVarargs() {
    compilationHelper
        .addSourceLines(
            "DontCrash.java",
            "class DontCrash {",
            "  public void doIt(String... fooStrings) {}",
            "  public void testMethod(String fooNotBest, String fooBetter) {",
            "    doIt();",
            "  }",
            "}")
        .doTest();
  }

  // Regression test for GitHub issue #453
  @Test
  public void shouldNotCrashOnEnum() {
    compilationHelper
        .addSourceLines("Enum.java", "enum Enum {", "  C(0);", "  Enum(final int i) {}", "}")
        .doTest();
  }

  // Regression test for #490
  @Test
  public void i490() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  Test(ImmutableMap<?, ?> map, Optional<?> optional) {}",
            "  Test g() {",
            "    return new Test(ImmutableMap.of(), Optional.empty());",
            "  }",
            "}")
        .doTest();
  }
}
