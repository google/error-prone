/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class JUnitIncompatibleTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitIncompatibleType.class, getClass());

  @Test
  public void assertEquals_mismatched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertEquals;",
            "import static org.junit.Assert.assertNotEquals;",
            "class Test {",
            "  public void test() {",
            "    // BUG: Diagnostic contains:",
            "    assertEquals(1, \"\");",
            "    // BUG: Diagnostic contains:",
            "    assertEquals(\"foo\", 1, \"\");",
            "    // BUG: Diagnostic contains:",
            "    assertNotEquals(1, \"\");",
            "    // BUG: Diagnostic contains:",
            "    assertNotEquals(\"foo\", 1, \"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertEquals_matched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertEquals;",
            "import static org.junit.Assert.assertNotEquals;",
            "class Test {",
            "  public void test() {",
            "    assertEquals(1, 2);",
            "    assertEquals(1, 2L);",
            "    assertEquals(\"foo\", 1, 2);",
            "    assertNotEquals(1, 2);",
            "    assertNotEquals(\"foo\", 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertArrayEquals_mismatched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertArrayEquals;",
            "final class Test {",
            "  public void test() {",
            "    // BUG: Diagnostic contains:",
            "    assertArrayEquals(new Test[]{}, new String[]{\"\"});",
            "    // BUG: Diagnostic contains:",
            "    assertArrayEquals(\"foo\", new Test[]{}, new String[]{\"\"});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assertArrayEquals_primitiveOverloadsFine() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.assertArrayEquals;",
            "final class Test {",
            "  public void test() {",
            "    assertArrayEquals(new long[]{1L}, new long[]{2L});",
            "  }",
            "}")
        .doTest();
  }
}
