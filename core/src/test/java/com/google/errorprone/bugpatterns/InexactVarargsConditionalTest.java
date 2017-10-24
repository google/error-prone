/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link InexactVarargsConditional}Test */
@RunWith(JUnit4.class)
public class InexactVarargsConditionalTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InexactVarargsConditional.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  public static void main(String[] args) {",
            "    Object[] a = {1, 2};",
            "    Object b = \"hello\";",
            "    for (boolean flag : new boolean[]{true, false}) {",
            "      // BUG: Diagnostic contains: 'f(0, flag ? a : new Object[] {b});'?",
            "      f(0, flag ? a : b);",
            "    }",
            "  }",
            "  static void f(int x, Object... xs) {",
            "    System.err.println(Arrays.deepToString(xs));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  public static void main(String[] args) {",
            "    Object[] a = {1, 2};",
            "    Object b = \"hello\";",
            "    f(0, a);",
            "    f(0, b);",
            "    for (boolean flag : new boolean[]{true, false}) {",
            "      f(0, 1, flag ? a : b);",
            "    }",
            "  }",
            "  static void f(int x, Object... xs) {",
            "    System.err.println(Arrays.deepToString(xs));",
            "  }",
            "}")
        .doTest();
  }
}
