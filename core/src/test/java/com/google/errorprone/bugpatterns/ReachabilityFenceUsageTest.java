/*
 * Copyright 2017 The Error Prone Authors.
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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ReachabilityFenceUsage}Test */
@RunWith(JUnit4.class)
public final class ReachabilityFenceUsageTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(ReachabilityFenceUsage.class, getClass());

  @Test
  public void positive() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.ref.Reference;",
            "class Test {",
            "  public void run() {",
            "    // BUG: Diagnostic contains:",
            "    Reference.reachabilityFence(this);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.ref.Reference;",
            "class Test {",
            "  public void run() {",
            "    try {",
            "    } finally {",
            "      Reference.reachabilityFence(this);",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
