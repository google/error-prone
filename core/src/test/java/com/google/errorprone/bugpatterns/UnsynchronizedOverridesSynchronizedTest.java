/*
 * Copyright 2015 The Error Prone Authors.
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
  public void positive() {
    compilationHelper
        .addSourceLines(
            "test/Super.java", "package test;", "class Super {", "  synchronized void f() {}", "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test extends Super {",
            "  int counter;",
            "  // BUG: Diagnostic contains: f overrides synchronized method in Super",
            "  // synchronized void f()",
            "  void f() {",
            "    counter++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "test/Super.java", //
            "package test;",
            "class Super {",
            "  synchronized void f() {}",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test extends Super {",
            "  int counter;",
            "  synchronized void f() {",
            "    counter++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInputStream() {
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

  @Test
  public void callsSuperWithOtherStatements() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test {",
            "  class B extends Throwable {",
            "    // BUG: Diagnostic contains:",
            "    public Throwable getCause() {",
            "      System.err.println();",
            "      return super.getCause();",
            "    }",
            "  }",
            "  class C extends Throwable {",
            "    // BUG: Diagnostic contains:",
            "    public Exception getCause() {",
            "      System.err.println();",
            "      return (Exception) super.getCause();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreDelegatesToSuper() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test {",
            "  class B extends Throwable {",
            "    public Throwable getCause() {",
            "      return super.getCause();",
            "    }",
            "  }",
            "  class C extends Throwable {",
            "    public Exception getCause() {",
            "      return (Exception) super.getCause();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreEmptyOverride() {
    compilationHelper
        .addSourceLines(
            "test/Lib.java",
            "package test;",
            "class Lib {",
            "  public synchronized void f() {}",
            "}")
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "class Test {",
            "  class B extends Lib {",
            "    public void f() {",
            "    }",
            "  }",
            "  class C extends Lib {",
            "    public void f() {",
            "      super.f();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
