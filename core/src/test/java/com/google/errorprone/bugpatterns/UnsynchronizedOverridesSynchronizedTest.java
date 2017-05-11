/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** {@link UnsynchronizedOverridesSynchronized}Test */
@RunWith(JUnit4.class)
public class UnsynchronizedOverridesSynchronizedTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(UnsynchronizedOverridesSynchronized.class, getClass());
  }

  @Test
  public void positive() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Super.java", "package test;", "class Super {", "  synchronized void f() {}", "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test extends Super {",
            "  // BUG: Diagnostic contains: f overrides synchronized method in Super",
            "  // synchronized void f()",
            "  void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Super.java", "package test;", "class Super {", "  synchronized void f() {}", "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test extends Super {",
            "  synchronized void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInputStream() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.io.InputStream;",
            "import java.io.IOException;",
            "class Test extends InputStream {",
            "  @Override public int read() throws IOException {",
            "    throw new IOException();",
            "  }",
            "  @Override public /*unsynchronized*/ void mark(int readlimit) {}",
            "}")
        .doTest();
  }
}
