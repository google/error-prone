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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Detects invocations of {@code LoggingApi.log(String)} for which the argument is not a
 * compile-time constant and provides suggested alternatives.
 *
 * <p>Currently the suggestions are only made in the error message; eventually most of these things
 * should be actual suggested fixes.
 */
@BugPattern(
    summary =
        "Arguments to log(String) must be compile-time constants or parameters annotated with"
            + " @CompileTimeConstant. If possible, use Flogger's formatting log methods instead.",
    linkType = NONE,
    severity = ERROR)
public class FloggerLogString extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LOG_STRING =
      instanceMethod()
          .onDescendantOf("com.google.common.flogger.LoggingApi")
          .named("log")
          .withParameters("java.lang.String");

  private static final Matcher<ExpressionTree> COMPILE_TIME_CONSTANT =
      CompileTimeConstantExpressionMatcher.instance();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return LOG_STRING.matches(tree, state)
            && !COMPILE_TIME_CONSTANT.matches(tree.getArguments().get(0), state)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }
}
