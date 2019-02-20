/*
 * Copyright 2018 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InterfaceWithOnlyStatics} bug pattern. */
@RunWith(JUnit4.class)
public class InterfaceWithOnlyStaticsTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(InterfaceWithOnlyStatics.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new InterfaceWithOnlyStatics(), getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains: InterfaceWithOnlyStatics",
            "interface Test {",
            "  public static final int foo = 42;",
            "}")
        .doTest();
  }

  @Test
  public void negative_hasNonStaticMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  public static final int foo = 42;",
            "  int bar();",
            "}")
        .doTest();
  }

  @Test
  public void negative_notInterface() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  public static final int foo = 42;",
            "}")
        .doTest();
  }

  @Test
  public void negative_annotation() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "@interface Test {",
            "  public static final int foo = 42;",
            "}")
        .doTest();
  }

  @Test
  public void negative_extends() {
    testHelper
        .addSourceLines(
            "A.java", //
            "interface A {}")
        .addSourceLines(
            "Test.java", //
            "interface Test extends A {",
            "  int foo = 42;",
            "  static int bar() { return 1; }",
            "}")
        .doTest();
  }

  @Test
  public void negative_daggerModules() {
    testHelper
        .addSourceLines(
            "Module.java", //
            "package dagger;",
            "public @interface Module {}")
        .addSourceLines(
            "ProducerModule.java", //
            "package dagger.producers;",
            "public @interface ProducerModule {}")
        .addSourceLines(
            "Test.java", //
            "import dagger.Module;",
            "@Module",
            "interface Test {",
            "  public static final int foo = 42;",
            "}")
        .addSourceLines(
            "Test.java", //
            "import dagger.producers.ProducerModule;",
            "@ProducerModule",
            "interface Test {",
            "  public static final int foo = 42;",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  int foo = 42;",
            "  static int bar() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "final class Test {",
            "  public static final int foo = 42;",
            "  public static int bar() { return 1; }",
            "  private Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring_innerClass() {
    refactoringHelper
        .addInputLines(
            "Foo.java", //
            "class Foo {",
            "  interface Test {",
            "    int foo = 42;",
            "  }",
            "}")
        .addOutputLines(
            "Foo.java",
            "class Foo {",
            "  static final class Test {",
            "    public static final int foo = 42;",
            "    private Test() {}",
            "  }",
            "}")
        .doTest();
  }
}
