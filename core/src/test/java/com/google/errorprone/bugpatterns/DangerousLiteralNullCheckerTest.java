/*
 * Copyright 2020 The Error Prone Authors.
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

/** Tests for {@link DangerousLiteralNullChecker}. */
@RunWith(JUnit4.class)
public class DangerousLiteralNullCheckerTest {
  private CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DangerousLiteralNullChecker.class, getClass());

  @Test
  public void javaUtilOptional() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "",
            "class Test {",
            "  void bad(Optional<Object> o) {",
            "    // BUG: Diagnostic contains: o.orElse(null)",
            "    o.orElseGet(null);",
            "    // BUG: Diagnostic contains: o.orElseThrow(NullPointerException::new)",
            "    o.orElseThrow(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "",
            "class Test {",
            "  void fine(Optional<Object> o) {",
            "    o.orElseGet(() -> null);", // lame, but behaves as expected so we let it slide
            "    o.orElseThrow(() -> new RuntimeException());",
            "  }",
            "}")
        .doTest();
  }
}
