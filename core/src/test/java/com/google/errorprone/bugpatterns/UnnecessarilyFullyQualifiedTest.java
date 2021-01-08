/*
 * Copyright 2020 The Error Prone Authors.
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

/** Tests for {@link UnnecessarilyFullyQualified}. */
@RunWith(JUnit4.class)
public final class UnnecessarilyFullyQualifiedTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessarilyFullyQualified(), getClass());

  @Test
  public void singleUse() {
    helper
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  java.util.List foo();",
            "  java.util.List bar();",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.List;",
            "interface Test {",
            "  List foo();",
            "  List bar();",
            "}")
        .doTest();
  }

  @Test
  public void wouldBeAmbiguous() {
    helper
        .addInputLines(
            "List.java", //
            "class List {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java", //
            "interface Test {",
            "  java.util.List foo();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void refersToMultipleTypes() {
    helper
        .addInputLines(
            "List.java", //
            "package a;",
            "public class List {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "package b;",
            "interface Test {",
            "  java.util.List foo();",
            "  a.List bar();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void clashesWithTypeInSuperType() {
    helper
        .addInputLines(
            "A.java", //
            "package a;",
            "public interface A {",
            "  public static class List {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "package b;",
            "import a.A;",
            "class Test implements A {",
            "  java.util.List foo() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void builder() {
    helper
        .addInputLines(
            "Foo.java", //
            "package a;",
            "public class Foo {",
            "  public static final class Builder {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "package b;",
            "interface Test {",
            "  a.Foo foo();",
            "  a.Foo.Builder fooBuilder();",
            "}")
        .addOutputLines(
            "Test.java",
            "package b;",
            "import a.Foo;",
            "interface Test {",
            "  Foo foo();",
            "  Foo.Builder fooBuilder();",
            "}")
        .doTest();
  }

  @Test
  public void packageInfo() {
    CompilationTestHelper.newInstance(UnnecessarilyFullyQualified.class, getClass())
        .addSourceLines(
            "a/A.java", //
            "package a;",
            "public @interface A {}")
        .addSourceLines(
            "b/package-info.java", //
            "@a.A",
            "package b;")
        .doTest();
  }
}
