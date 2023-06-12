/*
 * Copyright 2022 The Error Prone Authors.
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

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NewFileSystemTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(NewFileSystem.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(NewFileSystem.class, getClass());

  @Test
  public void refactoring() {
    assumeFalse(RuntimeVersion.isAtLeast13());
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.nio.file.FileSystems;",
            "import java.nio.file.Paths;",
            "import java.io.IOException;",
            "class Test {",
            "  void f() throws IOException {",
            "    FileSystems.newFileSystem(Paths.get(\".\"), null);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.nio.file.FileSystems;",
            "import java.nio.file.Paths;",
            "import java.io.IOException;",
            "class Test {",
            "  void f() throws IOException {",
            "    FileSystems.newFileSystem(Paths.get(\".\"), (ClassLoader) null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.FileSystems;",
            "import java.nio.file.Paths;",
            "import java.io.IOException;",
            "class Test {",
            "  void f() throws IOException {",
            "    FileSystems.newFileSystem(Paths.get(\".\"), (ClassLoader) null);",
            "  }",
            "}")
        .doTest();
  }
}
