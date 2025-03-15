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
public final class AssignmentExpressionTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AssignmentExpression.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(AssignmentExpression.class, getClass());

  @Test
  public void bareAssignment_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                int a = 1;
                a = 2;
                for (a = 3; true; ) {
                  break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void comparedToNull_sure() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                Object a;
                if ((a = new Object()) != null) {
                  return;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleAssignments_sure() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                Object a;
                Object b;
                a = b = new Object();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assignmentExpressionUsed_ohNo() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(boolean x) {
                // BUG: Diagnostic contains:
                test(x = true);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doubleAssignment_removed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test() {
                // BUG: Diagnostic contains:
                Object a = a = new Object();
                // BUG: Diagnostic contains:
                a = a = new Object();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test() {
                Object a = new Object();
                a = new Object();
              }
            }
            """)
        .doTest();
  }
}
