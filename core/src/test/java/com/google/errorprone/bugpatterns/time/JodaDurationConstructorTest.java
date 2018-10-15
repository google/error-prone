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

/** Tests for {@link JodaDurationConstructor}. */
@RunWith(JUnit4.class)
public class JodaDurationConstructorTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaDurationConstructor.class, getClass());

  @Test
  public void durationStaticFactories() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration oneMilli = Duration.millis(1);",
            "  private static final Duration oneSec = Duration.standardSeconds(1);",
            "  private static final Duration oneMin = Duration.standardMinutes(1);",
            "  private static final Duration oneHour = Duration.standardHours(1);",
            "  private static final Duration oneDay = Duration.standardDays(1);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorObject() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration oneMilli = new Duration(\"1\");",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntInt() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration interval = new Duration(42, 48);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration interval = new Duration(42, 48L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongInt() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration interval = new Duration(42L, 48);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongLong() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration interval = new Duration(42L, 48L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorIntPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: use Duration.millis(long) instead",
            "  private static final Duration oneMilli = new Duration(1);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorInteger() {
    // TODO(kak): This really should be an error too :(
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration oneMilli = new Duration(new Integer(42));",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: use Duration.millis(long) instead",
            "  private static final Duration oneMilli = new Duration(1L);",
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
            "  private static final Duration oneMilli = new Duration(1L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLongPrimitiveNoImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  private static final org.joda.time.Duration oneMilli = ",
            "      // BUG: Diagnostic contains: Did you mean 'org.joda.time.Duration.millis(1L);'",
            "      new org.joda.time.Duration(1L);",
            "}")
        .doTest();
  }

  @Test
  public void durationConstructorLong() {
    // TODO(kak): This really should be an error too :(
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration oneMilli = new Duration(new Long(1L));",
            "}")
        .doTest();
  }
}
