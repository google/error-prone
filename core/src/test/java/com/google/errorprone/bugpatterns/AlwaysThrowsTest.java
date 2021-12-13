/*
 * Copyright 2021 The Error Prone Authors.
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

/** {@link AlwaysThrows}Test */
@RunWith(JUnit4.class)
public class AlwaysThrowsTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(AlwaysThrows.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Instant;",
            "class T { ",
            "  void f() {",
            "    // BUG: Diagnostic contains: will fail at runtime with a DateTimeParseException",
            "    Instant.parse(\"2007-12-3T10:15:30.00Z\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableMapThrows() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private static final ImmutableMap<Integer, Integer> xs =",
            "    ImmutableMap.<Integer, Integer>builder()",
            "      .put(1, 1)",
            "      // BUG: Diagnostic contains:",
            "      .put(1, 2)",
            "      .buildOrThrow();",
            "}")
        .doTest();
  }

  @Test
  public void immutableBiMapThrows() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "class Test {",
            "  private static final ImmutableBiMap<Integer, Integer> xs =",
            "    ImmutableBiMap.<Integer, Integer>builder()",
            "      .put(1, 1)",
            "      // BUG: Diagnostic contains:",
            "      .put(2, 1)",
            "      .buildOrThrow();",
            "}")
        .doTest();
  }

  @Test
  public void immutableMapDoesNotThrow() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private static final ImmutableMap<Integer, Integer> xs =",
            "    ImmutableMap.<Integer, Integer>builder()",
            "      .put(1, 1)",
            "      .put(2, 2)",
            "      .buildOrThrow();",
            "}")
        .doTest();
  }

  @Test
  public void immutableMapOfThrows() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private static final ImmutableMap<Integer, Integer> xs =",
            "    // BUG: Diagnostic contains:",
            "    ImmutableMap.of(1, 1, 1, 2);",
            "}")
        .doTest();
  }

  @Test
  public void immutableMapOfThrowsWithEnums() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private enum E {A, B}",
            "  private static final ImmutableMap<E, Integer> xs =",
            "    // BUG: Diagnostic contains:",
            "    ImmutableMap.of(E.A, 1, E.A, 2);",
            "}")
        .doTest();
  }

  @Test
  public void immutableBiMapOfThrowsWithEnums() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  private enum E {A, B}",
            "  private static final ImmutableMap<E, Integer> xs =",
            "    // BUG: Diagnostic contains:",
            "    ImmutableMap.of(E.A, 1, E.A, 2);",
            "}")
        .doTest();
  }

  @Test
  public void immutableMapOfThrowsWithRepeatedFinalVariable() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "class Test {",
            "  ImmutableBiMap<String, Integer> map(String s) {",
            "    // BUG: Diagnostic contains:",
            "    return ImmutableBiMap.of(s, 1, s, 2);",
            "  }",
            "  ImmutableBiMap<Integer, String> values(String s) {",
            "    // BUG: Diagnostic contains:",
            "    return ImmutableBiMap.of(1, s, 2, s);",
            "  }",
            "}")
        .doTest();
  }
}
