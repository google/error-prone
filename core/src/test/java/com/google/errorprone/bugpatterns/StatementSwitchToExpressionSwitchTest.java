/*
 * Copyright 2022 The Error Prone Authors.
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

import static org.junit.Assume.assumeTrue;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StatementSwitchToExpressionSwitch}. */
@RunWith(JUnit4.class)
public final class StatementSwitchToExpressionSwitchTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(StatementSwitchToExpressionSwitch.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          StatementSwitchToExpressionSwitch.class, getClass());

  @Test
  public void switchByEnum_removesRedundantBreak_error() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case OBVERSE:",
            "          // Explanatory comment",
            "          System.out.println(\"the front is called the\");",
            "          // Middle comment",
            "          System.out.println(\"obverse\");",
            "          // Break comment",
            "          break;",
            "          // End comment",
            "       case REVERSE:",
            "          System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case OBVERSE /* left comment */ /* and there is more: */ // to end of line",
            "            :",
            "          // Explanatory comment",
            "          System.out.println(\"the front is called the\");",
            "          // Middle comment",
            "          System.out.println(\"obverse\");",
            "          // Break comment",
            "          break;",
            "          // End comment",
            "       case REVERSE:",
            "          System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case OBVERSE -> { /* left comment */",
            "          /* and there is more: */",
            "          // to end of line",
            "          // Explanatory comment",
            "          System.out.println(\"the front is called the\");",
            "          // Middle comment",
            "          System.out.println(\"obverse\");",
            "          // Break comment",
            "       }",
            "       case REVERSE -> System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumWithCompletionAnalsis_removesRedundantBreak_error() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case OBVERSE:",
            "          // Explanatory comment",
            "          System.out.println(\"this block cannot complete normally\");",
            "          {",
            "            throw new NullPointerException();",
            "          }",
            "       case REVERSE:",
            "          System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case OBVERSE:",
            "          // Explanatory comment",
            "          System.out.println(\"this block cannot complete normally\");",
            "          {",
            "            throw new NullPointerException();",
            "          }",
            "       case REVERSE:",
            "          System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {OBVERSE, REVERSE};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case OBVERSE -> {",
            "          // Explanatory comment",
            "          System.out.println(\"this block cannot complete normally\");",
            "          {",
            "            throw new NullPointerException();",
            "          }",
            "       }",
            "       case REVERSE -> ",
            "          System.out.println(\"reverse\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCard_combinesCaseComments_error() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          // Empty block comment 1",
            "          // Fall through",
            "       case SPADE:",
            "          // Empty block comment 2",
            "       case CLUB: ",
            "          // Start of block comment 1",
            "          System.out.println(\"what's not a heart is \");",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart2\");",
            "          break;",
            "       case /* sparkly */ DIAMOND:",
            "          // Empty block comment 1",
            "          // Fall through",
            "       case SPADE:",
            "          // Empty block comment 2",
            "       case CLUB: ",
            "          // Start of block comment 1",
            "          System.out.println(\"what's not a heart is \");",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART -> System.out.println(\"heart2\");",
            "       case DIAMOND, SPADE, CLUB -> { /* sparkly */",
            "          // Empty block comment 1",
            "          // Empty block comment 2",
            "          // Start of block comment 1",
            "          System.out.println(\"what's not a heart is \");",
            "          System.out.println(\"everything else\");",
            "       }",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCard2_removesRedundantBreaks_error() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          // Pre break comment",
            "          break;",
            "          // Post break comment",
            "       case DIAMOND:",
            "          // Diamond break comment",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          // Pre break comment",
            "          break;",
            "          // Post break comment",
            "       case DIAMOND:",
            "          // Diamond break comment",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART -> {",
            "          System.out.println(\"heart\");",
            "          // Pre break comment",
            "       }",
            "       case DIAMOND -> {",
            "          // Diamond break comment",
            "          break;",
            "       }",
            "       case SPADE, CLUB -> System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCard_onlyExpressionsAndThrowAreBraceless_error() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    for(;;) {",
            "      // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "      switch(side) {",
            "         case HEART:",
            "            System.out.println(\"heart\");",
            "            break;",
            "         case DIAMOND:",
            "            continue;",
            "         case SPADE:",
            "            return;",
            "         case CLUB:",
            "            throw new AssertionError();",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    for(;;) {",
            "      switch(side) {",
            "         case HEART:",
            "            System.out.println(\"heart\");",
            "            break;",
            "         case DIAMOND:",
            "            continue;",
            "         case SPADE:",
            "            return;",
            "         case CLUB:",
            "            throw new AssertionError();",
            "      }",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    for(;;) {",
            "      switch(side) {",
            "         case HEART ->",
            "            System.out.println(\"heart\");",
            "         case DIAMOND -> {",
            "            continue;",
            "         }",
            "         case SPADE -> {",
            "            return;",
            "         }",
            "         case CLUB -> throw new AssertionError();",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchFallsThruToDefault_noError() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       default:",
            "          System.out.println(\"spade or club\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchFallsThruFromDefault_noError() {

    // Placing default in the middle of the switch is not recommended, but is valid Java
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          System.out.println(\"diamond\");",
            "       default:",
            "          System.out.println(\"club\");",
            "          break;",
            "       case SPADE:",
            "          System.out.println(\"spade\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchWithDefaultInMiddle_error() {

    // Placing default in the middle of the switch is not recommended, but is valid Java
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          System.out.println(\"diamond\");",
            "          return;",
            "       default:",
            "          System.out.println(\"club\");",
            "          break;",
            "       case SPADE:",
            "          System.out.println(\"spade\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // This check does not attempt to re-order cases, for example to move the default to the end, as
    // this scope is delegated to other tests e.g. SwitchDefault
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          System.out.println(\"diamond\");",
            "          return;",
            "       default /* comment: */:",
            "          System.out.println(\"club\");",
            "          break;",
            "       case SPADE:",
            "          System.out.println(\"spade\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART -> System.out.println(\"heart\");",
            "       case DIAMOND -> {",
            "          System.out.println(\"diamond\");",
            "          return;",
            "       }",
            "       default -> { /* comment: */",
            "         System.out.println(\"club\");",
            "       }",
            "       case SPADE -> System.out.println(\"spade\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchWithLabelledBreak_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    outer:",
            "    for(;;) {",
            "      // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "      switch(side) {",
            "         case HEART:",
            "            System.out.println(\"will return\");",
            "            return;",
            "         case DIAMOND:",
            "            break outer;",
            "         case SPADE:",
            "         case CLUB:",
            "            System.out.println(\"everything else\");",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    outer:",
            "    for(;;) {",
            "      switch(side) {",
            "         case HEART:",
            "            System.out.println(\"will return\");",
            "            return;",
            "         case DIAMOND:",
            "            break outer;",
            "         case SPADE:",
            "         case CLUB:",
            "            System.out.println(\"everything else\");",
            "      }",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    outer:",
            "    for(;;) {",
            "      switch(side) {",
            "         case HEART -> { ",
            "            System.out.println(\"will return\");",
            "            return;",
            "         }",
            "         case DIAMOND -> {break outer;}",
            "         case SPADE, CLUB -> System.out.println(\"everything else\");",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnum_statementSwitchWithMultipleExpressions_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"will return\");",
            "          return;",
            "       case DIAMOND:",
            "       case SPADE, CLUB:",
            "            System.out.println(\"everything else\");",
            "      }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"will return\");",
            "          return;",
            "       case DIAMOND:",
            "       case SPADE, CLUB:",
            "            System.out.println(\"everything else\");",
            "      }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART -> {",
            "          System.out.println(\"will return\");",
            "          return;",
            "       }",
            "       case DIAMOND, SPADE, CLUB ->",
            "            System.out.println(\"everything else\");",
            "      }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithThrow_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"will return\");",
            "          return;",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchInSwitch_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "           // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "           switch(side) {",
            "             case HEART: ",
            "             case SPADE: ",
            "               System.out.println(\"non-default\");",
            "               break;",
            "             default: ",
            "               System.out.println(\"do nothing\");",
            "          }",
            "          break; ",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithReturnNested1_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          { System.out.println(\"nested1\"); break; }",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();

    // Check correct generated code
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          { System.out.println(\"nested1\"); break; }",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART-> System.out.println(\"heart\");",
            "       case DIAMOND -> {{ System.out.println(\"nested1\"); break; }}",
            "       case SPADE, CLUB -> System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumCardWithReturnNested2_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "       case DIAMOND:",
            "          { System.out.println(\"nested2a\"); ",
            "            {System.out.println(\"nested2b\"); break; } ",
            "          }",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumWithConditionalControl_noError() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          if(true) {",
            "            break;",
            "          }",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void switchByEnumWithLambda_noError() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            " import java.util.function.Function;",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            // "Last" statement in the HEART block is a return, but we don't want to conclude that
            // the block has definite control flow based on that
            "          Function<Integer,Integer> x = (i) -> {while(true) {break;} return i;};",
            "       case DIAMOND:",
            "          break;",
            "       case SPADE:",
            "       case CLUB:",
            "          System.out.println(\"everything else\");",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void singleCaseConvertible_error() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  enum Side {HEART, SPADE, DIAMOND, CLUB};",
            "  public Test(int foo) {",
            "  }",
            " ",
            "  public void foo(Side side) { ",
            "    // BUG: Diagnostic contains: [StatementSwitchToExpressionSwitch]",
            "    switch(side) {",
            "       case HEART:",
            "          System.out.println(\"heart\");",
            "          break;",
            "    }",
            "  }",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }

  @Test
  public void emptyExpressionSwitchCases_noMatch() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int value) { ",
            "    switch (value) {",
            "      case 0 -> {}",
            "      default -> {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonEmptyExpressionSwitchCases_noMatch() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo(int value) { ",
            "    switch (value) {",
            "      case 0 -> System.out.println(\"zero\");",
            "      default -> {System.out.println(\"non-zero\");}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dynamicWithThrowableDuringInitializationFromMethod_noMatch() {
    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Throwable foo = bar(); ",
            "  public Test(int foo) {",
            "  } ",
            " ",
            "  private static Throwable bar() { ",
            "    return new NullPointerException(\"initialized with return value\"); ",
            "  } ",
            "}")
        .setArgs(
            ImmutableList.of("-XepOpt:StatementSwitchToExpressionSwitch:EnableDirectConversion"))
        .doTest();
  }
}
