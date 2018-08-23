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

/**
 * Unit tests for {@link UndefinedEquals}
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@RunWith(JUnit4.class)
public final class UndefinedEqualsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UndefinedEquals.class, getClass());

  @Test
  public void positiveInstanceEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Queue;",
            "class Test {",
            "  void f(Queue a, Queue b) {",
            "  // BUG: Diagnostic contains: java.util.Queue does not have",
            "    a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveStaticEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.Objects;",
            "class Test {",
            "  void f(Collection a, Collection b) {",
            "     // BUG: Diagnostic contains: java.util.Collection does not have",
            "    Objects.equals(a,b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveAssertEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import com.google.common.collect.Iterables;",
            "import static org.junit.Assert.assertEquals;",
            "import static org.junit.Assert.assertNotEquals;",
            "class Test {",
            "  void test(List myList, List otherList) {",
            "    // BUG: Diagnostic contains: java.lang.Iterable does not have",
            "    assertEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));",
            "    // BUG: Diagnostic contains: java.lang.Iterable does not have",
            "    assertNotEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));",
            "    // BUG: Diagnostic contains: java.lang.Iterable does not have",
            "    assertEquals(\"foo\", Iterables.skip(myList, 1), Iterables.skip(myList, 2));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithGenerics() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Queue;",
            "class Test {",
            "  <T> void f(Queue<String> a, Queue<T> b) {",
            "    // BUG: Diagnostic contains: java.util.Queue does not have",
            "    a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTruth() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Queue;",
            "class Test {",
            "  <T> void f(Queue<String> a, Queue<T> b) {",
            "    // BUG: Diagnostic contains: java.util.Queue does not have",
            "    assertThat(a).isEqualTo(b);",
            "    // BUG: Diagnostic contains: java.util.Queue does not have",
            "    assertThat(a).isNotEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.PriorityQueue;",
            "class Test {",
            "  void f(PriorityQueue a, PriorityQueue b) {",
            "    a.equals(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void charSequenceFix() {
    BugCheckerRefactoringTestHelper.newInstance(new UndefinedEquals(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "class Test {",
            "  void f(CharSequence a, String b) {",
            "    assertThat(a).isEqualTo(b);",
            "    assertThat(b).isEqualTo(a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "class Test {",
            "  void f(CharSequence a, String b) {",
            "    assertThat(a.toString()).isEqualTo(b);",
            "    assertThat(b).isEqualTo(a.toString());",
            "  }",
            "}")
        .doTest();
  }
}
