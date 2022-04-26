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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link NegativeCharLiteral}Test */
@RunWith(JUnit4.class)
public class NegativeCharLiteralTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NegativeCharLiteral.class, getClass());

  @Test
  public void positive_literalNegativeOne() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: 'char x = Character.MAX_VALUE;'",
            "  char x = (char) -1;",
            "}")
        .doTest();
  }

  @Test
  public void positive_literalNegativeTwo() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: 'char x = Character.MAX_VALUE - 1;'",
            "  char x = (char) -2;",
            "}")
        .doTest();
  }

  @Test
  public void positive_literalOneLessThanMultipleOf65536() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: 'char x = Character.MAX_VALUE;'",
            "  char x = (char) -65537;",
            "}")
        .doTest();
  }

  @Test
  public void positive_longLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: 'char x = Character.MAX_VALUE;'",
            "  char x = (char) -1L;",
            "}")
        .doTest();
  }

  @Test
  public void positive_multipleOverflow() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: 'char x = Character.MAX_VALUE - 38527;'",
            "  char x = (char) -10000000;",
            "}")
        .doTest();
  }

  @Test
  public void negative_zeroLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  char x = (char) 0;",
            "}")
        .doTest();
  }

  @Test
  public void negative_positiveLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  char x = (char) 1;",
            "}")
        .doTest();
  }

  @Test
  public void negative_castToTypeNotChar() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  int x = (int) -1;",
            "}")
        .doTest();
  }
}
