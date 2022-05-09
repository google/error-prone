/*
 * Copyright 2022 The Error Prone Authors.
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

/** {@link Interruption}Test */
@RunWith(JUnit4.class)
public class InterruptionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Interruption.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void f(Future<?> f, boolean b) {",
            "    // BUG: Diagnostic contains: f.cancel(false)",
            "    f.cancel(true);",
            "    // BUG: Diagnostic contains: f.cancel(false)",
            "    f.cancel(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInterrupt() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f(Thread t) {",
            "    // BUG: Diagnostic contains:",
            "    t.interrupt();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.Future;",
            "class Test {",
            "  void f(Future<?> f) {",
            "    f.cancel(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeWasInterrupted() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.util.concurrent.AbstractFuture;",
            "class Test extends AbstractFuture<Object> {",
            "  void f() {",
            "    cancel(wasInterrupted());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeDelegate() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.util.concurrent.AbstractFuture;",
            "import java.util.concurrent.Future;",
            "class Test extends AbstractFuture<Object> {",
            "  void f(Future<?> f) {",
            "    new AbstractFuture<Object>() {",
            "      @Override",
            "      public boolean cancel(boolean mayInterruptIfRunning) {",
            "        return f.cancel(mayInterruptIfRunning);",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInterrupt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Thread t) {",
            "    Thread.currentThread().interrupt();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInTestonlyCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import java.util.concurrent.Future;",
            "@RunWith(JUnit4.class)",
            "class FutureTest {",
            "  Future<?> f;",
            "  @Test",
            "  public void t() {",
            "    f.cancel(true);",
            "  }",
            "}")
        .setArgs("-XepCompilingTestOnlyCode")
        .doTest();
  }
}
