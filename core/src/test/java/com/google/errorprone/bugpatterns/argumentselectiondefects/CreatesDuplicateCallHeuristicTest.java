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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
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
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for EnclosedByReverseHeuristic
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@RunWith(JUnit4.class)
public class CreatesDuplicateCallHeuristicTest {

  /** A {@link BugChecker} which runs the CreatesDuplicateCallHeuristic and prints the result */
  @BugPattern(
    name = "CreatesDuplicateCallHeuristicChecker",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Runs CreateDuplicateCallHeursitic and prints the result"
  )
  public static class CreatesDuplicateCallHeuristicChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      ImmutableList<Parameter> formal =
          Parameter.createListFromVarSymbols(ASTHelpers.getSymbol(tree).getParameters());
      Stream<Parameter> actual =
          Parameter.createListFromExpressionTrees(tree.getArguments()).stream();

      Changes changes =
          Changes.create(
              formal.stream().map(f -> 1.0).collect(toImmutableList()),
              formal.stream().map(f -> 0.0).collect(toImmutableList()),
              Streams.zip(formal.stream(), actual, ParameterPair::create)
                  .collect(toImmutableList()));

      boolean result =
          !new CreatesDuplicateCallHeuristic()
              .isAcceptableChange(changes, tree, ASTHelpers.getSymbol(tree), state);
      return buildDescription(tree).setMessage(String.valueOf(result)).build();
    }
  }

  @Test
  public void createsDuplicateCall_returnsTrue_withDuplicateCallInMethod() {
    CompilationTestHelper.newInstance(CreatesDuplicateCallHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  void test(Object first, Object second, Object normal) {",
            "     // BUG: Diagnostic contains: true",
            "     target(first, second);",
            "     // BUG: Diagnostic contains: true",
            "     target(first, second);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void createsDuplicateCall_returnsTrue_withDuplicateCallInField() {
    CompilationTestHelper.newInstance(CreatesDuplicateCallHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: true",
            "  Object o1 = target(getFirst(), getSecond());",
            "  // BUG: Diagnostic contains: true",
            "  Object o2 = target(getFirst(), getSecond());",
            "  abstract Object getFirst();",
            "  abstract Object getSecond();",
            "  abstract Object target(Object first, Object second);",
            "}")
        .doTest();
  }

  @Test
  public void createsDuplicateCall_returnsTrue_withDuplicateEnclosingMethod() {
    CompilationTestHelper.newInstance(CreatesDuplicateCallHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void test(Object param1, Object param2) {",
            "     // BUG: Diagnostic contains: true",
            "     test(param1, param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void createsDuplicateCall_returnsFalse_withNoDuplicateCall() {
    CompilationTestHelper.newInstance(CreatesDuplicateCallHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1, Object param2);",
            "  void test(Object param1, Object param2) {",
            "     // BUG: Diagnostic contains: false",
            "     target(param1, param2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void createsDuplicateCall_returnsFalse_withCallWithDifferentConstant() {
    CompilationTestHelper.newInstance(CreatesDuplicateCallHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(Object param1);",
            "  void test() {",
            "     // BUG: Diagnostic contains: false",
            "     target(1);",
            "     // BUG: Diagnostic contains: false",
            "     target(2);",
            "  }",
            "}")
        .doTest();
  }
}
