/*
 * Copyright 2023 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class NonFinalStaticFieldTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(NonFinalStaticField.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(NonFinalStaticField.class, getClass());

  @Test
  public void positiveFixable() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private static String FOO = \"\";",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  private static final String FOO = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void positiveButNotFixable_noRefactoring() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  public static String FOO = \"\";",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveNotFixable_finding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public static String FOO = \"\";",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  private static final String FOO = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void reassigned_noFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  private static String foo = \"\";",
            "  public void set(String s) {",
            "    foo = s;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void reassigned_finding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  private static String foo = \"\";",
            "  public void set(String s) {",
            "    foo = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void incremented_noFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private static int foo = 0;",
            "  public void increment() {",
            "    foo++;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void incrementedAnotherWay_noFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private static int foo = 0;",
            "  public void increment() {",
            "    foo += 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  // NOTE: this _is_ safe to suggest a fix for, just harder to detect.
  public void initialisedExactlyOnceInStaticInitializer_noFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private static int foo;",
            "  { foo = 1; }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void neverAssigned_getsDefaultInitializer() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private static int foo;",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  private static final int foo = 0;",
            "}")
        .doTest();
  }

  @Test
  public void negativeInterface() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "public interface Test {",
            "  String FOO = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void exemptedAnnotation_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "public class Test {",
            "  @Mock private static String foo;",
            "}")
        .doTest();
  }

  @Test
  public void volatileRemoved() {
    refactoringTestHelper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  private volatile static String FOO = \"\";",
            "}")
        .addOutputLines(
            "Test.java", //
            "public class Test {",
            "  private static final String FOO = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void beforeClass() {
    compilationTestHelper
        .addSourceLines(
            "Test.java", //
            "import org.junit.BeforeClass;",
            "public class Test {",
            "  private static String foo;",
            "  @BeforeClass",
            "  public static void setup() {",
            "    foo = \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void beforeAll() {
    compilationTestHelper
        .addSourceLines(
            "org/junit/jupiter/api/BeforeAll.java",
            "package org.junit.jupiter.api;",
            "",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "",
            "@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Documented",
            "public @interface BeforeAll {",
            "}")
        .addSourceLines(
            "Test.java", //
            "import org.junit.jupiter.api.BeforeAll;",
            "public class Test {",
            "  private static String foo;",
            "  @BeforeAll",
            "  public static void setup() {",
            "    foo = \"\";",
            "  }",
            "}")
        .doTest();
  }
}
