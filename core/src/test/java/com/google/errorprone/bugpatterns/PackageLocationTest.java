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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link PackageLocation}Test */
@RunWith(JUnit4.class)
public class PackageLocationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(PackageLocation.class, getClass());

  @Test
  public void positiveCustomRoot() {
    compilationHelper
        .addSourceLines(
            "src/main/java/a/b/A.java",
            "// BUG: Diagnostic contains: Expected package a to be declared in a directory "
                + "ending with a, instead found b",
            "package a;",
            "class A {}")
        .doTest();
  }

  @Test
  public void positiveTooLong() {
    compilationHelper
        .addSourceLines(
            "src/main/java/A.java",
            "// BUG: Diagnostic contains: Expected package a.b.c to be declared in a directory "
                + "ending with a/b/c, instead found src/main/java",
            "package a.b.c;",
            "class A {}")
        .doTest();
  }

  @Test
  public void positiveTooShort() {
    compilationHelper
        .addSourceLines(
            "java/b/c/d/A.java",
            "// BUG: Diagnostic contains: Expected package a.b.c.d to be declared in a directory "
                + "ending with a/b/c/d, instead found java/b/c/d",
            "package a.b.c.d;",
            "class A {}")
        .doTest();
  }

  @Test
  public void positiveTooShortSuffix() {
    compilationHelper
        .addSourceLines(
            "panda/b/c/d/A.java",
            "// BUG: Diagnostic contains: Expected package a.b.c.d to be declared in a directory "
                + "ending with a/b/c/d, instead found panda/b/c/d",
            "package a.b.c.d;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "a/A.java", //
            "package a;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negative2() {
    compilationHelper
        .addSourceLines(
            "a/b/c/A.java", //
            "package a.b.c;",
            "class A {}")
        .doTest();
  }

  @Test
  public void negativeSuffix() {
    compilationHelper
        .addSourceLines(
            "src/main/java/a/b/A.java", //
            "package a.b;",
            "class A {}")
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "java/com/google/foo/package-info.java",
            "@com.google.errorprone.annotations.SuppressPackageLocation",
            "package xyz.abc.foo;")
        .addSourceLines(
            "java/com/google/foo/A.java", //
            "package xyz.abc.foo;",
            "class A {}")
        .doTest();
  }
}
