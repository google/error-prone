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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link WildcardImport}Test */
@RunWith(JUnit4.class)
public class WildcardImportTest {
  private CompilationTestHelper compilationHelper;

  @BugPattern(
      name = "WildcardImport", summary = "Use of wildcard imports is forbidden", category = GUAVA,
      severity = ERROR, maturity = EXPERIMENTAL)
  public static class WildcardImportTestChecker extends WildcardImport {
    public WildcardImportTestChecker() {
      super(FixStrategies.TEST);
    }
  }

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(new WildcardImportTestChecker(), getClass());
  }

  @Test
  public void chainOffStatic() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "public class One {",
            "  public static Two THE_INSTANCE = null;",
            "}")
        .addSourceLines("a/Two.java",
            "package a;",
            "public class Two {",
            "  public static String MESSAGE = \"Hello\";",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import static a.One.THE_INSTANCE;]",
            "import static a.One.*;",
            "public class Test {",
            "  String m = THE_INSTANCE.MESSAGE;",
            "}")
        .doTest();
  }

  @Test
  public void classLiteral() throws Exception {
    compilationHelper
        .addSourceLines("a/A.java",
            "package a;",
            "public class A {",
            "}")
        .addSourceLines("test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import a.A;]",
            "import a.*;",
            "public class Test {",
            "  void m() {",
            "     System.err.println(A.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethod() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static void f() {}",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import static a.A.f;]",
            "import static a.A.*;",
            "public class Test {",
            "  void m() {",
            "    f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumTest() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import static java.nio.charset.StandardCharsets.UTF_8;]",
            "import static java.nio.charset.StandardCharsets.*;",
            "public class Test {",
            "  void m() {",
            "    System.err.println(UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import java.util.*;",
            "public class Test {",
            "    java.util.Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void doublePrefix() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import java.*;",
            "// BUG: Diagnostic contains: [import java.util.List;]",
            "import java.util.*;",
            "public class Test {",
            "    void f(List c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveClassSelect() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import java.util.Map;]",
            "import java.util.*;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveInnerClass() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import java.util.Map.Entry;]",
            "import java.util.Map.*;",
            "public class Test {",
            "    Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void dontImportRuntime() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import java.util.*;",
            "public class Test {",
            "    String s;",
            "}")
        .doTest();
  }

  @Test
  public void dontImportSelf() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import java.util.*;",
            "public class Test {",
            "    Test s;",
            "}")
        .doTest();
  }

  @Test
  public void dontImportSelfPrivate() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import test.Test.Inner.*;",
            "public class Test {",
            "  public static class Inner {",
            "    private static class InnerMost {",
            "      InnerMost i;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontImportSelfNested() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: []",
            "import java.util.*;",
            "public class Test {",
            "  public static class Inner {",
            "    Inner t;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void importSamePackage() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "public class A {",
            "  public static class Inner {}",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import test.A.Inner;]",
            "import test.A.*;",
            "public class Test {",
            "  Inner t;",
            "}")
        .doTest();
  }

  @Test
  public void negativeNoWildcard() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Map;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  // This is too hard to get right. We try to add an import for Test.C, which will fail because C
  // is private. Note that the import is only added because there was already a wildcard import
  // for test.Test.*.
  @Ignore
  public void sameUnitWithSpuriousWildImport() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Map;",
            "// BUG: Diagnostic contains: []",
            "import test.Test.*;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    private static class C {}",
            "}")
        .doTest();
  }

  // Also too hard. Here 'test.Two.Inner' is imported via 'import static a.One.*' by its
  // non-canonical name 'a.One.Inner'. We could add more heuristics for non-canonical names,
  // but this (almost) never happens in practice.
  @Ignore
  public void nonCanonical() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/One.java",
            "package a;",
            "public class One extends Two {",
            "}")
        .addSourceLines(
            "a/Two.java",
            "package a;",
            "public class Two {",
            "  public static class Inner {}",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "// BUG: Diagnostic contains: [import a.Two.Inner;]",
            "import static a.One.*;",
            "public class Test {",
            "  Inner i;",
            "}")
        .doTest();
  }

}
