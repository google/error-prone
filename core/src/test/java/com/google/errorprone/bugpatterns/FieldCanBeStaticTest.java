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
import com.google.errorprone.ErrorProneFlags;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FieldCanBeStatic}Test */
@RunWith(JUnit4.class)
public class FieldCanBeStaticTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(
          new FieldCanBeStatic(ErrorProneFlags.empty()), getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FieldCanBeStatic.class, getClass());

  @Test
  public void simpleCase() {
    helper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final Duration myDuration = Duration.ofMillis(1);",
            "  public Duration d() {",
            "    return this.myDuration;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final Duration MY_DURATION = Duration.ofMillis(1);",
            "  public Duration d() {",
            "    return MY_DURATION;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instantWithPureMethod() {
    helper
        .addInputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  private final Instant instant = Instant.ofEpochMilli(1);",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  private static final Instant INSTANT = Instant.ofEpochMilli(1);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithNonPureMethod() {
    helper
        .addInputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  private final Instant instant = Instant.now();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void refersToStaticVariable() {
    helper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final int millis = 1;",
            "  private final Duration myDuration = Duration.ofMillis(millis);",
            "  public Duration d() {",
            "    return this.myDuration;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final int millis = 1;",
            "  private static final Duration MY_DURATION = Duration.ofMillis(millis);",
            "  public Duration d() {",
            "    return MY_DURATION;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refersToInstanceVariable() {
    helper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  final int millis = 1;",
            "  private final Duration myDuration = Duration.ofMillis(millis);",
            "  public Duration d() {",
            "    return this.myDuration;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void notPrivate_noMatch() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.time.Duration;",
            "class Test {",
            "  public final Duration d = Duration.ofMillis(1);",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void notFinal_noMatch() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.time.Duration;",
            "class Test {",
            "  private Duration d = Duration.ofMillis(1);",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void exemptedAnnotation_noMatch() {
    helper
        .addInputLines(
            "Test.java",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import java.time.Duration;",
            "class Test {",
            "  @Bind private final Duration d = Duration.ofMillis(1);",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void possibleImpureInitializer_noMatch() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.time.Duration;",
            "class Test {",
            "  private final Duration d = getDuration();",
            "  Duration getDuration() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private final int primitive = 3;",
            "  // BUG: Diagnostic contains:",
            "  private final String string = \"string\";",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String staticFinalInitializer;",
            "  static {",
            "    staticFinalInitializer = \"string\";",
            "  }",
            "  private static final String staticFinal = \"string\";",
            "  private int nonFinal = 3;",
            "  private static int staticNonFinal = 4;",
            "  private final Object finalMutable = new Object();",
            "  private final int nonLiteral = new java.util.Random().nextInt();",
            "  private final Person pojo = new Person(\"Bob\", 42);",
            "  private static class Person {",
            "    final String name;",
            "    final int age;",
            "    Person(String name, int age) {",
            "      this.name = name;",
            "      this.age = age;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private final int foo = 1;",
            "  private final int BAR_FIELD = 2;",
            "  int f() {",
            "    return foo + BAR_FIELD;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  private static final int FOO = 1;",
            "  private static final int BAR_FIELD = 2;",
            "  int f() {",
            "    return FOO + BAR_FIELD;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  class I {",
            "    private final Duration D = Duration.ofMillis(1);",
            "    // BUG: Diagnostic contains: can be static",
            "    private final int I = 42;",
            "  }",
            "  static class S {",
            "    // BUG: Diagnostic contains: can be static",
            "    private final Duration D = Duration.ofMillis(1);",
            "    // BUG: Diagnostic contains: can be static",
            "    private final int I = 42;",
            "  }",
            "  void f() {",
            "    class L {",
            "      private final Duration D = Duration.ofMillis(1);",
            "      // BUG: Diagnostic contains: can be static",
            "      private final int I = 42;",
            "    }",
            "    new Object() {",
            "      private final Duration D = Duration.ofMillis(1);",
            "      // BUG: Diagnostic contains: can be static",
            "      private final int I = 42;",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
