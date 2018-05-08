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

/** Unit tests for {@link BadNestedImport}. */
@RunWith(JUnit4.class)
public final class BadNestedImportTest {
  CompilationTestHelper compilationTestHelper;

  @Before
  public void setup() {
    compilationTestHelper = CompilationTestHelper.newInstance(BadNestedImport.class, getClass());
  }

  @Test
  public void positiveCases() {
    compilationTestHelper.addSourceFile("BadNestedImportPositiveCases.java").doTest();
  }

  @Test
  public void positiveCases_parentNotAlreadyImported() {
    // Ensure that the snippet reports on the first occurrence, not the import.
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
  public void positiveCases_conflictingName() {
    // Ensure that the snippet reports on the first occurrence, not the import.
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
  public void negativeCases_conflictingNames_noResolution() {
    // Ensure that the snippet reports on the first occurrence, not the import.
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
            "  Builder builder;",
            "  static class A {}",
            "  static class B {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationTestHelper.addSourceFile("BadNestedImportNegativeCases.java").doTest();
  }

  @Test
  public void testFixes() throws Exception {
    BugCheckerRefactoringTestHelper.newInstance(new BadNestedImport(), getClass())
        .addInput("BadNestedImportPositiveCases.java")
        .addOutput("BadNestedImportPositiveCases_expected.java")
        .doTest(TestMode.AST_MATCH);
  }
}
