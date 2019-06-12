/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link PreferDurationOverload}. */
@RunWith(JUnit4.class)
public class PreferDurationOverloadTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(PreferDurationOverload.class, getClass());

  @Test
  public void callingLongTimeUnitMethodWithDurationOverload_microseconds() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.common.cache.CacheBuilder;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  public CacheBuilder foo(CacheBuilder builder) {",
            "    // BUG: Diagnostic contains: builder.expireAfterAccess(Duration.of(42,"
                + " ChronoUnit.MICROSECONDS));",
            "    return builder.expireAfterAccess(42, TimeUnit.MICROSECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingLongTimeUnitMethodWithDurationOverload() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.common.cache.CacheBuilder;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  public CacheBuilder foo(CacheBuilder builder) {",
            "    // BUG: Diagnostic contains: builder.expireAfterAccess(Duration.ofSeconds(42L));",
            "    return builder.expireAfterAccess(42L, TimeUnit.SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingLongTimeUnitMethodWithDurationOverload_intParam() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.common.cache.CacheBuilder;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  public CacheBuilder foo(CacheBuilder builder) {",
            "    // BUG: Diagnostic contains: builder.expireAfterAccess(Duration.ofSeconds(42));",
            "    return builder.expireAfterAccess(42, TimeUnit.SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingLongTimeUnitMethodWithDurationOverload_privateMethod() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  private void bar(long v, TimeUnit tu) {",
            "  }",
            "  private void bar(Duration d) {",
            "  }",
            "  public void foo() {",
            "    // BUG: Diagnostic contains: bar(Duration.ofSeconds(42L));",
            "    bar(42L, TimeUnit.SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingLongTimeUnitMethodWithoutDurationOverload() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.util.concurrent.Future;",
            "import java.util.concurrent.TimeUnit;",
            "public class TestClass {",
            "  public String foo(Future<String> future) throws Exception {",
            "    return future.get(42L, TimeUnit.SECONDS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingJodaDurationMethodWithDurationOverload_privateMethod() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private void bar(org.joda.time.Duration d) {",
            "  }",
            "  private void bar(Duration d) {",
            "  }",
            "  public void foo(org.joda.time.Duration jodaDuration) {",
            "    // BUG: Diagnostic contains: bar(Duration.ofMillis(jodaDuration.getMillis()));",
            "    bar(jodaDuration);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingJodaDurationMethodWithoutDurationOverload() {
    helper
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  private void bar(org.joda.time.Duration d) {",
            "  }",
            "  public void foo(org.joda.time.Duration jodaDuration) {",
            "    bar(jodaDuration);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingJodaReadableDurationMethodWithDurationOverload_privateMethod() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Duration;",
            "public class TestClass {",
            "  private void bar(org.joda.time.ReadableDuration d) {",
            "  }",
            "  private void bar(Duration d) {",
            "  }",
            "  public void foo(org.joda.time.Duration jodaDuration) {",
            "    // BUG: Diagnostic contains: bar(Duration.ofMillis(jodaDuration.getMillis()));",
            "    bar(jodaDuration);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void callingJodaReadableDurationMethodWithoutDurationOverload() {
    helper
        .addSourceLines(
            "TestClass.java",
            "public class TestClass {",
            "  private void bar(org.joda.time.ReadableDuration d) {",
            "  }",
            "  public void foo(org.joda.time.Duration jodaDuration) {",
            "    bar(jodaDuration);",
            "  }",
            "}")
        .doTest();
  }
}
