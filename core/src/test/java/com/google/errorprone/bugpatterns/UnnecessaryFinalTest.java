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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnnecessaryFinal}. */
@RunWith(JUnit4.class)
public final class UnnecessaryFinalTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryFinal.class, getClass());

  @Test
  public final void removesOnParameters() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void test(final Object o) {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void test(Object o) {}",
            "}")
        .doTest();
  }

  @Test
  public final void removesOnLocals() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void test() {",
            "    final Object o;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void test() {",
            "    Object o;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public final void doesNotRemoveOnFields() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  final Object o = null;",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
