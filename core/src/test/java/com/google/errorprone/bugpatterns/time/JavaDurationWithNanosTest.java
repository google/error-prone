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

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JavaDurationWithNanos}. */
@RunWith(JUnit4.class)
public class JavaDurationWithNanosTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JavaDurationWithNanos.class, getClass());

  @Test
  public void durationWithNanos() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Duration.ofSeconds(Duration.ZERO.getSeconds(), 42);",
            "  private static final Duration DURATION = Duration.ZERO.withNanos(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithNanosNamedVariable() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private static final Duration DURATION1 = Duration.ZERO;",
            "  // BUG: Diagnostic contains: Duration.ofSeconds(DURATION1.getSeconds(), 44);",
            "  private static final Duration DURATION2 = DURATION1.withNanos(44);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithNanosInsideJavaTime() {
    assumeFalse(RuntimeVersion.isAtLeast9());
    helper
        .addSourceLines(
            "TestClass.java",
            "package java.time;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.ZERO.withNanos(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithNanosPrimitiveImportClash() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Duration;",
            "public class TestClass {",
            "  private static final java.time.Duration DURATION = ",
            "      // BUG: Diagnostic contains: "
                + "java.time.Duration.ofSeconds(java.time.Duration.ZERO.getSeconds(), 42);",
            "      java.time.Duration.ZERO.withNanos(42);",
            "}")
        .doTest();
  }

}
