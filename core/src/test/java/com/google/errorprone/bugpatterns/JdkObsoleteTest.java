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

/** {@link JdkObsolete}Test */
@RunWith(JUnit4.class)
public class JdkObsoleteTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(JdkObsolete.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    new java.util.LinkedList<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Stack<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Vector<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Hashtable<>();",
            "    // BUG: Diagnostic contains:",
            "    new StringBuffer();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringBuffer_appendReplacement() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.regex.Matcher;",
            "class Test {",
            "  void f(Matcher m) {",
            "    StringBuffer sb = new StringBuffer();",
            "    m.appendReplacement(sb, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringBuffer_appendTail() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.regex.Matcher;",
            "class Test {",
            "  void f(Matcher m) {",
            "    StringBuffer sb = new StringBuffer();",
            "    m.appendTail(sb);",
            "  }",
            "}")
        .doTest();
  }
}
