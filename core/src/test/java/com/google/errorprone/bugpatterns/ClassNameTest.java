/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import java.util.Arrays;

/** {@link ClassName}Test */
@RunWith(JUnit4.class)
public class ClassNameTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new ClassName());
  }

  @Test
  public void twoClasses() throws Exception {
      compilationHelper.assertCompileFailsWithMessages(
          Arrays.asList(
              compilationHelper.fileManager().forSourceLines("a/A.java",
                  "// BUG: Diagnostic contains: A inside A.java, instead found: One, Two",
                  "package a;",
                  "class One {}",
                  "class Two {}"),
              compilationHelper.fileManager().forSourceLines("b/B.java",
                  "// BUG: Diagnostic contains: B inside B.java, instead found: Three, Four",
                  "package b;",
                  "class Three {}",
                  "class Four {}")));
  }

  @Test
  public void packageInfo() throws Exception {
      compilationHelper.assertCompileFailsWithMessages(
          Arrays.asList(
              compilationHelper.fileManager().forSourceLines("a/package-info.java",
                  "/** Documentation for our package */",
                  "package a;"),
              compilationHelper.fileManager().forSourceLines("b/Test.java",
                  "// BUG: Diagnostic contains: Test inside Test.java, instead found: Green",
                  "package b;",
                  "class Green {}")));
  }


  @Test
  public void negative() throws Exception {
      compilationHelper.assertCompileSucceeds(
          Arrays.asList(
              compilationHelper.fileManager().forSourceLines("a/A.java",
                  "package a;",
                  "class A {}"),
              compilationHelper.fileManager().forSourceLines("b/B.java",
                  "package b;",
                  "class B {}")));
  }

  @Test
  public void negativeMultipleTopLevel() throws Exception {
      compilationHelper.assertCompileSucceeds(
          Arrays.asList(
              compilationHelper.fileManager().forSourceLines("a/A.java",
                  "package a;",
                  "class A {}"),
              compilationHelper.fileManager().forSourceLines("b/B.java",
                  "package b;",
                  "class B {}",
                  "class C {}")));
  }

  @Test
  public void negativeInnerClass() throws Exception {
      compilationHelper.assertCompileSucceeds(
          compilationHelper.fileManager().forSourceLines("b/B.java",
              "package b;",
              "class B {",
              "  static class Inner {}",
              "}"));
  }

  @Test
  public void negativeInterface() throws Exception {
      compilationHelper.assertCompileSucceeds(
          compilationHelper.fileManager().forSourceLines("b/B.java",
              "package b;",
              "interface B {",
              "  static class Inner {}",
              "}"));
  }

  @Test
  public void negativeEnum() throws Exception {
      compilationHelper.assertCompileSucceeds(
          compilationHelper.fileManager().forSourceLines("b/B.java",
              "package b;",
              "enum B {",
              "  ONE;",
              "}"));
 }

  @Test
  public void negativeAnnotation() throws Exception {
      compilationHelper.assertCompileSucceeds(
          compilationHelper.fileManager().forSourceLines("b/B.java",
              "package b;",
              "public @interface B {",
              "}"));
  }
}
