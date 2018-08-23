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

/** {@link UnnecessaryTypeArgument}Test */
@RunWith(JUnit4.class)
public class UnnecessaryTypeArgumentTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(UnnecessaryTypeArgument.class, getClass());
  }

  @Test
  public void positiveCall() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  void f() {}",
            "  void m() {",
            "    // BUG: Diagnostic contains: this.f()",
            "    this.<String>f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveThis() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  static class C {",
            "    public <T> C() {",
            "      // BUG: Diagnostic contains: /*START*/this(42)",
            "      /*START*/<String>this(42);",
            "    }",
            "    public C(int i) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSuper() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  static class B {",
            "    public B() {",
            "    }",
            "  }",
            "  static class C extends B {",
            "    public <T> C() {",
            "      // BUG: Diagnostic contains: /*START*/super()",
            "      /*START*/<String>super();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstantiation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  static class C {",
            "    public C() {}",
            "  }",
            "  void m() {",
            "    // BUG: Diagnostic contains: new C()",
            "    new <String, Integer>C();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  <T> void f() {}",
            "  void m() {",
            "    this.<String>f();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeGenericSuper() {
    compilationHelper
        .addSourceLines(
            "Super.java", "public class Super {", "  public <T> T f(T x) { return x; }", "}")
        .addSourceLines(
            "Sub.java",
            "@SuppressWarnings(\"unchecked\")",
            "public class Sub extends Super {",
            "  public Object f(Object x) { return x; }",
            "}")
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  void m(Sub s) {",
            "    s.<String>f(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void whitespaceFix() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo.bar;",
            "class Test {",
            "  void f() {}",
            "  void m() {",
            "    // BUG: Diagnostic contains: this.f()",
            "    this.<  /*  */ String  /*  */ >f();",
            "  }",
            "}")
        .doTest();
  }
}
