/*
 * Copyright 2023 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class OverridingMethodInconsistentArgumentNamesCheckerTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(
          OverridingMethodInconsistentArgumentNamesChecker.class, getClass());

  @Test
  public void positiveSwap() {
    testHelper
        .addSourceLines("A.java", "class A {", "  void m(int p1, int p2) {}", "}")
        .addSourceLines(
            "B.java",
            "class B extends A {",
            "  @Override",
            "  // BUG: Diagnostic contains: A consistent order would be: m(p1, p2)",
            "  void m(int p2, int p1) {}",
            "}")
        .doTest();
  }

  @Test
  public void positivePermutation() {
    testHelper
        .addSourceLines("A.java", "class A {", "  void m(int p1, int p2, int p3) {}", "}")
        .addSourceLines(
            "B.java",
            "class B extends A {",
            "  @Override",
            "  // BUG: Diagnostic contains: A consistent order would be: m(p1, p2, p3)",
            "  void m(int p3, int p1, int p2) {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines("A.java", "class A {", "  void m(int p1, int p2) {}", "}")
        .addSourceLines(
            "B.java", "class B extends A {", "  @Override", "  void m(int p1, int p2) {}", "}")
        .doTest();
  }

  @Test
  public void negative2() {
    testHelper
        .addSourceLines("A.java", "class A {", "  void m(int p1, int p2) {}", "}")
        .addSourceLines(
            "B.java", "class B extends A {", "  @Override", "  void m(int p1, int p3) {}", "}")
        .doTest();
  }
}
