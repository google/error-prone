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

/**
 * Test for {@link com.google.errorprone.annotations.concurrent.LockMethod} and {@link
 * com.google.errorprone.annotations.concurrent.UnlockMethod}
 */
@RunWith(JUnit4.class)
public class GuardedByLockMethodTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(GuardedByChecker.class, getClass());
  }

  @Test
  public void testSimple() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "import com.google.errorprone.annotations.concurrent.LockMethod;",
            "import com.google.errorprone.annotations.concurrent.UnlockMethod;",
            "import java.util.concurrent.locks.Lock;",
            "class Test {",
            "  final Lock lock = null;",
            "  @GuardedBy(\"lock\")",
            "  int x;",
            "  @LockMethod(\"lock\")",
            "  void lock() {",
            "    lock.lock();",
            "  }",
            "  @UnlockMethod(\"lock\")",
            "  void unlock() {",
            "    lock.unlock();",
            "  }",
            "  void m() {",
            "    lock();",
            "    try {",
            "      x++;",
            "    } finally {",
            "      unlock();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
