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

/** @author kayco@google.com (Kayla Walker) */
@RunWith(JUnit4.class)
public class MathAbsoluteRandomTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MathAbsoluteRandom.class, getClass());

  @Test
  public void math() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteRandom",
            "    Math.abs(Math.random()); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void random() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "private static final Random random = new Random();",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteRandom",
            "    Math.abs(random.nextInt()); ",
            "  }",
            "}")
        .doTest();
  }


  @Test
  public void negativeNumber() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Math.abs(-9549451); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeMethod() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Math.abs(Math.sin(0) * 10.0); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    long random = Math.round(Math.random() * 10000);",
            "  }",
            "}")
        .doTest();
  }
}
