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
public class InlineTrivialConstantTest {
  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(InlineTrivialConstant.class, getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  private static final String EMPTY_STRING = \"\";",
            "  void f() {",
            "    System.err.println(EMPTY_STRING);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    System.err.println(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(InlineTrivialConstant.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static class NonPrivate {",
            "    static final String EMPTY_STRING = \"\";",
            "  }",
            "  static class NonStatic {",
            "    private final String EMPTY_STRING = \"\";",
            "  }",
            "  static class NonFinal {",
            "    private static String EMPTY_STRING = \"\";",
            "  }",
            "  static class NonString {",
            "    private static final Object EMPTY_STRING = \"\";",
            "  }",
            "  static class WrongName {",
            "    private static final String LAUNCH_CODES = \"\";",
            "  }",
            "  static class WrongValue {",
            "    private static final String EMPTY_STRING = \"hello\";",
            "  }",
            "}")
        .doTest();
  }
}
