/*
 * Copyright 2015 The Error Prone Authors.
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

/** {@link ClassName}Test */
@RunWith(JUnit4.class)
public class ClassNameTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ClassName.class, getClass());
  }

  @Test
  public void twoClasses() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "// BUG: Diagnostic contains: A inside A.java, instead found: One, Two",
            "package a;",
            "class One {}",
            "class Two {}")
        .doTest();
  }

  @Test
  public void packageInfo() {
    compilationHelper
        .addSourceLines("a/package-info.java", "/** Documentation for our package */", "package a;")
        .addSourceLines(
            "b/Test.java",
            "// BUG: Diagnostic contains: Test inside Test.java, instead found: Green",
            "package b;",
            "class Green {}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines("a/A.java", "package a;", "class A {}")
        .addSourceLines("b/B.java", "package b;", "class B {}")
        .doTest();
  }

  @Test
  public void negativeMultipleTopLevel() {
    compilationHelper
        .addSourceLines("a/A.java", "package a;", "class A {}")
        .addSourceLines("b/B.java", "package b;", "class B {}", "class C {}")
        .doTest();
  }

  @Test
  public void negativeInnerClass() {
    compilationHelper
        .addSourceLines("b/B.java", "package b;", "class B {", "  static class Inner {}", "}")
        .doTest();
  }

  @Test
  public void negativeInterface() {
    compilationHelper
        .addSourceLines("b/B.java", "package b;", "interface B {", "  static class Inner {}", "}")
        .doTest();
  }

  @Test
  public void negativeEnum() {
    compilationHelper.addSourceLines("b/B.java", "package b;", "enum B {", "  ONE;", "}").doTest();
  }

  @Test
  public void negativeAnnotation() {
    compilationHelper
        .addSourceLines("b/B.java", "package b;", "public @interface B {", "}")
        .doTest();
  }

  @Test
  public void negativeIsPublic() {
    compilationHelper
        .addSourceLines(
            "b/B.java",
            "package b;",
            "// BUG: Diagnostic contains: should be declared in a file named Test.java",
            "public class Test {",
            "}")
        .ignoreJavacErrors()
        .matchAllDiagnostics()
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "b/Test.java", //
            "package b;",
            "@SuppressWarnings(\"ClassName\")",
            "class Green {}")
        .doTest();
  }
}
