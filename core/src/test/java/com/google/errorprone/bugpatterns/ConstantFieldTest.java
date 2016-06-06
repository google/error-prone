/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChooser;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.fixes.Fix;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;

/** {@link ConstantField}Test */
@RunWith(JUnit4.class)
public class ConstantFieldTest {
  CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ConstantField.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: static final Object CONSTANT1 = 42;",
            "  Object CONSTANT1 = 42;",
            "  // BUG: Diagnostic contains: @Deprecated static final Object CONSTANT2 = 42;",
            "  @Deprecated Object CONSTANT2 = 42;",
            "  // BUG: Diagnostic contains: static final Object CONSTANT3 = 42;",
            "  static Object CONSTANT3 = 42;",
            "  // BUG: Diagnostic contains: static final Object CONSTANT4 = 42;",
            "  final Object CONSTANT4 = 42;",
            "  // BUG: Diagnostic contains:"
                + " @Deprecated private static final Object CONSTANT5 = 42;",
            "  @Deprecated private Object CONSTANT5 = 42;",
            "}")
        .doTest();
  }

  @Test
  public void rename() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: 'Object constantCaseName = 42;'",
            "  Object CONSTANT_CASE_NAME = 42;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static final Object CONSTANT = 42;",
            "  Object nonConst = 42;",
            "}")
        .doTest();
  }

  @Test
  public void renameUsages() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new ConstantField(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object CONSTANT_CASE = 42;",
            "  void f() {",
            "    System.err.println(CONSTANT_CASE);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object constantCase = 42;",
            "  void f() {",
            "    System.err.println(constantCase);",
            "  }",
            "}")
        .setFixChooser(
            new FixChooser() {
              @Override
              public Fix choose(List<Fix> fixes) {
                return fixes.get(1);
              }
            })
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }
}
