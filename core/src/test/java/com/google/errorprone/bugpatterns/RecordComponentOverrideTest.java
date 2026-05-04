/*
 * Copyright 2025 The Error Prone Authors.
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
public class RecordComponentOverrideTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(RecordComponentOverride.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(RecordComponentOverride.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "I.java",
            """
            public interface I {
              int foo();
            }
            """)
        .addSourceLines(
            "R.java",
            """
            // BUG: Diagnostic contains:
            public record R(@Override int bar) implements I {
              @Override
              public int foo() {
                return bar();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveExplicitAccessor() {
    testHelper
        .addSourceLines(
            "I.java",
            """
            public interface I {
              int foo();
            }
            """)
        .addSourceLines(
            "R.java",
            """
            // BUG: Diagnostic contains:
            public record R(@Override int foo) implements I {}
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "R.java",
            """
            public record R(@Override int bar) {}
            """)
        .addOutputLines(
            "R.java",
            """
            public record R(int bar) {}
            """)
        .doTest();
  }
}
