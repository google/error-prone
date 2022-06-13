/*
 * Copyright 2021 Google Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link PublicApiNamedStreamShouldReturnStream}. */
@RunWith(JUnit4.class)
public class PublicApiNamedStreamShouldReturnStreamTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setup() {
    compilationHelper =
        CompilationTestHelper.newInstance(PublicApiNamedStreamShouldReturnStream.class, getClass());
  }

  @Test
  public void abstractMethodPositiveCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public abstract class Test {",
            "  // BUG: Diagnostic contains: PublicApiNamedStreamShouldReturnStream",
            "  public abstract int stream();",
            "}")
        .doTest();
  }

  @Test
  public void regularMethodPositiveCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: PublicApiNamedStreamShouldReturnStream",
            "  public String stream() { return \"hello\";}",
            "}")
        .doTest();
  }

  @Test
  public void compliantNegativeCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import java.util.stream.Stream;",
            "public abstract class Test {",
            "  public abstract Stream<Integer> stream();",
            "}")
        .doTest();
  }

  @Test
  public void differentNameNegativeCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public class Test {",
            "  public int differentMethodName() { return 0; }",
            "}")
        .doTest();
  }

  @Test
  public void privateMethodNegativeCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public class Test {",
            "  private String stream() { return \"hello\"; }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeEndingWithStreamNegativeCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public class Test {",
            "  private static class TestStream {}",
            "  public TestStream stream() { return new TestStream(); }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeNotEndingWithStreamPositiveCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "public class Test {",
            "  private static class TestStreamRandomSuffix {}",
            "  // BUG: Diagnostic contains: PublicApiNamedStreamShouldReturnStream",
            "  public TestStreamRandomSuffix stream() { return new TestStreamRandomSuffix(); }",
            "}")
        .doTest();
  }
}
