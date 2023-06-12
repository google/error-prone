/*
 * Copyright 2022 The Error Prone Authors.
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

/** Tests for {@link AvoidObjectArrays}. */
@RunWith(JUnit4.class)
public class AvoidObjectArraysTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AvoidObjectArrays.class, getClass());

  @Test
  public void methodParam_instanceMethods() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: consider an Iterable<Object> instead",
            "  public void objectArray(Object[] objectArray) {",
            "  }",
            "  // BUG: Diagnostic contains: consider an Iterable<String> instead",
            "  public void stringArray(String[] stringArray) {",
            "  }",
            "  public void intArray(int[] intArray) {",
            "  }",
            "  public void objectValue(Object objectValue) {",
            "  }",
            "  public void stringValue(String stringValue) {",
            "  }",
            "  public void intValue(int intValue) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodParam_staticMethods() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: consider an Iterable<Object> instead",
            "  public static void objectArray(Object[] objectArray) {",
            "  }",
            "  // BUG: Diagnostic contains: consider an Iterable<String> instead",
            "  public static void stringArray(String[] stringArray) {",
            "  }",
            "  public static void intArray(int[] intArray) {",
            "  }",
            "  public static void objectValue(Object objectValue) {",
            "  }",
            "  public static void stringValue(String stringValue) {",
            "  }",
            "  public static void intValue(int intValue) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodParam_instanceMethods_withIterableOverload() {
    compilationHelper
        .addSourceLines(
            "IterableSubject.java",
            "public class IterableSubject {",
            "  public final void containsAnyIn(Iterable<?> expected) {",
            "  }",
            "  public final void containsAnyIn(Object[] expected) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnType_instanceMethods() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: consider an ImmutableList<Object> instead",
            "  public Object[] objectArray() {",
            "    return new String[]{\"a\"};",
            "  }",
            "  // BUG: Diagnostic contains: consider an ImmutableList<String> instead",
            "  public String[] stringArray() {",
            "    return new String[]{\"a\"};",
            "  }",
            "  public int[] intArray() {",
            "    return new int[]{42};",
            "  }",
            "  public Object objectValue() {",
            "    return \"a\";",
            "  }",
            "  public String stringValue() {",
            "    return \"a\";",
            "  }",
            "  public int intValue() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnType_staticMethods() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: consider an ImmutableList<Object> instead",
            "  public static Object[] objectArray() {",
            "    return new String[]{\"a\"};",
            "  }",
            "  // BUG: Diagnostic contains: consider an ImmutableList<String> instead",
            "  public static String[] stringArray() {",
            "    return new String[]{\"a\"};",
            "  }",
            "  public static int[] intArray() {",
            "    return new int[]{42};",
            "  }",
            "  public static Object objectValue() {",
            "    return \"a\";",
            "  }",
            "  public static String stringValue() {",
            "    return \"a\";",
            "  }",
            "  public static int intValue() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mainMethod() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  public static void main(String[] args) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void twoDimensionalArrays() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: Avoid returning a String[][]",
            "  public String[][] returnValue() {",
            "    return new String[2][2];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varArgs() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  public void varArgs(String... strings) {",
            "  }",
            "  // BUG: Diagnostic contains: consider an Iterable<Class> instead",
            "  public void varArgs(Class[] clazz, String... strings) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationMethod() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "public @interface TestAnnotation {", //
            "  String[] value();", //
            "}")
        .doTest();
  }

  @Test
  public void overridden() {
    compilationHelper
        .addSourceLines(
            "Parent.java",
            "public abstract class Parent {",
            "  // BUG: Diagnostic contains: consider an ImmutableList<String> instead",
            "  public abstract String[] stringArray();",
            "}")
        .addSourceLines(
            "Child.java",
            "public class Child extends Parent {",
            "  @Override",
            // we intentionally don't complain about this API since it's the parent class's fault
            "  public String[] stringArray() {",
            "    return new String[]{\"a\"};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void junitParams() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "import org.junit.runners.Parameterized.Parameters;",
            "public class ArrayUsage {",
            "  @Parameters(name = \"{0}\")",
            "  public static Object[] locales() {",
            "    return new Object[] {\"vi\"};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringArrayNamedArgs() {
    compilationHelper
        .addSourceLines(
            "ArrayUsage.java",
            "public class ArrayUsage {",
            "  // BUG: Diagnostic contains: consider an Iterable<String> instead",
            "  public static void doSomething1(String[] args) {",
            "  }",
            "  // BUG: Diagnostic contains: consider an Iterable<String> instead",
            "  public static void doSomething2(String[] argv) {",
            "  }",
            "  // BUG: Diagnostic contains: consider an Iterable<String> instead",
            "  public static void doSomething3(String[] argz) {",
            "  }",
            "}")
        .doTest();
  }
}
