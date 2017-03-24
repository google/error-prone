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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author hanuszczak@google.com (Łukasz Hanuszczak) */
@RunWith(JUnit4.class)
public final class UngroupedOverloadsTest {

  /**
   * Specialized version of the {@link UngroupedOverloads} bug checker for testing with a low cutoff
   * limit set.
   *
   * <p>This class needs to be public because of the limitation of {@link CompilationTestHelper}.
   */
  @BugPattern(
    name = "UngroupedOverloadsLowCutoff",
    summary = "A specialized version of the ungrouped overloads checker with a low cutoff limit",
    category = JDK,
    severity = SUGGESTION
  )
  public static final class UngroupedOverloadsLowCutoff extends UngroupedOverloads {

    public UngroupedOverloadsLowCutoff() {
      super(5);
    }
  }

  private CompilationTestHelper compilationHelper;
  private CompilationTestHelper cutoffCompilationHelper;

  private BugCheckerRefactoringTestHelper refactoringHelper;
  private BugCheckerRefactoringTestHelper cutoffRefactoringHelper;

  @Before
  public void createCompilationHelpers() {
    compilationHelper = createCompilationHelper(UngroupedOverloads.class);
    cutoffCompilationHelper = createCompilationHelper(UngroupedOverloadsLowCutoff.class);
  }

  private CompilationTestHelper createCompilationHelper(Class<? extends BugChecker> bugChecker) {
    return CompilationTestHelper.newInstance(bugChecker, getClass());
  }

  @Before
  public void createRefactoringHelper() {
    refactoringHelper = createRefactoringHelper(new UngroupedOverloads());
    cutoffRefactoringHelper = createRefactoringHelper(new UngroupedOverloadsLowCutoff());
  }

  private BugCheckerRefactoringTestHelper createRefactoringHelper(BugChecker bugChecker) {
    return BugCheckerRefactoringTestHelper.newInstance(bugChecker, getClass());
  }

  @Test
  public void ungroupedOverloadsPositiveCasesSingle() throws Exception {
    final String path = "UngroupedOverloadsPositiveCasesSingle.java";
    compilationHelper.addSourceFile(path).doTest();
    cutoffCompilationHelper.addSourceFile(path).doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesMultiple() throws Exception {
    final String path = "UngroupedOverloadsPositiveCasesMultiple.java";
    compilationHelper.addSourceFile(path).doTest();
    cutoffCompilationHelper.addSourceFile(path).doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesInterleaved() throws Exception {
    final String path = "UngroupedOverloadsPositiveCasesInterleaved.java";
    compilationHelper.addSourceFile(path).doTest();
    cutoffCompilationHelper.addSourceFile(path).doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesCovering() throws Exception {
    final String path = "UngroupedOverloadsPositiveCasesCovering.java";
    compilationHelper.addSourceFile(path).doTest();
    cutoffCompilationHelper.addSourceFile(path).doTest();
  }

  @Test
  public void ungroupedOverloadsNegativeCases() throws Exception {
    final String path = "UngroupedOverloadsNegativeCases.java";
    compilationHelper.addSourceFile(path).doTest();
    cutoffCompilationHelper.addSourceFile(path).doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringComments() throws Exception {
    refactoringHelper
        .addInput("UngroupedOverloadsRefactoringComments.java")
        .addOutput("UngroupedOverloadsRefactoringComments_expected.java")
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringMultiple() throws Exception {
    refactoringHelper
        .addInput("UngroupedOverloadsRefactoringMultiple.java")
        .addOutput("UngroupedOverloadsRefactoringMultiple_expected.java")
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringInterleaved() throws Exception {
    refactoringHelper
        .addInput("UngroupedOverloadsRefactoringInterleaved.java")
        .addOutput("UngroupedOverloadsRefactoringInterleaved_expected.java")
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringBelowCutoffLimit() throws Exception {
    // Here we have 4 methods so refactoring should be applied.
    cutoffRefactoringHelper
        .addInputLines(
            "in/BelowLimit.java",
            "class BelowLimit {",
            "  BelowLimit() {}",
            "  void foo() {}",
            "  void bar() {}",
            "  void foo(int x) {}",
            "}")
        .addOutputLines(
            "out/BelowLimit.java",
            "class BelowLimit {",
            "  BelowLimit() {}",
            "  void foo() {}",
            "  void foo(int x) {}",
            "  void bar() {}",
            "}")
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringAboveCutoffLimit() throws Exception {
    // Here we have 5 methods so refactoring should NOT be applied.
    cutoffRefactoringHelper
        .addInputLines(
            "in/AboveLimit.java",
            "class AboveLimit {",
            "  AboveLimit() {}",
            "  void foo() {}",
            "  void bar() {}",
            "  void foo(int x) {}",
            "  void baz() {}",
            "}")
        .addOutputLines(
            "out/AboveLimit.java",
            "class AboveLimit {",
            "  AboveLimit() {}",
            "  void foo() {}",
            "  void bar() {}",
            "  void foo(int x) {}",
            "  void baz() {}",
            "}")
        .doTest();
  }
}
