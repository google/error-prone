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

/** {@link ExpectedExceptionRefactoring}Test */
@RunWith(JUnit4.class)
public class TryFailRefactoringTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(TryFailRefactoring.class, getClass());
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new TryFailRefactoring(), getClass());

  @Test
  public void catchBlock() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.fail;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f(String message) throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "      fail(message);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "  @Test",
            "  public void g() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      fail(\"expected exception not thrown\");",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import static org.junit.Assert.fail;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f(String message) throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException e = assertThrows(message, IOException.class, () -> {",
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
        .doTest();
  }

  @Test
  public void emptyCatch() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.fail;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      fail();",
            "    } catch (IOException e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import static org.junit.Assert.fail;",
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
        .doTest();
  }

  @Test
  public void tryWithResources() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.fail;",
            "import com.google.common.io.CharSource;",
            "import java.io.BufferedReader;",
            "import java.io.IOException;",
            "import java.io.PushbackReader;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f(String message, CharSource cs) throws IOException {",
            "    try (BufferedReader buf = cs.openBufferedStream();",
            "         PushbackReader pbr = new PushbackReader(buf)) {",
            "      pbr.read();",
            "      fail(message);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import static org.junit.Assert.fail;",
            "import com.google.common.io.CharSource;",
            "import java.io.BufferedReader;",
            "import java.io.IOException;",
            "import java.io.PushbackReader;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f(String message, CharSource cs) throws IOException {",
            "    try (BufferedReader buf = cs.openBufferedStream();",
            "         PushbackReader pbr = new PushbackReader(buf)) {",
            "      IOException e = assertThrows(message, IOException.class, () -> pbr.read());",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(TryFailRefactoring.class, getClass())
        .addSourceLines(
            "ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.fail;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void noFail() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "    }",
            "  }",
            "  @Test",
            "  public void unionCatch() throws Exception {",
            "    try {",
            "      ((Class<?>) null).newInstance();",
            "      fail();",
            "    } catch (IllegalAccessException | InstantiationException e) {",
            "    }",
            "  }",
            "  @Test",
            "  public void multiCatch() throws Exception {",
            "    try {",
            "      ((Class<?>) null).newInstance();",
            "      fail();",
            "    } catch (IllegalAccessException e) {",
            "    } catch (InstantiationException e) {",
            "    }",
            "  }",
            "  @Test",
            "  public void finallyBlock() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "    } finally {}",
            "  }",
            "  public void nonTestMethod() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      fail();",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void tryInStaticInitializer() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static {",
            "    try {",
            "      int a;",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
