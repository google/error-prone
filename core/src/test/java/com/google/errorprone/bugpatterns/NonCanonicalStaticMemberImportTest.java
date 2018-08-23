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

/** {@link NonCanonicalStaticImport}Test */
@RunWith(JUnit4.class)
public class NonCanonicalStaticMemberImportTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(NonCanonicalStaticMemberImport.class, getClass());
  }

  @Test
  public void positiveMethod() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static final int foo() { return 42; }",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines(
            "b/Test.java",
            "package b;",
            "// BUG: Diagnostic contains: import static a.A.foo;",
            "import static b.B.foo;",
            "class Test {}")
        .doTest();
  }

  @Test
  public void positiveField() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static final int CONST = 42;",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines(
            "b/Test.java",
            "package b;",
            "// BUG: Diagnostic contains: import static a.A.CONST;",
            "import static b.B.CONST;",
            "class Test {}")
        .doTest();
  }

  // We can't test e.g. a.B.Inner.CONST (a double non-canonical reference), because
  // they're illegal.
  @Test
  public void positiveClassAndField() {
    compilationHelper
        .addSourceLines(
            "a/Super.java",
            "package a;",
            "public class Super {",
            "  public static final int CONST = 42;",
            "}")
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static class Inner extends Super {}",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines(
            "b/Test.java",
            "package b;",
            "// BUG: Diagnostic contains: import static a.Super.CONST;",
            "import static a.A.Inner.CONST;",
            "class Test {}")
        .doTest();
  }

  @Test
  public void negativeMethod() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static final int foo() { return 42; }",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines("b/Test.java", "package b;", "import static a.A.foo;", "class Test {}")
        .doTest();
  }

  @Test
  public void negativeField() {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static final int CONST = 42;",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines("b/Test.java", "package b;", "import static a.A.CONST;", "class Test {}")
        .doTest();
  }

  @Test
  public void negativeClassAndField() {
    compilationHelper
        .addSourceLines(
            "a/Super.java",
            "package a;",
            "public class Super {",
            "  public static final int CONST = 42;",
            "}")
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static class Inner extends Super {}",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {", "}")
        .addSourceLines(
            "b/Test.java", "package b;", "import static a.Super.CONST;", "class Test {}")
        .doTest();
  }
}
