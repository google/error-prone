/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InputStreamSlowMultibyteReadTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(InputStreamSlowMultibyteRead.class, getClass());
  }

  @Test
  public void doingItRight() {
    compilationHelper
        .addSourceLines(
            "TestClass.java",
            "class TestClass extends java.io.InputStream {",
            "  byte[] buf = new byte[42];",
            "  public int read(byte[] b, int a, int c) { return 0; }",
            "  public int read() { return buf[0]; }",
            "}")
        .doTest();
  }

  @Test
  public void basic() throws Exception {
    compilationHelper
        .addSourceLines(
            "TestClass.java",
            "class TestClass extends java.io.InputStream {",
            "  byte[] buf = new byte[42];",
            "  // BUG: Diagnostic contains:",
            "  public int read() { return buf[0]; }",
            "}")
        .doTest();
  }

  @Test
  public void dummyStreamIgnored() {
    compilationHelper
        .addSourceLines(
            "TestClass.java",
            "class TestClass extends java.io.InputStream {",
            "  public int read() { return 0; }",
            "}")
        .doTest();
  }

  // Here, the superclass still can't effectively multibyte-read without the underlying
  // read() method.
  @Test
  public void inherited() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "abstract class Super extends java.io.InputStream {",
            "  public int read(byte[] b, int a, int c) { return 0; }",
            "}")
        .addSourceLines(
            "TestClass.java",
            "class TestClass extends Super {",
            "  byte[] buf = new byte[42];",
            "  // BUG: Diagnostic contains:",
            "  public int read() { return buf[0]; }",
            "}")
        .doTest();
  }

  @Test
  public void junit3TestIgnored() {
    compilationHelper
        .addSourceLines(
            "javatests/TestClass.java",
            "class TestClass extends junit.framework.TestCase {",
            " static class SomeStream extends java.io.InputStream {",
            "  byte[] buf = new byte[42];",
            "  public int read() { return buf[0]; }",
            "}}")
        .doTest();
  }

  @Test
  public void junit4TestIgnored() {
    compilationHelper
        .addSourceLines(
            "javatests/TestClass.java",
            "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
            "class TestClass {",
            " static class SomeStream extends java.io.InputStream {",
            "  byte[] buf = new byte[42];",
            "  public int read() { return buf[0]; }",
            "}}")
        .doTest();
  }
}
