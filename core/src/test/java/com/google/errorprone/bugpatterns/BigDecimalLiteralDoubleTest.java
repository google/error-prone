/*
 * Copyright 2016 The Error Prone Authors.
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
 * Tests for {@link BigDecimalLiteralDouble}.
 *
 * @author endobson@google.com (Eric Dobson)
 */
@RunWith(JUnit4.class)
public class BigDecimalLiteralDoubleTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(BigDecimalLiteralDouble.class, getClass());

  @Test
  public void exactlyRepresentable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.math.BigDecimal;",
            "class Test {",
            "  void test() {",
            "    new BigDecimal(\"99\");",
            "    new BigDecimal(\"99.0\");",
            "    new BigDecimal(123_459);",
            "    new BigDecimal(123_456L);",
            "    BigDecimal.valueOf(123);",
            "    BigDecimal.valueOf(123L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void losesPrecision() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.math.BigDecimal;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic matches: A",
            "    new BigDecimal(-0.012);",
            "    // BUG: Diagnostic matches: B",
            "    new BigDecimal(0.1f);",
            "    // BUG: Diagnostic matches: C",
            "    new BigDecimal(0.99);",
            "  }",
            "}")
        .expectErrorMessage(
            "A",
            message ->
                message.contains("-0.0120000000000000002498001805406602215953171253204345703125")
                    && message.contains("new BigDecimal(\"-0.012\")"))
        .expectErrorMessage(
            "B",
            message ->
                message.contains("0.100000001490116119384765625")
                    && message.contains("new BigDecimal(\"0.1\")"))
        .expectErrorMessage(
            "C",
            message ->
                message.contains("0.9899999999999999911182158029987476766109466552734375")
                    && message.contains("new BigDecimal(\"0.99\")"))
        .doTest();
  }
}
