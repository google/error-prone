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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.FieldMatchers.staticField;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "SystemOut",
    summary =
        "Printing to standard output should only be used for debugging, not in production code",
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR,
    providesFix = ProvidesFix.NO_FIX)
public class SystemOut extends BugChecker
    implements MethodInvocationTreeMatcher, MemberSelectTreeMatcher {

  private static final Matcher<ExpressionTree> SYSTEM_OUT =
      anyOf(
          staticField(System.class.getName(), "out"), //
          staticField(System.class.getName(), "err"));

  private static final Matcher<ExpressionTree> PRINT_STACK_TRACE =
      anyOf(
          staticMethod().onClass(Thread.class.getName()).named("dumpStack").withParameters(),
          instanceMethod()
              .onDescendantOf(Throwable.class.getName())
              .named("printStackTrace")
              .withParameters());

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (SYSTEM_OUT.matches(tree, state)) {
      return describeMatch(tree);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (PRINT_STACK_TRACE.matches(tree, state)) {
      return describeMatch(tree);
    }
    return NO_MATCH;
  }
}
