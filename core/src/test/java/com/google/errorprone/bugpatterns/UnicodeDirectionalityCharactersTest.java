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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnicodeDirectionalityCharacters}. */
@RunWith(JUnit4.class)
public final class UnicodeDirectionalityCharactersTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnicodeDirectionalityCharacters.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(
          UnicodeDirectionalityCharacters.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** \u202A */",
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoring
        .addInputLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** \u202A */",
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .addOutputLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** \\u202A */",
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .doTest();
  }
}
