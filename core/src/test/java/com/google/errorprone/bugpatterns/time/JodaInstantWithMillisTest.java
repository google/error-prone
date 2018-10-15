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

/** Tests for {@link JodaInstantWithMillis}. */
@RunWith(JUnit4.class)
public class JodaInstantWithMillisTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaInstantWithMillis.class, getClass());

  @Test
  public void instantConstructor() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Instant INSTANT = new Instant(42);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithMillisIntPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: new Instant(42);",
            "  private static final Instant INSTANT = Instant.now().withMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithMillisLongPrimitive() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: new Instant(42L);",
            "  private static final Instant INSTANT = Instant.now().withMillis(42L);",
            "}")
        .doTest();
  }

  @Test
  public void instantWithMillisNamedVariable() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import org.joda.time.Instant;",
            "public class TestClass {",
            "  private static final Instant INSTANT1 = new Instant(42);",
            "  // BUG: Diagnostic contains: new Instant(44);",
            "  private static final Instant INSTANT2 = INSTANT1.withMillis(44);",
            "}")
        .doTest();
  }

  @Test
  public void instantConstructorIntPrimitiveInsideJoda() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Instant INSTANT = Instant.now().withMillis(42);",
            "}")
        .doTest();
  }

  @Test
  public void instantConstructorLongPrimitiveInsideJoda() {
    helper
        .addSourceLines(
            "TestClass.java",
            "package org.joda.time;",
            "public class TestClass {",
            "  private static final Instant INSTANT = Instant.now().withMillis(42L);",
            "}")
        .doTest();
  }

  @Test
  public void instantConstructorLongPrimitiveImportClash() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.Instant;",
            "public class TestClass {",
            "  private static final org.joda.time.Instant INSTANT = ",
            "      // BUG: Diagnostic contains: new org.joda.time.Instant(42L);",
            "      org.joda.time.Instant.now().withMillis(42L);",
            "}")
        .doTest();
  }
}
