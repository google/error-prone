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
public class UnnecessaryStringBuilderTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryStringBuilder.class, getClass());
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(UnnecessaryStringBuilder.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f(String hello) {",
            "    System.err.println(new StringBuilder().append(hello).append(\"world\"));",
            "    System.err.println(new StringBuilder(hello).append(\"world\"));",
            "    System.err.println(new StringBuilder(10).append(hello).append(\"world\"));",
            "    System.err.println(new StringBuilder(hello).append(\"world\").toString());",
            "    System.err.println(new StringBuilder().toString());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f(String hello) {",
            "    System.err.println(hello + \"world\");",
            "    System.err.println(hello + \"world\");",
            "    System.err.println(hello + \"world\");",
            "    System.err.println(hello + \"world\");",
            "    System.err.println(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f(String hello) {",
            "    String a = new StringBuilder().append(hello).append(\"world\").toString();",
            "    StringBuilder b = new StringBuilder().append(hello).append(\"world\");",
            "    StringBuilder c = new StringBuilder().append(hello).append(\"world\");",
            "    System.err.println(b);",
            "    System.err.println(b + \"\");",
            "    System.err.println(c);",
            "    c.append(\"goodbye\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f(String hello) {",
            "    String a = hello + \"world\";",
            "    String b = hello + \"world\";",
            "    StringBuilder c = new StringBuilder().append(hello).append(\"world\");",
            "    System.err.println(b);",
            "    System.err.println(b + \"\");",
            "    System.err.println(c);",
            "    c.append(\"goodbye\");",
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
            "  void f(Iterable<String> xs) {",
            "    StringBuilder sb = new StringBuilder();",
            "    for (String s : xs) {",
            "      sb.append(s);",
            "    }",
            "    System.err.println(sb);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeMethodReference() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Iterable<String> xs) {",
            "    StringBuilder sb = new StringBuilder();",
            "    xs.forEach(sb::append);",
            "    System.err.println(sb);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void needsParens() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void g(String x);",
            "  void f(boolean b, String hello) {",
            "    g(new StringBuilder().append(b ? hello : \"\").append(\"world\").toString());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void g(String x);",
            "  void f(boolean b, String hello) {",
            "    g((b ? hello : \"\") + \"world\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "abstract class Test {",
            "  void f() {",
            "    var sb = new StringBuilder().append(\"hello\");",
            "    System.err.println(sb);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "abstract class Test {",
            "  void f() {",
            "    var sb = \"hello\";",
            "    System.err.println(sb);",
            "  }",
            "}")
        .doTest();
  }
}
