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

package com.google.errorprone.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.parser.Tokens.Comment;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test code for {@code Comments} */
@RunWith(JUnit4.class)
public class CommentsTest {

  /**
   * A {@link BugChecker} that calls computeEndPosition for each invocation and prints the final
   * line of source ending at that position
   */
  @BugPattern(
      name = "ComputeEndPosition",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary = "Calls computeEndPosition and prints results")
  public static class ComputeEndPosition extends BugChecker implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      CharSequence sourceCode = state.getSourceCode();
      Optional<Integer> endPosition = Comments.computeEndPosition(tree, sourceCode, state);
      if (!endPosition.isPresent()) {
        return Description.NO_MATCH;
      }
      int startPosition = endPosition.get();
      do {
        startPosition--;
      } while (sourceCode.charAt(startPosition) != '\n');

      return buildDescription(tree)
          .setMessage(sourceCode.subSequence(startPosition + 1, endPosition.get()).toString())
          .build();
    }
  }

  @Test
  public void computeEndPosition_returnsEndOfInvocation_whenNoTrailingComment() {
    CompilationTestHelper.newInstance(ComputeEndPosition.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: target(param)",
            "    target(param);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void computeEndPosition_returnsEndOfNextStatement_whenTrailingComment() {
    CompilationTestHelper.newInstance(ComputeEndPosition.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: int i;",
            "    target(param); // 1",
            "    int i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void computeEndPosition_returnsEndOfBlock_whenLastWithTrailingComment() {
    CompilationTestHelper.newInstance(ComputeEndPosition.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: }",
            "    target(param); // 1",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints the contents of comments around arguments */
  @BugPattern(
      name = "PrintCommentsForArguments",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Prints comments occurring around arguments. Matches calls to methods named "
              + "'target' and all constructors")
  public static class PrintCommentsForArguments extends BugChecker
      implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!ASTHelpers.getSymbol(tree).getSimpleName().contentEquals("target")) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(commentsToString(Comments.findCommentsForArguments(tree, state)))
          .build();
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(commentsToString(Comments.findCommentsForArguments(tree, state)))
          .build();
    }

    private static String commentsToString(ImmutableList<Commented<ExpressionTree>> comments) {
      return comments.stream()
          .map(
              c ->
                  Stream.of(
                          String.valueOf(getTextFromCommentList(c.beforeComments())),
                          String.valueOf(c.tree()),
                          String.valueOf(getTextFromCommentList(c.afterComments())))
                      .collect(Collectors.joining(" ")))
          .collect(toImmutableList())
          .toString();
    }

    private static ImmutableList<String> getTextFromCommentList(ImmutableList<Comment> comments) {
      return comments.stream().map(Comments::getTextFromComment).collect(toImmutableList());
    }
  }

  @Test
  public void findCommentsForArguments_findsBothComments_beforeAndAfter() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: [[1] param [2]]",
            "    target(/* 1 */ param /* 2 */);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsBothComments_before() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: [[1, 2] param []]",
            "    target(/* 1 */ /* 2 */ param);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsBothComments_after() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test(Object param) {",
            "    // BUG: Diagnostic contains: [[] param [1, 2]]",
            "    target(param /* 1 */ /* 2 */);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_assignsToCorrectParameter_adjacentComments() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [1], [2] param2 []]",
            "    target(param1 /* 1 */, /* 2 */ param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsNoComments_whenNoComments() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [], [] param2 []]",
            "    target(param1, param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsNothing_whenNoArguments() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target();",
            "  private void test() {",
            "    // BUG: Diagnostic contains: []",
            "    target();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_assignToFirstParameter_withLineCommentAfterComma() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [1], [] param2 []]",
            "    target(param1, // 1",
            "           param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_assignToFirstParameter_withBlockAfterComma() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [1], [] param2 []]",
            "    target(param1, /* 1 */",
            "           param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_assignToSecondParameter_withLineCommentAfterMethod() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [], [] param2 [2]]",
            "    target(param1,",
            "           param2); // 2",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      findCommentsForArguments_assignToSecondParameter_withLineCommentAfterMethodMidBlock() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  private void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [], [] param2 [2]]",
            "    target(param1,",
            "           param2); // 2",
            "    int i = 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_assignToSecondParameter_withLineCommentAfterField() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object target(Object param);",
            "  // BUG: Diagnostic contains: [[] null [1]]",
            "  private Object test = target(null); // 1",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsComments_onConstructor() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Test(Object param1, Object param2) {}",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[1] param1 [], [] param2 [2]]",
            "    new Test(/* 1 */ param1, param2 /* 2 */);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_attachesCommentToSecondArgument_whenCommentOnOwnLine() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Test(Object param1, Object param2) {}",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [], [1] param2 []]",
            "    new Test(param1, ",
            "             // 1",
            "             param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      findCommentsForArguments_attachesCommentToArgument_whenCommentOnFollowingLineWithinCall() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Test(Object param1) {}",
            "  void test(Object param1) {",
            "    // BUG: Diagnostic contains: [[] param1 [1]]",
            "    new Test(param1",
            "             // 1",
            "            );",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_ignoresNextLineComment_withLineCommentAfterInvocation() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object target(Object param);",
            "  void test(Object param) {",
            "    // BUG: Diagnostic contains: [[] param [1]]",
            "    target(param); // 1",
            "    /* 2 */ int i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      findCommentsForArguments_attachesCommentToSecondArgument_whenFollowedByTreeContainingComma() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param1 [], [] param2 [1]]",
            "    target(param1, param2);  // 1",
            "    // BUG: Diagnostic contains: [[] param1 [], [] param2 []]",
            "    target(param1, param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_attachesCommentToFirstCall_whenMethodIsChained() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Test chain(Object param1);",
            "  abstract void target(Object param2);",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[] param2 []]",
            "    chain(/* 1 */ param1).target(param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_noCommentOnOuterMethod_whenCommentOnNestedMethod() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object nested(Object param);",
            "  abstract void target(Object param);",
            "  void test(Object param) {",
            "    // BUG: Diagnostic contains: [[] nested(param) []]",
            "    target(nested(/* 1 */ param));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findCommentsForArguments_findsCommentOnOuterMethodOnly_whenCommentOnNestedMethod() {
    CompilationTestHelper.newInstance(PrintCommentsForArguments.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Object nested(Object param);",
            "  abstract void target(Object param);",
            "  void test(Object param) {",
            "    // BUG: Diagnostic contains: [[1] nested(param) [4]]",
            "    target(/* 1 */ nested(/* 2 */ param /* 3 */) /* 4 */);",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints the source code at comment positions */
  @BugPattern(
      name = "PrintTextAtCommentPosition",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Prints the source code text which is under the comment position. Matches calls to "
              + "methods called target and constructors only")
  public static class PrintTextAtCommentPosition extends BugChecker
      implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!ASTHelpers.getSymbol(tree).getSimpleName().contentEquals("target")) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(commentsToString(Comments.findCommentsForArguments(tree, state), state))
          .build();
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(commentsToString(Comments.findCommentsForArguments(tree, state), state))
          .build();
    }

    private static String commentsToString(
        ImmutableList<Commented<ExpressionTree>> comments, VisitorState state) {
      return comments.stream()
          .map(
              c ->
                  Stream.of(
                          String.valueOf(getSourceAtComment(c.beforeComments(), state)),
                          String.valueOf(c.tree()),
                          String.valueOf(getSourceAtComment(c.afterComments(), state)))
                      .collect(Collectors.joining(" ")))
          .collect(toImmutableList())
          .toString();
    }

    private static ImmutableList<String> getSourceAtComment(
        ImmutableList<Comment> comments, VisitorState state) {
      return comments.stream()
          .map(
              c ->
                  state
                      .getSourceCode()
                      .subSequence(c.getSourcePos(0), c.getSourcePos(0) + c.getText().length())
                      .toString())
          .collect(toImmutableList());
    }
  }

  @Test
  public void printTextAtCommentPosition_isCorrect_whenMethodIsChained() {
    CompilationTestHelper.newInstance(PrintTextAtCommentPosition.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract Test chain(Object param1);",
            "  abstract void target(Object param2);",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[/* 1 */] param2 []]",
            "    chain(param1).target(/* 1 */ param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void printTextAtCommentPosition_isCorrect_onConstructor() {
    CompilationTestHelper.newInstance(PrintTextAtCommentPosition.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Test(Object param1, Object param2) {}",
            "  void test(Object param1, Object param2) {",
            "    // BUG: Diagnostic contains: [[/* 1 */] param1 [], [] param2 [/* 2 */]]",
            "    new Test(/* 1 */ param1, param2 /* 2 */);",
            "  }",
            "}")
        .doTest();
  }
}
