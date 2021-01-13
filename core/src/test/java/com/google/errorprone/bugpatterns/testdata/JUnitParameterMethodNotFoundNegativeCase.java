/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Negative cases for {@link com.google.errorprone.bugpatterns.JUnitParameterMethodNotFound} */
@RunWith(JUnitParamsRunner.class)
public class JUnitParameterMethodNotFoundNegativeCase {

  private static final String METHOD = "named1";

  private static Object[] dataProvider() {
    return new Object[] {1};
  }

  private static Object[] dataProvider1() {
    return new Object[] {2};
  }

  @Test
  @Parameters(method = "dataProvider, dataProvider1")
  public void paramStaticProvider(int a) {}

  @Test
  @Parameters(source = Inner.class, method = "dataProviderInner")
  public void testSource(int a) {}

  @Test
  @Parameters({"AAA,1", "BBB,2"})
  public void paramsInAnnotation(String p1, Integer p2) {}

  @Test
  @Parameters({"AAA|1", "BBB|2"})
  public void paramsInAnnotationPipeSeparated(String p1, Integer p2) {}

  @Test
  @Parameters
  public void paramsInDefaultMethod(String p1, Integer p2) {}

  private Object parametersForParamsInDefaultMethod() {
    return new Object[] {new Object[] {"AAA", 1}, new Object[] {"BBB", 2}};
  }

  @Test
  @Parameters(method = METHOD)
  public void paramsInNamedMethod(String p1, Integer p2) {}

  private Object named1() {
    return new Object[] {"AAA", 1};
  }

  @Test
  @Parameters(method = "named2,named3")
  public void paramsInMultipleMethods(String p1, Integer p2) {}

  private Object named2() {
    return new Object[] {"AAA", 1};
  }

  private Object named3() {
    return new Object[] {"BBB", 2};
  }

  @Test
  @Parameters
  public void paramsInCollection(String p1) {}

  private List<String> parametersForParamsInCollection() {
    return Arrays.asList("a");
  }

  @Test
  @Parameters
  public void paramsInIterator(String p1) {}

  private Iterator<String> parametersForParamsInIterator() {
    return Arrays.asList("a").iterator();
  }

  @Test
  @Parameters
  public void paramsInIterableOfIterables(String p1, String p2) {}

  private List<List<String>> parametersForParamsInIterableOfIterables() {
    return Arrays.asList(Arrays.asList("s01e01", "s01e02"), Arrays.asList("s02e01", "s02e02"));
  }

  @Test
  @Parameters(
      "please\\, escape commas if you use it here and don't want your parameters to be splitted")
  public void commasInParametersUsage(String phrase) {}

  @Test
  @Parameters({"1,1", "2,2", "3,6"})
  @TestCaseName("factorial({0}) = {1}")
  public void customNamesForTestCase(int argument, int result) {}

  @Test
  @Parameters({"value1, value2", "value3, value4"})
  @TestCaseName("[{index}] {method}: {params}")
  public void predefinedMacroForTestCaseNames(String param1, String param2) {}

  public Object mixedParameters() {
    boolean booleanValue = true;
    int[] primitiveArray = {1, 2, 3};
    String stringValue = "Test";
    String[] stringArray = {"one", "two", null};
    return new Object[] {new Object[] {booleanValue, primitiveArray, stringValue, stringArray}};
  }

  @Test
  @Parameters(method = "mixedParameters")
  @TestCaseName("{0}, {1}, {2}, {3}")
  public void usageOfMultipleTypesOfParameters(
      boolean booleanValue, int[] primitiveArray, String stringValue, String[] stringArray) {}

  static class Inner {
    public Object dataProviderInner() {
      return new Object[] {1};
    }
  }
}
