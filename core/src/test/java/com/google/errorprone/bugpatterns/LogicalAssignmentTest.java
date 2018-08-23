/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LogicalAssignment}Test */
@RunWith(JUnit4.class)
public class LogicalAssignmentTest {

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new LogicalAssignment(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(boolean x){",
            "    if (x = true) {}",
            "    while (x = true) {}",
            "    for (; x = true; ) {}",
            "    do {} while (x = true);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(boolean x){",
            "    if ((x = true)) {}",
            "    while ((x = true)) {}",
            "    for (; (x = true); ) {}",
            "    do {} while ((x = true));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(LogicalAssignment.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(boolean x){",
            "    if ((x = true)) {}",
            "    while ((x = true)) {}",
            "    for (; (x = true); ) {}",
            "    do {} while (x == true);",
            "    if (x == true) {}",
            "    while (x == true) {}",
            "    for (; x == true; ) {}",
            "    do {} while (x == true);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_equalityFix() {
    BugCheckerRefactoringTestHelper.newInstance(new LogicalAssignment(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(boolean x){",
            "    if (x = true) {}",
            "    if ((x = true)) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(boolean x){",
            "    if (x == true) {}",
            "    if ((x = true)) {}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }
}
