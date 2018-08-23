/*
 * Copyright 2017 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ExpectedExceptionChecker}Test. */
@RunWith(JUnit4.class)
public class ExpectedExceptionCheckerTest {

  @Test
  public void expect() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    if (true) {",
            "      Path p = Paths.get(\"NOSUCH\");",
            "      thrown.expect(IOException.class);",
            "      thrown.expect(CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "      thrown.expectCause(",
            "          CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "      thrown.expectMessage(\"error\");",
            "      thrown.expectMessage(CoreMatchers.containsString(\"error\"));",
            "      Files.readAllBytes(p);",
            "      assertThat(Files.exists(p)).isFalse();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    if (true) {",
            "      Path p = Paths.get(\"NOSUCH\");",
            "      IOException thrown =",
            "          assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "      assertThat(thrown,",
            "          CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "      assertThat(thrown.getCause(),",
            "          CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "      assertThat(thrown).hasMessageThat().contains(\"error\");",
            "      assertThat(thrown.getMessage(), CoreMatchers.containsString(\"error\"));",
            "      assertThat(Files.exists(p)).isFalse();",
            "    }",
            "  }",
            "}")
        // TODO(cushon): remove this once we update to a version of Truth that includes
        // hasMessageThat()
        .allowBreakingChanges()
        .doTest();
  }

  @Test
  public void noExceptionType() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "    Files.readAllBytes(p);",
            "    Files.readAllBytes(p);",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    Files.readAllBytes(p);",
            "    Throwable thrown = assertThrows(Throwable.class, () -> Files.readAllBytes(p));",
            "    assertThat(thrown, CoreMatchers.is(CoreMatchers.instanceOf(IOException.class)));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noExpectations() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "    Files.readAllBytes(p);",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonExpressionStatement() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "    Files.readAllBytes(p);",
            "    if (true) Files.readAllBytes(p);",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    Files.readAllBytes(p);",
            "    assertThrows(IOException.class, () -> {",
            "      if (true) Files.readAllBytes(p);",
            "    });",
            "  }",
            "}")
        .doTest();
  }

  // https://github.com/hamcrest/JavaHamcrest/issues/27
  @Test
  public void isA_hasCauseThat() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "    thrown.expectCause(CoreMatchers.isA(IOException.class));",
            "    thrown.expectCause(org.hamcrest.core.Is.isA(IOException.class));",
            "    Files.readAllBytes(p);",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.CoreMatchers;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException thrown =",
            "        assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(thrown).hasCauseThat().isInstanceOf(IOException.class);",
            "    assertThat(thrown).hasCauseThat().isInstanceOf(IOException.class);",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        // TODO(cushon): remove this once we update to a version of Truth that includes
        // hasCauseThat()
        .allowBreakingChanges()
        .doTest();
  }

  @Test
  public void typedMatcher() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.hamcrest.Matcher;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  Matcher<IOException> matcher;",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(matcher);",
            "    Files.readAllBytes(p);",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.hamcrest.MatcherAssert.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.hamcrest.Matcher;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  Matcher<IOException> matcher;",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException thrown =",
            "        assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(thrown, matcher);",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nothingButAsserts() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    thrown.expect(RuntimeException.class);",
            "    assertThat(false).isFalse();",
            "    assertThat(true).isTrue();",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    assertThat(false).isFalse();",
            "    assertThrows(RuntimeException.class, () -> assertThat(true).isTrue());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void removeExplicitFail() {
    BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionChecker(), getClass())
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.fail;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void testThrow() throws Exception {",
            "    thrown.expect(IOException.class);",
            "    throw new IOException();",
            "  }",
            "  @Test",
            "  public void one() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "    Files.readAllBytes(p);",
            "    assertThat(Files.exists(p)).isFalse();",
            "    fail();",
            "  }",
            "  @Test",
            "  public void two() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "    Files.readAllBytes(p);",
            "    assertThat(Files.exists(p)).isFalse();",
            "    throw new AssertionError();",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import static org.junit.Assert.fail;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Rule;",
            "import org.junit.Test;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void testThrow() throws Exception {",
            "    thrown.expect(IOException.class);",
            "    throw new IOException();",
            "  }",
            "  @Test",
            "  public void one() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "  @Test",
            "  public void two() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(Files.exists(p)).isFalse();",
            "  }",
            "}")
        .doTest();
  }
}
