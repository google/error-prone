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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.variableType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/** Bugpattern to prevent splitting flogger log invocations into multiple statements. */
@BugPattern(
    name = "FloggerSplitLogStatement",
    summary = "Splitting log statements and using Api instances directly breaks logging.",
    severity = ERROR)
public final class FloggerSplitLogStatement extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {

  private static final Matcher<Tree> IS_LOGGER_API =
      isSubtypeOf("com.google.common.flogger.LoggingApi");

  private static final Matcher<Tree> CLASS_MATCHES =
      not(
          anyOf(
              enclosingClass(isSubtypeOf("com.google.common.flogger.AbstractLogger")),
              enclosingClass(isSubtypeOf("com.google.common.flogger.LoggingApi"))));

  private static final Matcher<MethodTree> METHOD_MATCHES =
      allOf(methodReturns(IS_LOGGER_API), CLASS_MATCHES);

  private static final Matcher<VariableTree> VARIABLE_MATCHES =
      allOf(variableType(IS_LOGGER_API), CLASS_MATCHES);

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return METHOD_MATCHES.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return VARIABLE_MATCHES.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
  }
}
