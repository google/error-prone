/*
 * Copyright 2021 The Error Prone Authors.
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

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link RemovedInJDK11}Test */
@RunWith(JUnit4.class)
public class RemovedInJDK11Test {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(RemovedInJDK11.class, getClass());

  @Test
  public void positive() {
    assumeFalse(RuntimeVersion.isAtLeast11());
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(SecurityManager sm, Thread t) {",
            "    // BUG: Diagnostic contains:",
            "    System.runFinalizersOnExit(false);",
            "    // BUG: Diagnostic contains:",
            "    Runtime.runFinalizersOnExit(false);",
            "    // BUG: Diagnostic contains:",
            "    sm.checkAwtEventQueueAccess();",
            "    // BUG: Diagnostic contains:",
            "    sm.checkMemberAccess(null, 0);",
            "    // BUG: Diagnostic contains:",
            "    sm.checkSystemClipboardAccess();",
            "    // BUG: Diagnostic contains:",
            "    sm.checkTopLevelWindow(null);",
            "    // BUG: Diagnostic contains:",
            "    t.stop(null);",
            "    // BUG: Diagnostic contains:",
            "    t.destroy();",
            "  }",
            "}")
        .doTest();
  }
}
