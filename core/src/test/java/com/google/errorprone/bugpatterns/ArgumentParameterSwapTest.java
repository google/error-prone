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
  public void calculateSimilarityMatrix() throws Exception {
    String[] args = {"foo", "fooBarFoo", "fooBar"};
    String[] params = {"foo", "bar", "barFoo"};
    double[][] simMatrix = ArgumentParameterSwap.calculateSimilarityMatrix(args, params);

    assertThat(simMatrix[0]).hasValuesWithin(0.001).of(new double[] {1.0, 0.0, 0.6666});
    assertThat(simMatrix[1]).hasValuesWithin(0.001).of(new double[] {0.6666, 0.6666, 1.0});
    assertThat(simMatrix[2]).hasValuesWithin(0.001).of(new double[] {0.6666, 0.6666, 1.0});
  }

  @Test
  public void calculateSimilarityMatrix2() throws Exception {
    String[] args = {"extraPath", "keepPath", "dropPath"};
    String[] params = {"keepPath", "dropPath", "extraPath"};
    double[][] simMatrix = ArgumentParameterSwap.calculateSimilarityMatrix(args, params);

    assertThat(simMatrix[0]).hasValuesWithin(0.001).of(new double[] {0.5, 0.5, 1.0});
    assertThat(simMatrix[1]).hasValuesWithin(0.001).of(new double[] {1.0, 0.5, 0.5});
    assertThat(simMatrix[2]).hasValuesWithin(0.001).of(new double[] {0.5, 1.0, 0.5});
  }

  @Test
  public void splitStringTermsToSet() throws Exception {
    Set<String> terms = ArgumentParameterSwap.splitStringTerms("foo");
    assertThat(terms).containsExactly("foo");

    terms = ArgumentParameterSwap.splitStringTerms("fooBar");
    assertThat(terms).containsExactly("foo", "bar");

    terms = ArgumentParameterSwap.splitStringTerms("fooBarFoo");
    assertThat(terms).containsExactly("foo", "bar");

    terms = ArgumentParameterSwap.splitStringTerms("fooBarID");
    assertThat(terms).containsExactly("foo", "bar", "i", "d");
  }

  @Test
  public void ignoreDisallowedParams() {
    compilationHelper
        .addSourceLines(
            "SwapNotExact.java",
            "class SwapNotExact {",
            "  public void doIt(String message, String value) {}",
            "  public void testMethod(String messageThing, String valueThing) {",
            "    doIt(valueThing, messageThing);",
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
}
