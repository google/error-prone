/*
 * Copyright 2023 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class ClosingStandardOutputStreamsTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ClosingStandardOutputStreams.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.OutputStreamWriter;",
            "import java.io.PrintWriter;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.err), true))"
                + " {",
            "        pw.println(\"hello\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.OutputStreamWriter;",
            "import java.io.PrintWriter;",
            "class Test {",
            "  void f(OutputStreamWriter os) {",
            "    System.err.println(42);",
            "    try (PrintWriter pw = new PrintWriter(os, true)) {",
            "        pw.println(\"hello\");",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
