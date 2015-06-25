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

/** {@link PackageLocation}Test */
@RunWith(JUnit4.class)
public class PackageLocationTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(PackageLocation.class, getClass());
  }

  @Test
  public void positiveCustomRoot() throws Exception {
    compilationHelper
        .addSourceLines(
            "src/main/java/a/b/A.java",
            "// BUG: Diagnostic contains: Expected package a to be declared in a directory ending with a, instead found /src/main/java/a/b",
            "package a;",
            "class A {}")
        .doTest();
  }

  @Test
  public void positiveTooLong() throws Exception {
    compilationHelper
        .addSourceLines(
            "src/main/java/A.java",
            "// BUG: Diagnostic contains: Expected package a.b.c to be declared in a directory ending with a/b/c, instead found /src/main/java",
            "package a.b.c;",
            "class A {}")
        .doTest();
  }

  @Test
  public void positiveTooShort() throws Exception {
    compilationHelper
        .addSourceLines(
            "b/c/d/A.java",
            "// BUG: Diagnostic contains: Expected package a.b.c.d to be declared in a directory ending with a/b/c/d, instead found /b/c/d",
            "package a.b.c.d;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negative2() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/b/c/A.java",
            "package a.b.c;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negativeSuffix() throws Exception {
    compilationHelper
        .addSourceLines(
            "src/main/java/a/b/A.java",
            "package a.b;",
            "class A {}")
        .doTest();
  }
}
