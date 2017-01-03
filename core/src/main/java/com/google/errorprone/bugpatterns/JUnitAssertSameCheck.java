/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

/**
 * Points out if an object is tested for reference equality to itself using JUnit library.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "JUnitAssertSameCheck",
  summary = "An object is tested for reference equality to itself using JUnit library.",
  category = JUNIT,
  severity = ERROR
)
public class JUnitAssertSameCheck extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Cases:
   *
   * <ol>
   *   <li>org.junit.Assert.assertSame(a, a);
   *   <li>org.junit.Assert.assertSame("message", a, a);
   *   <li>junit.framework.Assert.assertSame(a, a);
   *   <li>junit.framework.Assert.assertSame("message", a, a);
   * </ol>
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<ExpressionTree> ASSERT_SAME_MATCHER =
      staticMethod().onClassAny("org.junit.Assert", "junit.framework.Assert").named("assertSame");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!ASSERT_SAME_MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> args = methodInvocationTree.getArguments();

    // cases: assertSame(a, a);
    if (args.size() == 2 && ASTHelpers.sameVariable(args.get(0), args.get(1))) {
      return describeMatch(methodInvocationTree);
    }

    // cases: assertSame("message", a, a);
    if (args.size() == 3 && ASTHelpers.sameVariable(args.get(1), args.get(2))) {
      return describeMatch(methodInvocationTree);
    }
    return Description.NO_MATCH;
  }
}
