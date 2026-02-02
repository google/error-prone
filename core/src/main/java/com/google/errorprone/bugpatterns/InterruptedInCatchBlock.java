/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.UnusedReturnValueMatcher.isReturnValueUnused;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Did you mean to call Thread.currentThread().interrupt() instead of Thread.interrupted()?",
    severity = WARNING)
public final class InterruptedInCatchBlock extends BugChecker implements CatchTreeMatcher {

  private static final Matcher<ExpressionTree> INTERRUPTED_MATCHER =
      staticMethod().onClass("java.lang.Thread").named("interrupted").withNoParameters();

  @Override
  public Description matchCatch(CatchTree tree, VisitorState state) {
    Type type = getType(tree.getParameter());
    Type interruptedType = state.getSymtab().interruptedExceptionType;
    if (type == null || !isSubtype(type, interruptedType, state)) {
      return NO_MATCH;
    }

    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
        VisitorState localState = state.withPath(getCurrentPath());
        if (INTERRUPTED_MATCHER.matches(invocation, localState)) {
          Description.Builder description = buildDescription(invocation);
          if (isReturnValueUnused(invocation, localState)) {
            description.addFix(
                SuggestedFix.replace(invocation, "Thread.currentThread().interrupt()"));
          }
          state.reportMatch(description.build());
        }
        return super.visitMethodInvocation(invocation, null);
      }
    }.scan(state.getPath(), null);

    return NO_MATCH;
  }
}
