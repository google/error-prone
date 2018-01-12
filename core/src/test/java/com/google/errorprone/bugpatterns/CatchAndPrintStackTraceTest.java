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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CatchAndPrintStackTrace}Test */
@RunWith(JUnit4.class)
public class CatchAndPrintStackTraceTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(CatchAndPrintStackTrace.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Throwable t) {",
            "      // BUG: Diagnostic contains:",
            "      t.printStackTrace();",
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
            "class Test {",
            "  void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Throwable t) {}",
            "    try {",
            "      System.err.println();",
            "    } catch (Throwable t) {",
            "      t.printStackTrace();",
            "      t.printStackTrace();",
            "    }",
            "    try {",
            "      System.err.println();",
            "    } catch (Throwable t) {",
            "      t.printStackTrace(System.err);",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
