/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "RemovedInJDK11",
    summary = "This API is no longer supported in JDK 11",
    severity = ERROR)
public class RemovedInJDK11 extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(
          staticMethod()
              .onClassAny("java.lang.Runtime", "java.lang.System")
              .named("runFinalizersOnExit"),
          instanceMethod()
              .onExactClass("java.lang.SecurityManager")
              .namedAnyOf(
                  "checkAwtEventQueueAccess",
                  "checkMemberAccess",
                  "checkSystemClipboardAccess",
                  "checkTopLevelWindow"),
          instanceMethod()
              .onExactClass("java.lang.Thread")
              .named("stop")
              .withParameters("java.lang.Throwable"),
          instanceMethod().onExactClass("java.lang.Thread").named("destroy"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
  }
}
