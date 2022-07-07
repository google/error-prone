/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class UnusedTypeParameterTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnusedTypeParameter.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(UnusedTypeParameter.class, getClass());

  @Test
  public void positiveOnClass() {
    helper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "final class Test<T> {}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoring
        .addInputLines(
            "Test.java", //
            "final class Test<T> {}")
        .addOutputLines(
            "Test.java", //
            "final class Test {}")
        .doTest();
  }

  @Test
  public void refactoringWithTwoParameters() {
    refactoring
        .addInputLines(
            "Test.java", //
            "final class Test<A, B> {",
            "  B get() { return null; }",
            "}")
        .addOutputLines(
            "Test.java", //
            "final class Test<B> {",
            "  B get() { return null; }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringWithGtgt() {
    refactoring
        .addInputLines(
            "Test.java", //
            "final class Test<A extends java.util.List<?>> {}")
        .addOutputLines(
            "Test.java", //
            "final class Test {}")
        .doTest();
  }

  @Test
  public void positiveOnMethod() {
    helper
        .addSourceLines(
            "Test.java", //
            "final class Test {",
            "  // BUG: Diagnostic contains:",
            "  private <T> void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodCouldBeOverridden_negativeFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  <T> void test() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  private <T> boolean contains(java.util.Set<T> set, T elem) {",
            "    return set.contains(elem);",
            "  }",
            "}")
        .doTest();
  }
}
