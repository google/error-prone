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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for EnclosedByReverseHeuristic
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@RunWith(JUnit4.class)
public class EnclosedByReverseHeuristicTest {

  /** A {@link BugChecker} which runs the EnclosedByReverseHeuristic and prints the result */
  @BugPattern(
    name = "EnclosedByReverseHeuristic",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Run the EnclosedByReverseHeuristic and print result"
  )
  public static class EnclosedByReverseHeuristicChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      boolean result =
          !new EnclosedByReverseHeuristic(ImmutableSet.of("reverse"))
              .isAcceptableChange(Changes.empty(), tree, ASTHelpers.getSymbol(tree), state);
      return buildDescription(tree).setMessage(String.valueOf(result)).build();
    }
  }

  @Test
  public void enclosedByReverse_returnsFalse_whenNotInReverse() {
    CompilationTestHelper.newInstance(EnclosedByReverseHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     // BUG: Diagnostic contains: false",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enclosedByReverse_returnsTrue_withinReverseMethod() {
    CompilationTestHelper.newInstance(EnclosedByReverseHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void reverse(Object first, Object second) {",
            "     // BUG: Diagnostic contains: true",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enclosedByReverse_returnsTrue_nestedInReverseClass() {
    CompilationTestHelper.newInstance(EnclosedByReverseHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Reverse {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second) {",
            "     // BUG: Diagnostic contains: true",
            "     target(second, first);",
            "  }",
            "}")
        .doTest();
  }
}
