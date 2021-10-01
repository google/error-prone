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

package com.google.errorprone.bugpatterns.apidiff;

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Java8ApiChecker}Test */
@RunWith(JUnit4.class)
public class Java8ApiCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Java8ApiChecker.class, getClass());

  @Test
  public void positive() {
    assumeTrue(RuntimeVersion.isAtLeast11());
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  boolean f(Optional<String> o) {",
            "    // BUG: Diagnostic contains: java.util.Optional#isEmpty() is not available",
            "    return o.isEmpty();",
            "  }",
            "}")
        .doTest();
  }
}
