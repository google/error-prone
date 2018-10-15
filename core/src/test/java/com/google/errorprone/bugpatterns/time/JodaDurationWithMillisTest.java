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

/** Tests for {@link JodaDurationWithMillis}. */
@RunWith(JUnit4.class)
public class JodaDurationWithMillisTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaDurationWithMillis.class, getClass());

  @Test
  public void durationMillis() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.millis(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithMillisIntPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: use Duration.millis(long) instead",
            "  private static final Duration DURATION = Duration.ZERO.withMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithMillisLongPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: use Duration.millis(long) instead",
            "  private static final Duration DURATION = Duration.ZERO.withMillis(42L);",
            "}")
        .doTest();
  }

  @Test
  public void durationStandardHoursWithMillisLongPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: use Duration.millis(long) instead",
            "  private static final Duration DURATION = Duration.standardHours(1).withMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntPrimitiveInsideJoda() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.ZERO.withMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitiveInsideJoda() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.ZERO.withMillis(42L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitiveImportClash() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private static final org.joda.time.Duration DURATION = ",
            "      // BUG: Diagnostic contains: Did you mean 'org.joda.time.Duration.millis(42L);'",
            "      org.joda.time.Duration.ZERO.withMillis(42L);",
            "}")
        .doTest();
  }
}
