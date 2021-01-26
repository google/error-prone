/*
 * Copyright 2021 The Error Prone Authors.
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

/** Unit tests of {@link StreamMapToPeek}. */
@RunWith(JUnit4.class)
public class StreamMapToPeekTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(StreamMapToPeek.class, StreamMapToPeekTest.class);
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(StreamMapToPeek.class, StreamMapToPeekTest.class);

  @Test
  public void positive_diagnostic() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    // BUG: Diagnostic contains:Stream.peek",
            "    return stream.map(val -> {",
            "          System.out.println(val);",
            "          return val;",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_refactoring() {
    refactoringTestHelper
        .addInputLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.map(val -> {",
            "          System.out.println(val);",
            "          return val;",
            "        });",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.peek(val -> {",
            "          System.out.println(val);",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_multipleReturns() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.map(val -> {",
            // There are multiple return paths from the block
            "          if (val < 0) {",
            "            return 0;",
            "          }",
            "          return val;",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonIdentityReturn() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.map(val -> {",
            "          System.out.println(val);",
            // Return value is not simply 'val'
            "          return val * 2;",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_paramAssignment() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.map(val -> {",
            "          val = val + 2;",
            "          return val;",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_expressionLambda() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Integer> operate(Stream<Integer> stream) {",
            "    return stream.map(val -> val);",
            "  }",
            "}")
        .doTest();
  }

  /** Stream.map() can be used for an implicit type conversion, .peek() cannot. */
  @Test
  public void negative_typeConversion() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream<Object> operate(Stream<Integer> stream) {",
            // No suggestion, because peek() will fail here.
            "    return stream.map(val -> {",
            "          System.out.println(val);",
            "          return val;",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_raw() {
    compilationTestHelper
        .addSourceLines(
            "in/Foo.java",
            "import java.util.stream.Stream;",
            "",
            "class Foo {",
            "  static Stream operate(Stream stream) {",
            // No transformation, because the type analysis fails
            "    return stream.map(val -> {",
            "          System.out.println(val);",
            "          return val;",
            "        });",
            "  }",
            "}")
        .doTest();
  }
}
