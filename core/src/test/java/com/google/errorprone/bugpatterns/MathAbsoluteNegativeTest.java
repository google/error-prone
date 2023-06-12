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

/**
 * @author kayco@google.com (Kayla Walker)
 */
@RunWith(JUnit4.class)
public class MathAbsoluteNegativeTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MathAbsoluteNegative.class, getClass());

  @Test
  public void random() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "  private static final Random random = new Random();",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    Math.abs(random.nextInt());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void randomWithBounds() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "  private static final Random random = new Random();",
            "  void f() {",
            "    Math.abs(random.nextInt(10));",
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
            "    Math.abs(-9549451);",
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
            "    Math.abs(Math.sin(0) * 10.0);",
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

  @Test
  public void negativeDouble() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "  void f() {",
            "    double random = Math.abs(new Random().nextDouble());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashAsInt() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.hash.Hashing.goodFastHash;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    int foo = Math.abs(goodFastHash(64).hashUnencodedChars(\"\").asInt());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashAsLong() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.hash.Hashing.goodFastHash;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long foo = Math.abs(goodFastHash(64).hashUnencodedChars(\"\").asLong());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void hashPadToLong() {
    helper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.hash.Hashing.goodFastHash;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long foo = Math.abs(goodFastHash(64).hashUnencodedChars(\"\").padToLong());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void objectHashCode() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(String s) {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long foo = Math.abs(s.hashCode());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void identityHashCode() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(Object o) {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long foo = Math.abs(System.identityHashCode(o));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void uuid() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.UUID;",
            "class Test {",
            "  void f(UUID uuid) {",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long foo = Math.abs(uuid.getLeastSignificantBits());",
            "    // BUG: Diagnostic contains: MathAbsoluteNegative",
            "    long bar = Math.abs(uuid.getMostSignificantBits());",
            "  }",
            "}")
        .doTest();
  }
}
