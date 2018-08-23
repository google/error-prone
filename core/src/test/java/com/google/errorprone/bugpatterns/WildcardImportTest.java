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

import com.google.common.base.Predicates;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link WildcardImport}Test */
@RunWith(JUnit4.class)
public class WildcardImportTest {
  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    testHelper = BugCheckerRefactoringTestHelper.newInstance(new WildcardImport(), getClass());
  }

  @Test
  public void chainOffStatic() {
    testHelper
        .addInputLines(
            "a/One.java",
            "package a;",
            "public class One {",
            "  public static Two THE_INSTANCE = null;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "a/Two.java",
            "package a;",
            "public class Two {",
            "  public static String MESSAGE = \"Hello\";",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import static a.One.*;",
            "public class Test {",
            "  String m = THE_INSTANCE.MESSAGE;",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import static a.One.THE_INSTANCE;",
            "public class Test {",
            "  String m = THE_INSTANCE.MESSAGE;",
            "}")
        .doTest();
  }

  @Test
  public void classLiteral() {
    testHelper
        .addInputLines("a/A.java", "package a;", "public class A {", "}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import a.*;",
            "public class Test {",
            "  void m() {",
            "     System.err.println(A.class);",
            "  }",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import a.A;",
            "public class Test {",
            "  void m() {",
            "     System.err.println(A.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethod() {
    testHelper
        .addInputLines(
            "a/A.java", //
            "package a;",
            "public class A {",
            "  public static void f() {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import static a.A.*;",
            "public class Test {",
            "  void m() {",
            "    f();",
            "  }",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import static a.A.f;",
            "public class Test {",
            "  void m() {",
            "    f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enumTest() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import static java.nio.charset.StandardCharsets.*;",
            "public class Test {",
            "  void m() {",
            "    System.err.println(UTF_8);",
            "  }",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "public class Test {",
            "  void m() {",
            "    System.err.println(UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.*;",
            "public class Test {",
            "    java.util.Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "public class Test {",
            "    java.util.Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void doublePrefix() {
    testHelper
        .addInputLines(
            "foo/Foo.java", //
            "package foo;",
            "public class Foo {}")
        .expectUnchanged()
        .addInputLines(
            "foo/bar/Bar.java", //
            "package foo.bar;",
            "public class Bar {}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import foo.*;",
            "import foo.bar.*;",
            "public class Test {",
            "    void f(Bar c) {}",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import foo.bar.Bar;",
            "public class Test {",
            "    void f(Bar c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveClassSelect() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.*;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import java.util.Map;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveInnerClass() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.Map.*;",
            "public class Test {",
            "    Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import java.util.Map.Entry;",
            "public class Test {",
            "    Entry<String, String> e;",
            "    C c;",
            "    static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void dontImportRuntime() {
    testHelper
        .addInputLines(
            "in/test/Test.java", //
            "package test;",
            "public class Test {",
            "    String s;",
            "}")
        .addOutputLines(
            "out/test/Test.java", //
            "package test;",
            "public class Test {",
            "    String s;",
            "}")
        .doTest();
  }

  @Test
  public void dontImportSelf() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.*;",
            "public class Test {",
            "    Test s;",
            "}")
        .addOutputLines(
            "out/test/Test.java", //
            "package test;",
            "public class Test {",
            "    Test s;",
            "}")
        .doTest();
  }

  @Test
  public void dontImportSelfPrivate() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import test.Test.Inner.*;",
            "public class Test {",
            "  public static class Inner {",
            "    private static class InnerMost {",
            "      InnerMost i;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
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
  public void dontImportSelfNested() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.*;",
            "public class Test {",
            "  public static class Inner {",
            "    Inner t;",
            "  }",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "public class Test {",
            "  public static class Inner {",
            "    Inner t;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void importSamePackage() {
    testHelper
        .addInputLines(
            "test/A.java",
            "package test;",
            "public class A {",
            "  public static class Inner {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import test.A.*;",
            "public class Test {",
            "  Inner t;",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import test.A.Inner;",
            "public class Test {",
            "  Inner t;",
            "}")
        .doTest();
  }

  @Test
  public void negativeNoWildcard() {
    CompilationTestHelper.newInstance(WildcardImport.class, getClass())
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

  @Test
  public void sameUnitWithSpuriousWildImport() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import java.util.Map;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    private static class C {}",
            "}")
        .addOutputLines(
            "test/Test.java",
            "package test;",
            "import java.util.Map;",
            "public class Test {",
            "    Map.Entry<String, String> e;",
            "    C c;",
            "    private static class C {}",
            "}")
        .doTest();
  }

  @Test
  public void nonCanonical() {
    testHelper
        .addInputLines(
            "a/One.java", //
            "package a;",
            "public class One extends Two {",
            "}")
        .expectUnchanged()
        .addInputLines(
            "a/Two.java", //
            "package a;",
            "public class Two {",
            "  public static class Inner {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import static a.One.*;",
            "public class Test {",
            "  Inner i;",
            "}")
        .addOutputLines(
            "out/test/Test.java",
            "package test;",
            "import a.Two.Inner;",
            "public class Test {",
            "  Inner i;",
            "}")
        .doTest();
  }

  @Test
  public void memberImport() {
    testHelper
        .addInputLines(
            "in/test/Test.java",
            "package test;",
            "import static java.util.Arrays.*;",
            "import java.util.*;",
            "public class Test {",
            "  List<Integer> xs = asList(1, 2, 3);",
            "}")
        .addOutputLines(
            "test/Test.java",
            "package test;",
            "import static java.util.Arrays.asList;",
            "import java.util.List;",
            "public class Test {",
            "  List<Integer> xs = asList(1, 2, 3);",
            "}")
        .doTest();
  }

  @Test
  public void qualifyMembersFix() {
    String[] enumLines = {
      "package e;",
      "public enum E {",
      "  A, B, C, D, E, F, G, H, I, J,",
      "  K, L, M, N, O, P, Q, R, S, T,",
      "  U, V, W, X, Y, Z",
      "}"
    };
    String[] testLines = {
      "// BUG: Diagnostic matches: X",
      "import static e.E.*;",
      "public class Test {",
      "  Object[] ex = {",
      "    A, B, C, D, E, F, G, H, I, J,",
      "    K, L, M, N, O, P, Q, R, S, T,",
      "    U, V, W, X, Y, Z",
      "  };",
      "  boolean f(e.E e) {",
      "    switch (e) {",
      "      case A:",
      "      case E:",
      "      case I:",
      "      case O:",
      "      case U:",
      "        return true;",
      "      default:",
      "        return false;",
      "    }",
      "  }",
      "}"
    };

    CompilationTestHelper.newInstance(WildcardImport.class, getClass())
        .addSourceLines("e/E.java", enumLines)
        .addSourceLines("e/Test.java", testLines)
        .expectErrorMessage(
            "X",
            Predicates.and(
                Predicates.containsPattern("Wildcard imports.*should not be used"),
                Predicates.not(Predicates.contains(Pattern.compile("Did you mean")))))
        .doTest();

    testHelper
        .addInputLines("e/E.java", enumLines)
        .expectUnchanged()
        .addInputLines("in/Test.java", testLines)
        .addOutputLines(
            "out/Test.java",
            "import e.E;",
            "public class Test {",
            "  Object[] ex = {",
            "    E.A, E.B, E.C, E.D, E.E, E.F, E.G, E.H, E.I, E.J,",
            "    E.K, E.L, E.M, E.N, E.O, E.P, E.Q, E.R, E.S, E.T,",
            "    E.U, E.V, E.W, E.X, E.Y, E.Z",
            "  };",
            "  boolean f(e.E e) {",
            "    switch (e) {",
            "      case A:",
            "      case E:",
            "      case I:",
            "      case O:",
            "      case U:",
            "        return true;",
            "      default:",
            "        return false;",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
