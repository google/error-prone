/*
 * Copyright 2018 The Error Prone Authors.
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

/** {@link DiscardedPostfixExpression}Test */
@RunWith(JUnit4.class)
public final class DiscardedPostfixExpressionTest {
  @Test
  public void negative() {
    CompilationTestHelper.newInstance(DiscardedPostfixExpression.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.function.UnaryOperator;",
            "class Test {",
            "  int x;",
            "  UnaryOperator<Integer> f = x1 -> x++;",
            "  UnaryOperator<Integer> g = x1 -> x--;",
            "  UnaryOperator<Integer> h = x -> ++x;",
            "  UnaryOperator<Integer> i = x -> --x;",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new DiscardedPostfixExpression(), getClass())
        .addInputLines(
            "Test.java",
            "import java.util.function.UnaryOperator;",
            "class Test {",
            "  UnaryOperator<Integer> f = x -> x++;",
            "  UnaryOperator<Integer> g = x -> x--;",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.UnaryOperator;",
            "class Test {",
            "  UnaryOperator<Integer> f = x -> x + 1;",
            "  UnaryOperator<Integer> g = x -> x - 1;",
            "}")
        .doTest();
  }
}
