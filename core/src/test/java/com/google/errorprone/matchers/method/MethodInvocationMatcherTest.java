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

import static com.google.common.truth.Truth.assertThat;
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
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests a basic combination of a few ordinary method matchers. */
@RunWith(JUnit4.class)
public class MethodInvocationMatcherTest {
  @BugPattern(
      name = "MethodInvocationChecker",
      summary = "Checker that flags the given method invocation if the given matcher matches",
      severity = ERROR)
  static class MethodInvocationChecker extends BugChecker implements MethodInvocationTreeMatcher {
    private final Matcher<ExpressionTree> matcher;

    MethodInvocationChecker(Matcher<ExpressionTree> matcher) {
      this.matcher = matcher;
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return matcher.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
    }
  }

  @Test
  public void invocationMatchers() {
    List<MethodMatchers.MethodMatcher> matchers =
        ImmutableList.of(
            instanceMethod().anyClass().named("toString").withParameters(),
            anyMethod().anyClass().named("valueOf").withParameters("int"),
            staticMethod().anyClass().named("valueOf").withParameters("long"),
            instanceMethod().onDescendantOf("java.lang.Number"));
    assertThat(matchers.stream().allMatch(m -> m.asRule().isPresent())).isTrue();
    Matcher<ExpressionTree> matcher =
        MethodInvocationMatcher.compile(
            matchers.stream()
                .map(m -> m.asRule().orElseThrow(RuntimeException::new))
                .collect(Collectors.toList()));

    CompilationTestHelper.newInstance(methodTreeScanner(matcher), getClass())
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

  private static ScannerSupplier methodTreeScanner(Matcher<ExpressionTree> m) {
    return ScannerSupplier.fromScanner(new ErrorProneScanner(new MethodInvocationChecker(m)));
  }
}
