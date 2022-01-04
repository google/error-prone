/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.matchers.method;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests a basic combination of a few ordinary method matchers. */
@RunWith(JUnit4.class)
public class MethodInvocationMatcherTest {

  @Test
  public void invocationMatchers() {

    CompilationTestHelper.newInstance(MethodInvocationChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public String toString() {",
            "    System.out.println(\"Stringifying\");",
            "    // BUG: Diagnostic contains: ",
            "    String s = \"5\".toString();",
            "    // BUG: Diagnostic contains: ",
            "    int result = Integer.valueOf(5).compareTo(6);",
            "    // BUG: Diagnostic contains: ",
            "    return String.valueOf(5);",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} for test. */
  @BugPattern(
      name = "MethodInvocationChecker",
      summary = "Checker that flags the given method invocation if the matcher matches",
      severity = ERROR)
  public static class MethodInvocationChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    private final Matcher<ExpressionTree> matcher;

    public MethodInvocationChecker() {
      List<MethodMatchers.MethodMatcher> matchers =
          ImmutableList.of(
              instanceMethod().anyClass().named("toString").withNoParameters(),
              anyMethod().anyClass().named("valueOf").withParameters("int"),
              staticMethod().anyClass().named("valueOf").withParameters("long"),
              instanceMethod().onDescendantOf("java.lang.Number"));
      this.matcher = Matchers.anyOf(matchers);
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return matcher.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
    }
  }
}
