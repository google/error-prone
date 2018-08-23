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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AssertionFailureIgnored}Test */
@RunWith(JUnit4.class)
public class AssertionFailureIgnoredTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(AssertionFailureIgnored.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import org.junit.Assert;",
            "class Test {",
            "  void f() {",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      Assert.fail();",
            "    } catch (Throwable t) {",
            "    }",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "    }",
            "    try {",
            "      if (true) throw new NoSuchFieldException();",
            "      if (true) throw new NoSuchMethodException();",
            "      // BUG: Diagnostic contains:",
            "      Assert.fail();",
            "    } catch (NoSuchFieldException | NoSuchMethodException | AssertionError t) {",
            "    }",
            "    AssertionError e = null;",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      throw e;",
            "    }",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      throw new AssertionError(e);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import org.junit.Assert;",
            "import java.io.IOException;",
            "class Test {",
            "  void f() {",
            "    try {",
            "      if (true) throw new IOException();",
            "      Assert.fail();",
            "    } catch (IOException t) {",
            "    }",
            "    try {",
            "      Assert.fail();",
            "    } catch (Exception t) {",
            "    }",
            "    try {",
            "      if (true) throw new NoSuchFieldException();",
            "      if (true) throw new NoSuchMethodException();",
            "      Assert.fail();",
            "    } catch (NoSuchFieldException | NoSuchMethodException t) {",
            "    }",
            "    try {",
            "    } catch (Throwable t) {",
            "      Assert.fail();",
            "    }",
            "    try {",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      throw t;",
            "    }",
            "    try {",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      throw new AssertionError(t);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new AssertionFailureIgnored(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import org.junit.Assert;",
            "import java.io.IOException;",
            "import static com.google.common.truth.Truth.assertThat;",
            "class Test {",
            "  void f() {",
            "    try {",
            "      System.err.println();",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      assertThat(t).isInstanceOf(AssertionError.class);",
            "    }",
            "    try {",
            "      System.err.println();",
            "      Assert.fail();",
            "    } catch (AssertionError e) {",
            "    }",
            "    try {",
            "      if (true) throw new IOException();",
            "      Assert.fail();",
            "    } catch (AssertionError e) {",
            "    } catch (Exception e) {",
            "    }",
            "    try {",
            "      if (true) throw new NoSuchFieldException();",
            "      if (true) throw new NoSuchMethodException();",
            "      Assert.fail();",
            "    } catch (AssertionError | NoSuchFieldException | NoSuchMethodException e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import org.junit.Assert;",
            "class Test {",
            "  void f() {",
            "    AssertionError t = assertThrows(AssertionError.class, () -> ",
            "      System.err.println());",
            "    assertThat(t).isInstanceOf(AssertionError.class);",
            "    assertThrows(AssertionError.class, () -> ",
            "      System.err.println());",
            "    try {",
            "      if (true) throw new IOException();",
            "      Assert.fail();",
            "    } catch (AssertionError e) {",
            "    } catch (Exception e) {",
            "    }",
            "    try {",
            "      if (true) throw new NoSuchFieldException();",
            "      if (true) throw new NoSuchMethodException();",
            "      Assert.fail();",
            "    } catch (AssertionError | NoSuchFieldException | NoSuchMethodException e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringStatements() {
    BugCheckerRefactoringTestHelper.newInstance(new AssertionFailureIgnored(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import org.junit.Assert;",
            "import java.io.IOException;",
            "import static com.google.common.truth.Truth.assertThat;",
            "class Test {",
            "  void f() {",
            "    try {",
            "      System.err.println();",
            "      System.err.println();",
            "      Assert.fail();",
            "    } catch (AssertionError t) {",
            "      assertThat(t).isInstanceOf(AssertionError.class);",
            "    }",
            "    try {",
            "      System.err.println();",
            "      System.err.println();",
            "      Assert.fail();",
            "    } catch (AssertionError e) {",
            "    }",
            "    try {",
            "      if (true) throw new AssertionError();",
            "      Assert.fail();",
            "    } catch (AssertionError e) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import org.junit.Assert;",
            "class Test {",
            "  void f() {",
            "    AssertionError t = assertThrows(AssertionError.class, () -> {",
            "      System.err.println();",
            "      System.err.println();",
            "    });",
            "    assertThat(t).isInstanceOf(AssertionError.class);",
            "    assertThrows(AssertionError.class, () -> {",
            "      System.err.println();",
            "      System.err.println();",
            "    });",
            "    assertThrows(AssertionError.class, () -> {",
            "      if (true) throw new AssertionError();",
            "    });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void union() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import org.junit.Assert;",
            "import java.io.IOError;",
            "class Test {",
            "  void f() {",
            "    try {",
            "      if (true) throw new NullPointerException();",
            "      Assert.fail();",
            "    } catch (NullPointerException | IOError t) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
