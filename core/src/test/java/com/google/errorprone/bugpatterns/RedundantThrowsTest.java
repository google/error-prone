/*
 * Copyright 2016 The Error Prone Authors.
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

/** {@link RedundantThrows}Test */
@RunWith(JUnit4.class)
public final class RedundantThrowsTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(RedundantThrows.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.io.FileNotFoundException;",
            "import java.nio.file.AccessDeniedException;",
            "interface Test {",
            "  // BUG: Diagnostic contains: FileNotFoundException is a subtype of IOException",
            "  void f() throws FileNotFoundException, IOException;",
            "  // BUG: Diagnostic contains: FileNotFoundException is a subtype of IOException",
            "  void g() throws IOException, FileNotFoundException;",
            "}")
        .doTest();
  }

  @Test
  public void positiveTwoSubtypes() {
    testHelper
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains:"
                + " IllegalAccessException and NoSuchFieldException are subtypes of"
                + " ReflectiveOperationException",
            "  void f() throws IllegalAccessException, NoSuchFieldException,"
                + " ReflectiveOperationException;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "interface Test {",
            "  void f() throws NullPointerException, FileNotFoundException;",
            "}")
        .doTest();
  }

  @Test
  public void transitiveSuper() {
    BugCheckerRefactoringTestHelper.newInstance(new RedundantThrows(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "import java.io.FileNotFoundException;",
            "import java.nio.file.AccessDeniedException;",
            "interface Test {",
            "  void f() throws FileNotFoundException, IOException, AccessDeniedException;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "import java.io.FileNotFoundException;",
            "import java.nio.file.AccessDeniedException;",
            "interface Test {",
            "  void f() throws IOException;",
            "}")
        .doTest();
  }
}
