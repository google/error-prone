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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringCharsetTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringCharset.class, getClass());
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(StringCharset.class, getClass());

  @Test
  public void invalid() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(String s) throws Exception {",
            "    // BUG: Diagnostic contains: nosuch is not a valid charset",
            "    s.getBytes(\"nosuch\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f(String s) throws Exception {",
            "    new String(new byte[0], \"utf8\");",
            "    s.getBytes(\"latin1\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static java.nio.charset.StandardCharsets.ISO_8859_1;",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "class Test {",
            "  void f(String s) throws Exception {",
            "    new String(new byte[0], UTF_8);",
            "    s.getBytes(ISO_8859_1);",
            "  }",
            "}")
        .doTest();
  }
}
