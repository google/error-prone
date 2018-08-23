/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.assertNotNull;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.StatementTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link NextStatement}. */
@RunWith(JUnit4.class)
public final class NextStatementTest {

  /** A bugchecker to test the ability to notice the 'next statement' */
  @BugPattern(
      name = "CompoundAssignmentBeforeReturn",
      category = JDK,
      summary = "This is a compound assignment before another statement in the same block",
      severity = ERROR)
  public static class CompoundBeforeAnythingChecker extends BugChecker
      implements BugChecker.CompoundAssignmentTreeMatcher {

    @Override
    public Description matchCompoundAssignment(CompoundAssignmentTree cat, VisitorState state) {
      StatementTree exprStat =
          ASTHelpers.findEnclosingNode(state.getPath(), ExpressionStatementTree.class);
      assertNotNull(exprStat);
      if (new NextStatement<StatementTree>(Matchers.<StatementTree>anything())
          .matches(exprStat, state)) {
        return describeMatch(cat);
      }
      return Description.NO_MATCH;
    }
  }

  // If a statement is inside an if statement with no block braces, the NextStatement should return
  // false, since there's no other statement inside the block.
  @Test
  public void testSingleStatementBlock() {
    CompilationTestHelper.newInstance(CompoundBeforeAnythingChecker.class, getClass())
        .addSourceLines(
            "B.java",
            "public class B {",
            "  public boolean getHash() {",
            "    int a = 0;",
            "    if (true) a += 1;",
            "    else a += 2;",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNextStatementInBlock() {
    CompilationTestHelper.newInstance(CompoundBeforeAnythingChecker.class, getClass())
        .addSourceLines(
            "A.java",
            "public class A {",
            "  public boolean getHash() {",
            "    int a = 0;",
            "    if (a == 0) {",
            "      // BUG: Diagnostic contains: This is a compound assignment",
            "      a += 1;",
            "      return true;",
            "    }",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }
}
