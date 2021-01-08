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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Bug checker to detect usage of {@code Thread.stop()}, {@code Thread.yield()}, and changing thread
 * priorities.
 *
 * @author siyuanl@google.com (Siyuan Liu)
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    name = "ThreadPriorityCheck",
    summary = "Relying on the thread scheduler is discouraged.",
    severity = WARNING)
public class ThreadPriorityCheck extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> THREAD_MATCHERS =
      anyOf(
          Matchers.staticMethod().onClass("java.lang.Thread").named("yield"),
          Matchers.instanceMethod().onDescendantOf("java.lang.Thread").named("setPriority"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return THREAD_MATCHERS.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
