/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class StaticQualifiedUsingExpressionTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(StaticQualifiedUsingExpression.class, getClass());
  }

  @Test
  public void testPositiveCase1() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionPositiveCase1.java").doTest();
  }

  @Test
  public void testPositiveCase2() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionPositiveCase2.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("StaticQualifiedUsingExpressionNegativeCases.java").doTest();
  }

  @Test
  public void clash() {
    BugCheckerRefactoringTestHelper.newInstance(new StaticQualifiedUsingExpression(), getClass())
        .addInputLines(
            "a/Lib.java", //
            "package a;",
            "public class Lib {",
            "  public static final int CONST = 0;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "b/Lib.java", //
            "package b;",
            "public class Lib {",
            "  public static final int CONST = 0;",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "import a.Lib;",
            "class Test {",
            "  int x = Lib.CONST + new b.Lib().CONST;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import a.Lib;",
            "class Test {",
            "  int x = Lib.CONST + b.Lib.CONST;",
            "}")
        .doTest();
  }

  @Test
  public void expr() {
    BugCheckerRefactoringTestHelper.newInstance(new StaticQualifiedUsingExpression(), getClass())
        .addInputLines(
            "I.java", //
            "interface I {",
            "  int CONST = 42;",
            "  I id();",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  void f(I i) {",
            "    System.err.println(((I) null).CONST);",
            "    System.err.println(i.id().CONST);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "class Test {",
            "  void f(I i) {",
            "    System.err.println(I.CONST);",
            "    i.id();",
            "    System.err.println(I.CONST);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void superAccess() {
    BugCheckerRefactoringTestHelper.newInstance(new StaticQualifiedUsingExpression(), getClass())
        .addInputLines(
            "I.java", //
            "interface I {",
            "  interface Builder {",
            "    default void f() {}",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java", //
            "interface J extends I {",
            "  interface Builder extends I.Builder {",
            "    default void f() {}",
            "    default void aI() {",
            "      I.Builder.super.f();",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
