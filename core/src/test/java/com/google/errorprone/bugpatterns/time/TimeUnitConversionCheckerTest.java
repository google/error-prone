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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TimeUnitConversionChecker}. */
@RunWith(JUnit4.class)
public class TimeUnitConversionCheckerTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TimeUnitConversionChecker.class, getClass());

  @Test
  public void literalConvertToSelf() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private long value = 42L /* milliseconds */;",
            "  private long value = TimeUnit.MILLISECONDS.toMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void literalConvertToSelf_withStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private long value = 42L /* milliseconds */;",
            "  private long value = MILLISECONDS.toMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void variableConvertToSelf_withStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "public class TestClass {",
            "  private long toConvert = 42;",
            "  // BUG: Diagnostic contains: private long value = toConvert /* milliseconds */;",
            "  private long value = MILLISECONDS.toMillis(toConvert);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToZero() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private static final long VALUE1 = 0L /* seconds */;",
            "  private static final long VALUE1 = TimeUnit.MILLISECONDS.toSeconds(4);",
            "  // BUG: Diagnostic contains: private static final long VALUE2 = 0L /* seconds */;",
            "  private static final long VALUE2 = TimeUnit.MILLISECONDS.toSeconds(400);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToZero_withStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private static final long VALUE1 = 0L /* seconds */;",
            "  private static final long VALUE1 = MILLISECONDS.toSeconds(4);",
            "  // BUG: Diagnostic contains: private static final long VALUE2 = 0L /* seconds */;",
            "  private static final long VALUE2 = MILLISECONDS.toSeconds(400);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToOne() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private static final long VALUE1 = 1L /* seconds */;",
            "  private static final long VALUE1 = TimeUnit.MILLISECONDS.toSeconds(1000);",
            "  // BUG: Diagnostic contains: private static final long VALUE2 = 1L /* seconds */;",
            "  private static final long VALUE2 = TimeUnit.MILLISECONDS.toSeconds(1999);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToNegativeOne() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE1 = TimeUnit.MILLISECONDS.toSeconds(-1000);",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE2 = TimeUnit.MILLISECONDS.toSeconds(-1999);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToLargeNumber() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE1 = TimeUnit.MILLISECONDS.toSeconds(4321);",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE2 = TimeUnit.MILLISECONDS.toSeconds(-4321);",
            "}")
        .doTest();
  }

  @Test
  public void expressionEvaluatesToLargeNumber_withStaticImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.MILLISECONDS;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE1 = MILLISECONDS.toSeconds(4321);",
            "  // BUG: Diagnostic contains: TimeUnitConversionChecker",
            "  private static final long VALUE2 = MILLISECONDS.toSeconds(-4321);",
            "}")
        .doTest();
  }

  @Test
  public void largeUnitToSmallUnit_succeeds() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import static java.util.concurrent.TimeUnit.SECONDS;",
            "public class TestClass {",
            "  private static final long VALUE1 = SECONDS.toMillis(4321);",
            "  private static final long VALUE2 = SECONDS.toMillis(-4321);",
            "}")
        .doTest();
  }
}
