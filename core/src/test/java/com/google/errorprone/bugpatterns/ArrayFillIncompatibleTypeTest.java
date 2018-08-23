/*
 * Copyright 2015 The Error Prone Authors.
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
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ArrayFillIncompatibleType} */
@RunWith(JUnit4.class)
public class ArrayFillIncompatibleTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ArrayFillIncompatibleType.class, getClass());

  @Test
  public void testPrimitiveBoxingIntoObject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  void something(boolean b, Object[] o) {",
            "     Arrays.fill(o, b);",
            "  }",
            "}")
        .setArgs(Arrays.asList("-source", "1.6", "-target", "1.6"))
        .doTest();
  }

  @Test
  public void testPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  void something(String b, Integer[] o, Number n) {",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, b);",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, 2.0d);",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, n);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTernary() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  enum Foo {BAR, BAZ};",
            "  void something(Foo[] o, String[] s) {",
            "     Arrays.fill(o, true ? Foo.BAR : Foo.BAZ);",
            "     Arrays.fill(s, true ? \"a\" : \"b\");",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(s, true ? \"a\" : 123);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBoxing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "class Test {",
            "  void something(int i, long l, Integer[] o) {",
            "     Arrays.fill(o, 1);",
            "     Arrays.fill(o, i);",
            "     Arrays.fill(o, 0, 4, i);",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, l);",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, 4L);",
            "     // BUG: Diagnostic contains: ",
            "     Arrays.fill(o, 0, 4, l);",
            "  }",
            "}")
        .doTest();
  }
}
