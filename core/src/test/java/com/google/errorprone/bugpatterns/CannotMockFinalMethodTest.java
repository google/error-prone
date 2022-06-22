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

@RunWith(JUnit4.class)
public final class CannotMockFinalMethodTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CannotMockFinalMethod.class, getClass());

  @Test
  public void whenCall_flagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  final Integer foo() {",
            "    return 1;",
            "  }",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    when(this.foo());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void verifyCall_flagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  final Integer foo() {",
            "    return 1;",
            "  }",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    verify(this.foo());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  Integer foo() {",
            "    return 1;",
            "  }",
            "  void test() {",
            "    when(this.foo());",
            "  }",
            "}")
        .doTest();
  }
}
