/*
 * Copyright 2018 The Error Prone Authors.
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

/** Tests for {@link ComparingThisWithNull}. */
@RunWith(JUnit4.class)
public class ComparingThisWithNullTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ComparingThisWithNull.class, getClass());

  @Test
  public void thisIsNull() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ComparingThisWithNull",
            "    if (this == null) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullIsThis() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ComparingThisWithNull",
            "    if (null == this) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thisIsNotNull() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ComparingThisWithNull",
            "    if (this != null) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullIsNotThis() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ComparingThisWithNull",
            "    if (null != this) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void null_Negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    Object o = new Object();",
            "    if (null != o) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void this_Negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    Object o = new Object();",
            "    if (this != o) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullNot_Negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    Object o = new Object();",
            "    if (null == o) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void thisNot_Negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    Object o = new Object();",
            "    if (this == o) {",
            "     String x = \"Test\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
