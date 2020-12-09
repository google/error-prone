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
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;

/** @author Graeme Morgan (ghm@google.com) */
@BugPattern(
    name = "FloggerLogVarargs",
    summary = "logVarargs should be used to pass through format strings and arguments.",
    severity = ERROR)
public final class FloggerLogVarargs extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(
          instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log"),
          argument(0, isSameType(Suppliers.STRING_TYPE)),
          argument(1, isSameType(Suppliers.arrayOf(Suppliers.OBJECT_TYPE))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    MethodTree enclosingMethod = state.findEnclosing(MethodTree.class);
    if (enclosingMethod == null) {
      return NO_MATCH;
    }
    // Heuristically, a compile time constant format string might be intended to be used with an
    // actual array, but a non-CTC probably isn't.
    return ASTHelpers.constValue(tree.getArguments().get(0)) == null
        ? describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "logVarargs", state))
        : NO_MATCH;
  }
}
