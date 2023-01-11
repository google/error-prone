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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class YodaConditionTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(YodaCondition.class, getClass());

  @Test
  public void primitive() {
    refactoring
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(int a) {",
            "    return 4 == a;",
            "  }",
            "  boolean notYoda(int a) {",
            "    return a == 4;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(int a) {",
            "    return a == 4;",
            "  }",
            "  boolean notYoda(int a) {",
            "    return a == 4;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enums() {
    refactoring
        .addInputLines(
            "E.java",
            "enum E {",
            "  A, B;",
            "  boolean foo(E e) {",
            "    return this == e;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return E.A == a;",
            "  }",
            "  boolean notYoda(E a) {",
            "    return a == E.A;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return a == E.A;",
            "  }",
            "  boolean notYoda(E a) {",
            "    return a == E.A;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullIntolerantFix() {
    refactoring
        .addInputLines("E.java", "enum E {A, B}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return E.A.equals(a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return a.equals(E.A);",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void nullTolerantFix() {
    refactoring
        .addInputLines("E.java", "enum E {A, B}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return E.A.equals(a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Objects;",
            "class Test {",
            "  boolean yoda(E a) {",
            "    return Objects.equals(a, E.A);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void provablyNonNull_nullIntolerantFix() {
    refactoring
        .addInputLines("E.java", "enum E {A, B}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    if (a != null) {",
            "      return E.A.equals(a);",
            "    }",
            "    return true;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  boolean yoda(E a) {",
            "    if (a != null) {",
            "      return a.equals(E.A);",
            "    }",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }
}
