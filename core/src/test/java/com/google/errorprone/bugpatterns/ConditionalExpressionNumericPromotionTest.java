/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ConditionalExpressionNumericPromotion}Test */
@RunWith(JUnit4.class)
public class ConditionalExpressionNumericPromotionTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new ConditionalExpressionNumericPromotion(), getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.io.Serializable;",
            "class Test {",
            "  Object returnObject(boolean b) {",
            "    return b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  Number returnNumber(boolean b) {",
            "    // Extra parentheses, just for fun.",
            "    return (b ? Integer.valueOf(0) : Long.valueOf(0));",
            "  }",
            "  Serializable returnSerializable(boolean b) {",
            "    return b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void assignObject(boolean b, Object obj) {",
            "    obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void assignNumber(boolean b, Number obj) {",
            "    obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void variableObject(boolean b) {",
            "    Object obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void variableNumber(boolean b) {",
            "    Number obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void invokeMethod(boolean b, Number n) {",
            "    invokeMethod(b, b ? Integer.valueOf(0) : Long.valueOf(0));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.Serializable;",
            "class Test {",
            "  Object returnObject(boolean b) {",
            "    return b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  Number returnNumber(boolean b) {",
            "    // Extra parentheses, just for fun.",
            "    return (b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0)));",
            "  }",
            "  Serializable returnSerializable(boolean b) {",
            "    return b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  void assignObject(boolean b, Object obj) {",
            "    obj = b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  void assignNumber(boolean b, Number obj) {",
            "    obj = b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  void variableObject(boolean b) {",
            "    Object obj = b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  void variableNumber(boolean b) {",
            "    Number obj = b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0));",
            "  }",
            "  void invokeMethod(boolean b, Number n) {",
            "    invokeMethod(b, b ? ((Number) Integer.valueOf(0)) : ((Number) Long.valueOf(0)));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  Long returnLong(boolean b) {",
            "    // OK, because the return type is the correct type.",
            "    return false ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void assignLong(boolean b, Long obj) {",
            "    obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void variableLong(boolean b) {",
            "    Long obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void variablePrimitive(boolean b) {",
            "    long obj = b ? Integer.valueOf(0) : Long.valueOf(0);",
            "  }",
            "  void invokeMethod(boolean b, Long n) {",
            "    invokeMethod(b, b ? Integer.valueOf(0) : Long.valueOf(0));",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
