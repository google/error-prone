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

/** Tests for {@link RedundantCondition}. */
@RunWith(JUnit4.class)
public class RedundantConditionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RedundantCondition.class, getClass());

  @Test
  public void singleBinaryInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       // BUG: Diagnostic contains: RedundantCondition",
            "       boolean b = a && false;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleInitialization_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       boolean b = a;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleBinaryInitialization_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       boolean b = false;",
            "   } else {",
            "       boolean c = a && false;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleBinaryAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   boolean b = true;",
            "   if (a) { ",
            "       // BUG: Diagnostic contains: RedundantCondition",
            "       b = a && false;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleAssignment_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   boolean b = true;",
            "   if (a) { ",
            "       b = a;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleConditionalCheck() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       // BUG: Diagnostic contains: RedundantCondition",
            "       int b = a ? 1 : 0;",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleIfSymbol() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       // BUG: Diagnostic contains: RedundantCondition",
            "       if (a) {",
            "          return true;",
            "       }",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void singleIfSymbol_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   if (a) { ",
            "       a = false;",
            "       if (a) {",
            "          return true;",
            "       }",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void binaryIfSymbol() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   boolean b = false;",
            "   boolean c = false;",
            "   if (a && b) { ",
            "       // BUG: Diagnostic contains: RedundantCondition",
            "       if (a || c) {",
            "          return true;",
            "       }",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void binaryIfSymbol_Negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            " public boolean ifTest() {",
            "   boolean a = true;",
            "   boolean b = false;",
            "   boolean c = false;",
            "   if (a && b) { ",
            "       if (!a || c) {",
            "          return true;",
            "       }",
            "   }",
            "   return false;",
            " }",
            "}")
        .doTest();
  }
}
