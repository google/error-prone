/*
 * Copyright 2016 The Error Prone Authors.
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

/** {@link StaticGuardedByInstance}Test */
@RunWith(JUnit4.class)
public class StaticGuardedByInstanceTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(StaticGuardedByInstance.class, getClass());
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  final Object lock = new Object();",
            "  static boolean init = false;",
            "  void m() {",
            "    synchronized (lock) {",
            "      // BUG: Diagnostic contains:",
            "      // static variable should not be guarded by instance lock 'lock'",
            "      init = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_twoWrites() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  final Object lock = new Object();",
            "  static int x = 0;",
            "  void m() {",
            "    synchronized (lock) {",
            "      // BUG: Diagnostic contains:",
            "      // static variable should not be guarded by instance lock 'lock'",
            "      x++;",
            "      // BUG: Diagnostic contains:",
            "      // static variable should not be guarded by instance lock 'lock'",
            "      x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_staticLock() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final Object lock = new Object();",
            "  static boolean init = false;",
            "  void m() {",
            "    synchronized (lock) {",
            "      init = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_instanceVar() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  final Object lock = new Object();",
            "  boolean init = false;",
            "  void m() {",
            "    synchronized (lock) {",
            "      init = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_method() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static boolean init = false;",
            "  void m() {",
            "    synchronized (getClass()) {",
            "      init = true;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nested() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  final Object lock = new Object();",
            "  static boolean init = false;",
            "  void m() {",
            "    synchronized (lock) {",
            "      synchronized (Test.class) {",
            "        init = true;",
            "      }",
            "      new Test() {{",
            "        init = true;",
            "      }};",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
