/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers.FIRST;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for the missing fail matcher. */
@RunWith(JUnit4.class)
public class MissingFailTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingFail.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MissingFail.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("MissingFailPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCases2() {
    compilationHelper.addSourceFile("MissingFailPositiveCases2.java").doTest();
  }

  @Test
  public void testPositiveCases3() {
    compilationHelper.addSourceFile("MissingFailPositiveCases3.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("MissingFailNegativeCases.java").doTest();
  }

  @Test
  public void testNegativeCases2() {
    compilationHelper.addSourceFile("MissingFailNegativeCases2.java").doTest();
  }

  @Test
  public void testFailImport() {
    BugCheckerRefactoringTestHelper.newInstance(MissingFail.class, getClass())
        .addInputLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException expected) {}",
            "  }",
            "}")
        .addOutputLines(
            "test/A.java",
            "package test;",
            "import static org.junit.Assert.fail;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "      fail(\"Expected IllegalArgumentException\");",
            "    } catch (IllegalArgumentException expected) {}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void testFailMessageMultiCatch() {
    BugCheckerRefactoringTestHelper.newInstance(MissingFail.class, getClass())
        .addInputLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException expected) {}",
            "  }",
            "}")
        .addOutputLines(
            "test/A.java",
            "package test;",
            "import static org.junit.Assert.fail;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "      fail(\"Expected Exception\");",
            "    } catch (IllegalArgumentException | IllegalStateException expected) {}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  // verify that exceptions not named 'expected' are ignored
  @Test
  public void testToleratedException() {
    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException tolerated) {}",
            "  }",
            "}")
        .doTest();
  }

  // verify that exceptions not named 'expected' are ignored
  @Test
  public void testToleratedExceptionWithAssert() {
    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException tolerated) {",
            "      assertDummy();",
            "    }",
            "  }",
            "  static void assertDummy() {}",
            "}")
        .doTest();
  }

  @Test
  public void assertThrowsCatchBlock() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "  @Test",
            "  public void g() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException e = assertThrows(IOException.class, () -> {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "    });",
            "    assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "  }",
            "  @Test",
            "  public void g() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException e = assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest();
  }

  @Test
  public void assertThrowsEmptyCatch() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException expected) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest();
  }

  @Test
  public void emptyTry() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "abstract class ExceptionTest {",
            "  abstract AutoCloseable c();",
            "  @Test",
            "  public void test() {",
            "    try (AutoCloseable c = c()) {",
            "    } catch (Exception expected) {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noEnclosingMethod() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "abstract class ExceptionTest {",
            "  abstract void c();",
            "  {",
            "    try {",
            "      c();",
            "    } catch (Exception expected) {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
