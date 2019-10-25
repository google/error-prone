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

import com.google.common.base.Joiner;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link UndefinedEquals}
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@RunWith(JUnitParamsRunner.class)
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
            "  // BUG: Diagnostic contains: Queue does not have",
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
            "     // BUG: Diagnostic contains: Collection does not have",
            "    Objects.equals(a,b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableCollection() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableCollection;",
            "import java.util.Objects;",
            "class Test {",
            "  void f(ImmutableCollection a, ImmutableCollection b) {",
            "     // BUG: Diagnostic contains: ImmutableCollection does not have",
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
            "import com.google.common.collect.FluentIterable;",
            "import com.google.common.collect.Iterables;",
            "import static org.junit.Assert.assertEquals;",
            "import static org.junit.Assert.assertNotEquals;",
            "class Test {",
            "  void test(List myList, List otherList) {",
            "    // BUG: Diagnostic contains: Iterable does not have",
            "    assertEquals(FluentIterable.of(1), FluentIterable.of(1));",
            "    // BUG: Diagnostic contains: Iterable does not have",
            "    assertEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));",
            "    // BUG: Diagnostic contains: Iterable does not have",
            "    assertNotEquals(Iterables.skip(myList, 1), Iterables.skip(myList, 2));",
            "    // BUG: Diagnostic contains: Iterable does not have",
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
            "    // BUG: Diagnostic contains: Queue does not have",
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
            "    // BUG: Diagnostic contains: Queue does not have",
            "    assertThat(a).isEqualTo(b);",
            "    // BUG: Diagnostic contains: Queue does not have",
            "    assertThat(a).isNotEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Parameters(method = "truthFixParameters")
  public void truthFixParameterized(String input, String output) {
    BugCheckerRefactoringTestHelper.newInstance(new UndefinedEquals(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.Multimap;",
            "import java.lang.Iterable;",
            "import java.util.Collection;",
            "import java.util.Queue;",
            "class Test {",
            input,
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.Multimap;",
            "import java.lang.Iterable;",
            "import java.util.Collection;",
            "import java.util.Queue;",
            "class Test {",
            output,
            "}")
        .doTest();
  }

  private Object truthFixParameters() {
    return new Object[] {
      new String[] {
        lines(
            "  void f1(Multimap a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f1(Multimap a, Multimap b) {", //
            "    assertThat(a).containsExactlyEntriesIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f2(Multimap a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f2(Multimap a, Collection b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f3(Multimap a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f3(Multimap a, Iterable b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f4(Multimap a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f4(Multimap a, Queue b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f5(Collection a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f5(Collection a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f6(Collection a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f6(Collection a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f7(Collection a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f7(Collection a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f8(Collection a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f8(Collection a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f9(Iterable a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f9(Iterable a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f10(Iterable a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f10(Iterable a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f11(Iterable a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f11(Iterable a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f12(Iterable a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f12(Iterable a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f13(Queue a, Multimap b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f13(Queue a, Multimap b) {",
            "    assertThat(a).isEqualTo(b);", // no fix
            "  }")
      },
      new String[] {
        lines(
            "  void f14(Queue a, Collection b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f14(Queue a, Collection b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f15(Queue a, Iterable b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f15(Queue a, Iterable b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      },
      new String[] {
        lines(
            "  void f16(Queue a, Queue b) {", //
            "    assertThat(a).isEqualTo(b);",
            "  }"),
        lines(
            "  void f16(Queue a, Queue b) {", //
            "    assertThat(a).containsExactlyElementsIn(b);",
            "  }")
      }
    };
  }

  private String lines(String... lines) {
    return Joiner.on('\n').join(lines);
  }

  @Test
  public void truthFixAssertWithMessage() {
    BugCheckerRefactoringTestHelper.newInstance(new UndefinedEquals(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertWithMessage;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertWithMessage(\"message\").that(a).isEqualTo(b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertWithMessage;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertWithMessage(\"message\").that(a).containsExactlyElementsIn(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void truthFixDontRewriteIsNotEqualTo() {
    BugCheckerRefactoringTestHelper.newInstance(new UndefinedEquals(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertThat(a).isNotEqualTo(b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertThat(a).isNotEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void truthFixAcrossMultipleLinesAndPoorlyFormatted() {
    BugCheckerRefactoringTestHelper.newInstance(new UndefinedEquals(), getClass())
        .addInputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertThat(a).", // period should be on following line per style guide
            "      isEqualTo(b);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.lang.Iterable;",
            "class Test {",
            "  void f(Iterable a, Iterable b) {",
            "    assertThat(a).",
            "      containsExactlyElementsIn(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSparseArray() {
    compilationHelper
        .addSourceLines(
            "SparseArray.java", //
            "package android.util;",
            "public class SparseArray <T> {}")
        .addSourceLines(
            "Test.java",
            "import android.util.SparseArray;",
            "class Test {",
            "  <T> boolean f(SparseArray<T> a, SparseArray<T> b) {",
            "    // BUG: Diagnostic contains: SparseArray does not have",
            "    return a.equals(b);",
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
