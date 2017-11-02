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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
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
            "    // BUG: Diagnostic contains:",
            "    new java.util.Hashtable<Object, Object>() {};",
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

  @Test
  public void positiveExtends() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  abstract class A implements java.util.Enumeration<Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class B implements java.util.SortedSet<Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class C implements java.util.SortedMap<Object, Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class D extends java.util.Dictionary<Object, Object> {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new JdkObsolete(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import java.util.*;",
            "class Test {",
            "  Deque<Object> d = new LinkedList<>();",
            "  List<Object> l = new LinkedList<>();",
            "  LinkedList<Object> ll = new LinkedList<>();",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import java.util.*;",
            "class Test {",
            "  Deque<Object> d = new ArrayDeque<>();",
            "  List<Object> l = new ArrayList<>();",
            "  LinkedList<Object> ll = new LinkedList<>();",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void stringBufferRefactoringTest() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new JdkObsolete(), getClass())
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  String f() {",
            "    StringBuffer sb = new StringBuffer();",
            "    return sb.append(42).toString();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "class Test {",
            "  String f() {",
            "    StringBuilder sb = new StringBuilder();",
            "    return sb.append(42).toString();",
            "  }",
            "}")
        .doTest();
  }
}
