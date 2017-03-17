/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link IterablePathParameter}Test */
@RunWith(JUnit4.class)
public class IterablePathParameterTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(IterablePathParameter.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  // BUG: Diagnostic contains: f(Collection<? extends Path> xs)",
            "  void f(Iterable<? extends Path> xs) {}",
            "  // BUG: Diagnostic contains: g(Collection<? super Path> xs)",
            "  void g(Iterable<? super Path> xs) {}",
            "  // BUG: Diagnostic contains: h(Collection<Path> xs)",
            "  void h(Iterable<Path> xs) {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "import java.util.Collection;",
            "class Test {",
            "  void f(Collection<Path> xs) {}",
            "}")
        .doTest();
  }

  @Test
  public void raw() {
    testHelper
        .addSourceLines("Test.java", "class Test {", "  void f(Iterable xs) {}", "}")
        .doTest();
  }
}
