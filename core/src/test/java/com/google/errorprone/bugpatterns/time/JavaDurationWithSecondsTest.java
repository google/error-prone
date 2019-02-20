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

/** Tests for {@link JavaDurationWithSeconds}. */
@RunWith(JUnit4.class)
public class JavaDurationWithSecondsTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JavaDurationWithSeconds.class, getClass());

  @Test
  public void durationWithSeconds() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: Duration.ofSeconds(42, Duration.ZERO.getNano());",
            "  private static final Duration DURATION = Duration.ZERO.withSeconds(42);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithSecondsNamedVariable() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private static final Duration DURATION1 = Duration.ZERO;",
            "  // BUG: Diagnostic contains: Duration.ofSeconds(44, DURATION1.getNano());",
            "  private static final Duration DURATION2 = DURATION1.withSeconds(44);",
            "}")
        .doTest();
  }

  @Test
  public void durationWithSecondsInsideJavaTime() {
    assumeFalse(RuntimeVersion.isAtLeast9());
    helper
        .addSourceLines(
            "TestClass.java",
            "package java.time;",
            "public class TestClass {",
            "  private static final Duration DURATION = Duration.ZERO.withSeconds(42);",
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
            "  private static final java.time.Duration DURATION = ",
            "      // BUG: Diagnostic contains: "
                + "java.time.Duration.ofSeconds(42, java.time.Duration.ZERO.getNano());",
            "      java.time.Duration.ZERO.withSeconds(42);",
            "}")
        .doTest();
  }
}
