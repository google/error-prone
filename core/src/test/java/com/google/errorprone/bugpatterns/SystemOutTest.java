/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link SystemOut}. */
@RunWith(JUnit4.class)
public class SystemOutTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(SystemOut.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.io.PrintStream;",
            "",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: SystemOut",
            "    System.out.println();",
            "    // BUG: Diagnostic contains: SystemOut",
            "    System.err.println();",
            "    // BUG: Diagnostic contains: SystemOut",
            "    p(System.out);",
            "    // BUG: Diagnostic contains: SystemOut",
            "    p(System.err);",
            "    // BUG: Diagnostic contains: SystemOut",
            "    Thread.dumpStack();",
            "    // BUG: Diagnostic contains: SystemOut",
            "    new Exception().printStackTrace();",
            "  }",
            "  private void p(PrintStream ps) {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "",
            "class Test {",
            "  void f() {",
            "    new Exception().printStackTrace(new PrintStream((OutputStream) null));",
            "    new Exception().printStackTrace(new PrintWriter((OutputStream) null));",
            "  }",
            "}")
        .doTest();
  }
}
