/*
 * Copyright 2019 The Error Prone Authors.
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

/** Unit tests for {@link RedundantOverride}. */
@RunWith(JUnit4.class)
public final class RedundantOverrideTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(RedundantOverride.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  @Override public boolean equals(Object o) {",
            "    return super.equals(o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void addingJavadoc() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  /** Adding javadoc. */",
            "  @Override public boolean equals(Object o) {",
            "    return super.equals(o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void addingComments() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @Override public boolean equals(Object o) {",
            "    // TODO..",
            "    return super.equals(o);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void considersParameterOrder() {
    testHelper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  public void swap(int a, int b) {}",
            "}")
        .addSourceLines(
            "B.java",
            "class B extends A {",
            "  @Override public void swap(int a, int b) {",
            "    super.swap(b, a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wideningVisibilityNoMatch() {
    testHelper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  void swap(int a, int b) {}",
            "}")
        .addSourceLines(
            "B.java",
            "class B extends A {",
            "  @Override public void swap(int a, int b) {",
            "    super.swap(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void addingAnnotations() {
    testHelper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  Object swap(int a, int b) {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "B.java",
            "import javax.annotation.Nullable;",
            "class B extends A {",
            "  @Nullable",
            "  @Override public Object swap(int a, int b) {",
            "    return super.swap(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameAnnotation() {
    testHelper
        .addSourceLines(
            "A.java", //
            "import javax.annotation.Nullable;",
            "class A {",
            "  @Nullable Object swap(int a, int b) {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "B.java",
            "import javax.annotation.Nullable;",
            "class B extends A {",
            "  @Nullable",
            "  // BUG: Diagnostic contains:",
            "  @Override Object swap(int a, int b) {",
            "    return super.swap(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void removesAnnotation() {
    testHelper
        .addSourceLines(
            "A.java", //
            "import javax.annotation.Nullable;",
            "class A {",
            "  @Nullable Object swap(int a, int b) {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "B.java",
            "import javax.annotation.Nullable;",
            "class B extends A {",
            "  @Override Object swap(int a, int b) {",
            "    return super.swap(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void addsAnnotation() {
    testHelper
        .addSourceLines(
            "A.java", //
            "class A {",
            "  Object swap(int a, int b) {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "B.java",
            "import javax.annotation.Nullable;",
            "class B extends A {",
            "  @Nullable",
            "  @Override Object swap(int a, int b) {",
            "    return super.swap(a, b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void protectedOverrideInDifferentPackage() {
    testHelper
        .addSourceLines(
            "A.java", //
            "package foo;",
            "public class A {",
            "  protected void swap() {}",
            "}")
        .addSourceLines(
            "B.java",
            "package bar;",
            "public class B extends foo.A {",
            "  @Override protected void swap() {",
            "    super.swap();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void protectedOverrideInSamePackage() {
    testHelper
        .addSourceLines(
            "A.java", //
            "package foo;",
            "class A {",
            "  protected void swap() {}",
            "}")
        .addSourceLines(
            "B.java",
            "package foo;",
            "class B extends A {",
            "  // BUG: Diagnostic contains:",
            "  @Override protected void swap() {",
            "    super.swap();",
            "  }",
            "}")
        .doTest();
  }
}
