/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link StreamToIterable}. */
@RunWith(JUnit4.class)
public final class StreamToIterableTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(StreamToIterable.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new StreamToIterable(), getClass());

  @Test
  public void lambdaWithinEnhancedForLoop_recreatedEachTime_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test() {",
            "    for (int i : (Iterable<Integer>) () -> Stream.of(1, 2, 3).iterator()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withinEnhancedForLoop_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test() {",
            "    Stream<Integer> stream = Stream.of(1, 2, 3);",
            "    for (int i : (Iterable<Integer>) () -> stream.iterator()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReferenceRefactoredToExplicitCollection() {
    refactoring
        .addInputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test(List<Integer> i) {",
            "    addAll(Stream.of(1, 2, 3)::iterator);",
            "  }",
            "  void addAll(Iterable<Integer> ints) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test(List<Integer> i) {",
            "    addAll(Stream.of(1, 2, 3).collect(toImmutableList()));",
            "  }",
            "  void addAll(Iterable<Integer> ints) {}",
            "}")
        .doTest();
  }

  @Test
  public void lambdaRefactoredToExplicitCollection() {
    refactoring
        .addInputLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test(List<Integer> i) {",
            "    Stream<Integer> stream = Stream.of(1, 2, 3);",
            "    addAll(() -> stream.iterator());",
            "  }",
            "  void addAll(Iterable<Integer> ints) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.collect.ImmutableList.toImmutableList;",
            "import java.util.List;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void test(List<Integer> i) {",
            "    Stream<Integer> stream = Stream.of(1, 2, 3);",
            "    addAll(stream.collect(toImmutableList()));",
            "  }",
            "  void addAll(Iterable<Integer> ints) {}",
            "}")
        .doTest();
  }
}
