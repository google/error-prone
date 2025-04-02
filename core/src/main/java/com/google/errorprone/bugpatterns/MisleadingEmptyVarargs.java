/*
 * Copyright 2025 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = ERROR,
    summary =
        "`thenThrow` with no arguments is a no-op, despite reading like it makes the mock throw.")
public final class MisleadingEmptyVarargs extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MOCKITO_THROW =
      anyOf(
          instanceMethod()
              .onDescendantOf("org.mockito.stubbing.OngoingStubbing")
              .named("thenThrow"),
          staticMethod().onClass("org.mockito.Mockito").named("doThrow"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MOCKITO_THROW.matches(tree, state) && tree.getArguments().isEmpty()
        ? describeMatch(tree)
        : NO_MATCH;
  }
}
