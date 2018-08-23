/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link SynchronizeOnNonFinalFieldTest}Test */
@RunWith(JUnit4.class)
public class SynchronizeOnNonFinalFieldTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(SynchronizeOnNonFinalField.class, getClass());
  }

  @Test
  public void testPositive1() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "class Test {",
            "  Object lock = new Object();",
            "  void m() {",
            "    // BUG: Diagnostic contains: Synchronizing on non-final fields is not safe",
            "    synchronized (lock) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositive2() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "class Test {",
            "  Object lock = new Object();",
            "  Test[] tx = null;",
            "  void m(int i) {",
            "    // BUG: Diagnostic contains: Synchronizing on non-final fields is not safe",
            "    synchronized (this.tx[i].lock) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositive3() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "class Test {",
            "  Object lock = new Object();",
            "  void m(Test t) {",
            "    // BUG: Diagnostic contains: Synchronizing on non-final fields is not safe",
            "    synchronized (t.lock) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "class Test {",
            "  final Object lock = new Object();",
            "  void m() {",
            "    synchronized (lock) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_lazyInit() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety.Test;",
            "import com.google.errorprone.annotations.concurrent.LazyInit;",
            "class Test {",
            "  @LazyInit transient Object lock = new Object();",
            "  void m() {",
            "    synchronized (lock) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_Writer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.Writer;",
            "abstract class Test extends Writer {",
            "  void m() {",
            "    synchronized (lock) {}",
            "  }",
            "}")
        .doTest();
  }
}
