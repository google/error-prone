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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MathRoundIntLong}. */
@RunWith(JUnit4.class)
public class MathRoundIntLongTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new MathRoundIntLong(), getClass());

  @Test
  public void deleteRoundMethodInt() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    int y = Math.round(3);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    int y = 3;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void deleteRoundMethodIntClass() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Integer i = new Integer(3);",
            "    int y = Math.round(i);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Integer i = new Integer(3);",
            "    int y = i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replaceRoundMethodLong() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    long l = 3L;",
            "    int y = Math.round(l);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import com.google.common.primitives.Ints;",
            "class Test {",
            "  void f() {",
            "    long l = 3L;",
            "    int y = Ints.saturatedCast(l);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replaceRoundMethodLongClass() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Long l = new Long(\"3\");",
            "    int y = Math.round(l);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import com.google.common.primitives.Ints;",
            "class Test {",
            "  void f() {",
            "    Long l = new Long(\"3\");",
            "    int y = Ints.saturatedCast(l);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void roundingFloatNegative() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Float f = new Float(\"3\");",
            "    int y = Math.round(f);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void roundingDoubleNegative() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    Double d = new Double(\"3\");",
            "    Long y = Math.round(d);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void replaceRoundMethodAddParenthesis() {
    helper
        .addInputLines(
            "Test.java", //
            "import com.google.common.primitives.Ints;",
            "class Test {",
            "  void f() {",
            "    long l = 3L;",
            "    long x = 6L;",
            "    int y = Math.round(l/x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import com.google.common.primitives.Ints;",
            "class Test {",
            "  void f() {",
            "    long l = 3L;",
            "    long x = 6L;",
            "    int y = Ints.saturatedCast(l/x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void removeMathRoundLeaveParenthesisIfUnary() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    int y = Math.round(1 + 3) * 3;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void f() {",
            "    int y = (1 + 3) * 3;",
            "  }",
            "}")
        .doTest();
  }
}
