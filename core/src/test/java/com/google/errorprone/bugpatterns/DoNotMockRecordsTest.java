/*
 * Copyright 2024 The Error Prone Authors.
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
 * Unit tests for {@link DoNotMockRecords}.
 */
@RunWith(JUnit4.class)
public class DoNotMockRecordsTest {

  private final CompilationTestHelper helper = CompilationTestHelper.newInstance(
      DoNotMockRecords.class, getClass());

  @Test
  public void positive_mockito_annotation() {
    helper
        .addSourceLines(
            "R.java",
            "public record R() {}")
        .addSourceLines(
            "Test.java",
        "import org.mockito.Mock;",
        "class TestMockMethod {",
        "  // BUG: Diagnostic contains:",
        "  @Mock private R r1;",
            "}")
        .doTest();
  }

  @Test
  public void positive_mockito_method() {
    helper
        .addSourceLines(
            "R.java",
            "public record R() {}")
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "class TestMockMethod {",
        "  // BUG: Diagnostic contains:",
        "  private final R r2 = mock(R.class);",
        "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "R.java",
            "public class R {}")
        .addSourceLines(
            "Test.java",
        "import org.mockito.Mockito;",
        "import org.mockito.Mock;",
        "class TestMockMethod {",
        "  @Mock private R r1;",
        "  private final R r2 = Mockito.mock(R.class);",
        "}")
        .doTest();
  }
}