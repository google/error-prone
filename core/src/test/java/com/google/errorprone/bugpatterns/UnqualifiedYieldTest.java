/*
 * Copyright 2013 The Error Prone Authors.
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
public class UnqualifiedYieldTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnqualifiedYield.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void yield() {}",
            "  {",
            "    // BUG: Diagnostic contains: this.yield();",
            "    yield();",
            "  }",
            "}")
        .setArgs("--release", "11")
        .doTest();
  }

  @Test
  public void enclosing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class A {",
            "    void yield() {}",
            "    class B {",
            "      class C {",
            "        {",
            "          // BUG: Diagnostic contains: A.this.yield();",
            "          yield();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs("--release", "11")
        .doTest();
  }

  @Test
  public void staticMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static void yield() {}",
            "  static class I {",
            "    {",
            "      // BUG: Diagnostic contains: Test.yield();",
            "      yield();",
            "    }",
            "  }",
            "}")
        .setArgs("--release", "11")
        .doTest();
  }
}
