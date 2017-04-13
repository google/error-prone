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
package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.BugChecker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ArgumentSelectionDefectChecker}.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@RunWith(JUnit4.class)
public class ArgumentSelectionDefectCheckerTest {

  /**
   * A {@link BugChecker} which runs the ArgumentSelectionDefectChecker checker using string
   * equality for edit distance
   */
  @BugPattern(
    name = "ArgumentSelectionDefectWithStringEquality",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary =
        "Run the ArgumentSelectionDefectChecker checker using string equality for edit distance"
  )
  public static class ArgumentSelectionDefectWithStringEquality
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithStringEquality() {
      super((s, t) -> s.equals(t) ? 0.0 : 1.0, ImmutableList.of());
    }
  }

  @Test
  public void argumentSelectionDefectChecker_findsSwap_withSwappedMatchingPair() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     // BUG: Diagnostic contains: target(first, second)",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectChecker_findsSwap_withSwappedMatchingPairWithMethod() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object getSecond();",
            "  void test(Object first) {",
            "     // BUG: Diagnostic contains: target(first, getSecond())",
            "     target(getSecond(), first);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectChecker_findsSwap_withOneNullArgument() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object second) {",
            "     // BUG: Diagnostic contains: target(null, second)",
            "     target(second, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectChecker_rejectsSwap_withArgumentWithoutName() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object second) {",
            "     target(second, 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectChecker_rejectsSwap_withNoAssignableAlternatives() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(String first, Integer second);",
            "  void test(Integer first, String second) {",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }

  /**
   * A {@link BugChecker} which runs the ArgumentSelectionDefectChecker checker using string
   * equality for edit distance and ignores formal parameters called 'ignore'.
   */
  @BugPattern(
    name = "ArgumentSelectionDefectWithIgnoredFormalsHeuristic",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary =
        "Run the ArgumentSelectionDefectChecker checker with a heuristic that ignores formal "
            + "parameters"
  )
  public static class ArgumentSelectionDefectWithIgnoredFormalsHeuristic
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithIgnoredFormalsHeuristic() {
      super(
          (s, t) -> s.equals(t) ? 0.0 : 1.0,
          ImmutableList.of(new LowInformationNameHeuristic(ImmutableSet.of("ignore"))));
    }
  }

  @Test
  public void argumentSelectionDefectChecker_rejectsSwap_withIgnoredFormalParameter() {
    CompilationTestHelper.newInstance(
            ArgumentSelectionDefectWithIgnoredFormalsHeuristic.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object ignore, Object second);",
            "  void test(Object ignore, Object second) {",
            "     target(second, ignore);",
            "  }",
            "}")
        .doTest();
  }
}
