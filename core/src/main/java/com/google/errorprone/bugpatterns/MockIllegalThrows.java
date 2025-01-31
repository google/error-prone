/*
 * Copyright 2025 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCheckedExceptionType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

/** A BugPattern; see the summary */
@BugPattern(summary = "This exception can't be thrown by the mocked method.", severity = WARNING)
public final class MockIllegalThrows extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> WHEN =
      staticMethod().onClass("org.mockito.Mockito").named("when");

  // TODO(ghm): Consider covering doThrow as well, even if we weakly discourage it.
  private static final Matcher<ExpressionTree> THEN_THROW =
      instanceMethod().onDescendantOf("org.mockito.stubbing.OngoingStubbing").named("thenThrow");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!THEN_THROW.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree exceptionTree = tree.getArguments().get(0);
    var thrownType = getType(exceptionTree);
    if (!isCheckedExceptionType(thrownType, state)) {
      return NO_MATCH;
    }
    // Heuristic: if the type being thrown is Exception/Throwable, but doesn't come directly from a
    // constructor, it might be a parameter, and we can't know that it's not always given sensible
    // types.
    if (!(exceptionTree instanceof NewClassTree)
        && (isSameType(thrownType, state.getSymtab().exceptionType, state)
            || isSameType(thrownType, state.getSymtab().throwableType, state))) {
      return NO_MATCH;
    }
    for (var receiver = getReceiver(tree);
        receiver instanceof MethodInvocationTree whenMit;
        receiver = getReceiver(receiver)) {
      if (WHEN.matches(receiver, state)
          && whenMit.getArguments().get(0) instanceof MethodInvocationTree mit
          && getType(mit.getMethodSelect()).getThrownTypes().stream()
              .noneMatch(
                  throwableType -> state.getTypes().isAssignable(thrownType, throwableType))) {
        var thrownTypes = getType(mit.getMethodSelect()).getThrownTypes();
        return buildDescription(whenMit.getArguments().get(0))
            .setMessage(
                thrownTypes.isEmpty()
                    ? format(
                        "%s is not throwable by this method; only unchecked exceptions can be"
                            + " thrown.",
                        thrownType.tsym.getSimpleName())
                    : format(
                        "%s is not throwable by this method; possible exception types are %s, or"
                            + " any unchecked exception.",
                        thrownType.tsym.getSimpleName(),
                        thrownTypes.stream()
                            .map(t -> t.tsym.getSimpleName().toString())
                            .collect(joining(", "))))
            .build();
      }
    }
    return NO_MATCH;
  }
}
