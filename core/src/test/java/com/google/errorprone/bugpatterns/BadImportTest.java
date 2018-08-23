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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link BadImport}. */
@RunWith(JUnit4.class)
public final class BadImportTest {
  CompilationTestHelper compilationTestHelper;

  @Before
  public void setup() {
    compilationTestHelper = CompilationTestHelper.newInstance(BadImport.class, getClass());
  }

  @Test
  public void positive_static_simpleCase() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableList.of()",
            "  ImmutableList<?> list = of();",
            "}")
        .doTest();
  }

  @Test
  public void positive_static_differentOverloadsInvoked() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  // BUG: Diagnostic contains: "
                + "ImmutableList.of(ImmutableList.of(1, 2, 3), ImmutableList.of())",
            "  ImmutableList<?> list = of(of(1, 2, 3), of());",
            "}")
        .doTest();
  }

  @Test
  public void positive_static_locallyDefinedMethod() {
    BugCheckerRefactoringTestHelper.newInstance(new BadImport(), getClass())
        .addInputLines(
            "in/Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  class Blah {",
            "    Blah() {",
            "      of();  // Left unchanged, because this is invoking Test.Blah.of.",
            "    }",
            "    void of() {}",
            "  }",
            "  ImmutableList<?> list = of();",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  class Blah {",
            "    Blah() {",
            "      of();  // Left unchanged, because this is invoking Test.Blah.of.",
            "    }",
            "    void of() {}",
            "  }",
            "  ImmutableList<?> list = ImmutableList.of();",
            "}")
        .doTest();
  }

  @Test
  public void negative_static_noStaticImport() {
    compilationTestHelper
        .addSourceLines(
            "in/Test.java",
            "class Test {",
            "  void of() {}",
            "  void foo() {",
            "    of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_nested() {
    compilationTestHelper.addSourceFile("BadImportPositiveCases.java").doTest();
  }

  @Test
  public void positive_nested_parentNotAlreadyImported() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList.Builder;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableList.Builder<String> builder = null;",
            "  Builder<String> builder = null;",
            "}")
        .doTest();
  }

  @Test
  public void positive_nested_conflictingName() {
    compilationTestHelper
        .addSourceLines(
            "thing/A.java",
            "package thing;",
            "public class A {",
            "  public static class B {",
            "    public static class Builder {",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import thing.A.B.Builder;",
            "class Test {",
            "  // BUG: Diagnostic contains: A.B.Builder builder;",
            "  Builder builder;",
            "  static class B {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_nested_conflictingNames_fullyQualified() {
    compilationTestHelper
        .addSourceLines(
            "thing/A.java",
            "package thing;",
            "public class A {",
            "  public static class B {",
            "    public static class Builder {",
            "    }",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import thing.A.B.Builder;",
            "class Test {",
            "  // BUG: Diagnostic contains: thing.A.B.Builder builder",
            "  Builder builder;",
            "  static class A {}",
            "  static class B {}",
            "}")
        .doTest();
  }

  @Test
  public void negative_nested() {
    compilationTestHelper.addSourceFile("BadImportNegativeCases.java").doTest();
  }

  @Test
  public void negative_badImportIsTopLevelClass() {
    compilationTestHelper
        .addSourceLines(
            "thing/thang/Builder.java", // Avoid rewrapping onto single line.
            "package thing.thang;",
            "public class Builder {}")
        .addSourceLines(
            "Test.java", // Avoid rewrapping onto single line.
            "import thing.thang.Builder;",
            "class Test {",
            "  Builder builder;",
            "}")
        .doTest();
  }

  @Test
  public void test_nested_fixes() {
    BugCheckerRefactoringTestHelper.newInstance(new BadImport(), getClass())
        .addInput("BadImportPositiveCases.java")
        .addOutput("BadImportPositiveCases_expected.java")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void test_nested_typeUseAnnotation() {
    BugCheckerRefactoringTestHelper.newInstance(new BadImport(), getClass())
        .addInputLines(
            "input/TypeUseAnnotation.java",
            "package test;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})",
            "@interface TypeUseAnnotation {}")
        .expectUnchanged()
        .addInputLines(
            "input/SomeClass.java",
            "package test;",
            "class SomeClass {",
            "  static class Builder {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "input/Test.java",
            "package test;",
            "import java.util.List;",
            "import test.SomeClass.Builder;",
            "abstract class Test {",
            "  @TypeUseAnnotation Builder builder;",
            "  @TypeUseAnnotation abstract Builder method1();",
            "  abstract @TypeUseAnnotation Builder method2();",
            "  abstract void method3(@TypeUseAnnotation Builder builder);",
            "  abstract void method4(List<@TypeUseAnnotation Builder> builder);",
            "}")
        .addOutputLines(
            "output/Test.java",
            "package test;",
            "import java.util.List;",
            "abstract class Test {",
            "  SomeClass.@TypeUseAnnotation Builder builder;",
            "  abstract SomeClass.@TypeUseAnnotation Builder method1();",
            "  abstract SomeClass.@TypeUseAnnotation Builder method2();",
            "  abstract void method3(SomeClass.@TypeUseAnnotation Builder builder);",
            "  abstract void method4(List<SomeClass.@TypeUseAnnotation Builder> builder);",
            "}")
        .doTest();
  }

  @Test
  public void suppressed_class() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "@SuppressWarnings(\"BadImport\")",
            "class Test {",
            "  ImmutableList<?> list = of();",
            "  ImmutableList<?> list2 = of();",
            "}")
        .doTest();
  }

  @Test
  public void suppressed_field() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  @SuppressWarnings(\"BadImport\")",
            "  ImmutableList<?> list = of();",
            "",
            "  // BUG: Diagnostic contains: ImmutableList.of()",
            "  ImmutableList<?> list2 = of();",
            "}")
        .doTest();
  }

  @Test
  public void suppressed_method() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.of;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  @SuppressWarnings(\"BadImport\")",
            "  void foo() {",
            "    ImmutableList<?> list = of();",
            "  }",
            "  void bar() {",
            "    // BUG: Diagnostic contains: ImmutableList.of()",
            "    ImmutableList<?> list2 = of();",
            "  }",
            "}")
        .doTest();
  }
}
