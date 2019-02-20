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

/** Tests for {@link JodaToSelf}. */
@RunWith(JUnit4.class)
public class JodaToSelfTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaToSelf.class, getClass());

  @Test
  public void durationWithSeconds() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private static final Duration DUR = Duration.ZERO;",
            "  private static final Duration DUR = Duration.ZERO.toDuration();",
            "}")
        .doTest();
  }

  @Test
  public void durationWithSecondsNamedVariable() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final Duration DURATION1 = Duration.ZERO;",
            "  // BUG: Diagnostic contains: private static final Duration DURATION2 = DURATION1;",
            "  private static final Duration DURATION2 = DURATION1.toDuration();",
            "}")
        .doTest();
  }

  @Test
  public void durationWithSecondsInsideJodaTime() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.ZERO.toDuration();",
            "}")
        .doTest();
  }

  @Test
  public void durationWithSecondsPrimitiveImportClash() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final org.joda.time.Duration DURATION = ",
            "  // BUG: Diagnostic contains: org.joda.time.Duration.ZERO;",
            "      org.joda.time.Duration.ZERO.toDuration();",
            "}")
        .doTest();
  }

  @Test
  public void dateTimeConstructor() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTime;",
            "public class TestClass {",
            "  DateTime test(DateTime dt) {",
            "    // BUG: Diagnostic contains: return dt;",
            "    return new DateTime(dt);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dateTimeConstructorInstant() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.DateTime;",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  DateTime test(Instant i) {",
            "    return new DateTime(i);",
            "  }",
            "}")
        .doTest();
  }
}
