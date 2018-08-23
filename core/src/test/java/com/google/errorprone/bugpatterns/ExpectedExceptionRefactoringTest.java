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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ExpectedExceptionRefactoring}Test */
@RunWith(JUnit4.class)
public class ExpectedExceptionRefactoringTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ExpectedExceptionRefactoring(), getClass());

  @Test
  public void positive() {
    testHelper
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
            "    Files.readAllBytes(p);",
            "    Files.readAllBytes(p);",
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
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "    });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noThrowingStatements() {
    testHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Rule ExpectedException thrown = ExpectedException.none();",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    thrown.expect(IOException.class);",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "import org.junit.Rule;",
            "import org.junit.rules.ExpectedException;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "  }",
            "}")
        .doTest();
  }
}
