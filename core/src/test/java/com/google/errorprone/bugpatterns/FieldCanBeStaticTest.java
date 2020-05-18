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

/** {@link FieldCanBeStatic}Test */
@RunWith(JUnit4.class)
public class FieldCanBeStaticTest {

  @Test
  public void positive() {
    CompilationTestHelper.newInstance(FieldCanBeStatic.class, getClass())
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
    CompilationTestHelper.newInstance(FieldCanBeStatic.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String staticFinalInitializer;",
            "  static {",
            "    staticFinalInitializer = \"string\";",
            "  }",
            "  private static final String staticFinal = \"string\";",
            "  private int nonFinal = 3;",
            "  private static int staticNontFinal = 4;",
            "  private final Object finalMutable = new Object();",
            "  private final int nonLiteral = new java.util.Random().nextInt();",
            "  private final Person pojo = new Person(\"Bob\", 42);",
            "  private final String nullString = null;",
            "  @Deprecated private final String annotatedField = \"\";",
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
    BugCheckerRefactoringTestHelper.newInstance(new FieldCanBeStatic(), getClass())
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
}
