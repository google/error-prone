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
package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import java.util.function.Function;
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
          "Run the ArgumentSelectionDefectChecker checker using string equality for edit distance")
  public static class ArgumentSelectionDefectWithStringEquality
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithStringEquality() {
      super(ArgumentChangeFinder.builder().setDistanceFunction(buildEqualityFunction()).build());
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
            "     // target(/* first= */second, /* second= */first)",
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
            "     // target(/* first= */getSecond(), /* second= */first)",
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
            "     // target(/* first= */second, /* second= */null)",
            "     target(second, null);",
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

  @Test
  public void argumentSelectionDefectChecker_commentsOnlyOnSwappedPair_withThreeArguments() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second, Object third);",
            "  void test(Object first, Object second, Object third) {",
            "     // BUG: Diagnostic contains: target(first, second, third)",
            "     // target(/* first= */second, /* second= */first, third)",
            "     target(second, first, third);",
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
              + "parameters")
  public static class ArgumentSelectionDefectWithIgnoredFormalsHeuristic
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithIgnoredFormalsHeuristic() {
      super(
          ArgumentChangeFinder.builder()
              .setDistanceFunction(buildEqualityFunction())
              .addHeuristic(new LowInformationNameHeuristic(ImmutableSet.of("ignore")))
              .build());
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

  @Test
  public void argumentSelectionDefectChecker_makesSwap_withNullArgument() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object second) {",
            "     target(second, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectChecker_rejectsSwap_withArgumentWithoutName() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectChecker.class, getClass())
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

  /**
   * A {@link BugChecker} which runs the ArgumentSelectionDefectChecker checker using string
   * equality for edit distance and a penaltyThreshold of 0.9
   */
  @BugPattern(
      name = "ArgumentSelectionDefectWithPenaltyThreshold",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Run the ArgumentSelectionDefectChecker checker with the penalty threshold heuristic")
  public static class ArgumentSelectionDefectWithPenaltyThreshold
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithPenaltyThreshold() {
      super(
          ArgumentChangeFinder.builder()
              .setDistanceFunction(buildEqualityFunction())
              .addHeuristic(new PenaltyThresholdHeuristic(0.9))
              .build());
    }
  }

  @Test
  public void argumentSelectionDefectCheckerWithPenalty_findsSwap_withSwappedMatchingPair() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithPenaltyThreshold.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     // BUG: Diagnostic contains: target(first, second)",
            "     // target(/* first= */second, /* second= */first)",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void argumentSelectionDefectCheckerWithPenalty_makesNoChange_withAlmostMatchingSet() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithPenaltyThreshold.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second, Object third, Object fourth);",
            "  void test(Object first, Object second, Object third, Object different) {",
            "     target(different, third, second, first);",
            "  }",
            "}")
        .doTest();
  }

  /**
   * A {@link BugChecker} which runs the ArgumentSelectionDefectChecker checker using string
   * equality for edit distance and name in comments heuristic
   */
  @BugPattern(
      name = "ArgumentSelectionDefectWithNameInCommentsHeuristic",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Run the ArgumentSelectionDefectChecker checker using string equality for edit distance")
  public static class ArgumentSelectionDefectWithNameInCommentsHeuristic
      extends ArgumentSelectionDefectChecker {

    public ArgumentSelectionDefectWithNameInCommentsHeuristic() {
      super(
          ArgumentChangeFinder.builder()
              .setDistanceFunction(buildEqualityFunction())
              .addHeuristic(new NameInCommentHeuristic())
              .build());
    }
  }

  @Test
  public void argumentSelectionDefectCheckerWithPenalty_noSwap_withNamedPair() {
    CompilationTestHelper.newInstance(
            ArgumentSelectionDefectWithNameInCommentsHeuristic.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     target(/* first= */second, /* second= */first);",
            "  }",
            "}")
        .doTest();
  }

  private static final Function<ParameterPair, Double> buildEqualityFunction() {
    return new Function<ParameterPair, Double>() {
      @Override
      public Double apply(ParameterPair parameterPair) {
        return parameterPair.formal().name().equals(parameterPair.actual().name()) ? 0.0 : 1.0;
      }
    };
  }

  /** A {@link BugChecker} which returns true if parameter names are available */
  @BugPattern(
      name = "ParameterNamesAvailableChecker",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary = "Returns true if parameter names are available on a method call")
  public static class ParameterNamesAvailableChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              String.valueOf(
                  ASTHelpers.getSymbol(tree).getParameters().stream()
                      .noneMatch(p -> p.getSimpleName().toString().matches("arg[0-9]"))))
          .build();
    }
  }

  @Test
  public void parameterNamesAvailable_returnsTree_onMethodNotInCompilationUnit() {
    CompilationTestHelper.newInstance(ParameterNamesAvailableChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", CompilationTestHelper.class.getName()),
            String.format("import %s;", BugChecker.class.getName()),
            "class Test {",
            "  void test() {",
            "     // BUG: Diagnostic contains: true",
            "     CompilationTestHelper.newInstance((Class<BugChecker>)null, getClass());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void description() {
    CompilationTestHelper.newInstance(ArgumentSelectionDefectWithStringEquality.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     // BUG: Diagnostic contains: The following arguments may have been swapped:"
                + " 'second' for formal parameter 'first', 'first' for formal parameter 'second'."
                + " Either add clarifying `/* paramName= */` comments, or swap the arguments if"
                + " that is what was intended",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }
}
