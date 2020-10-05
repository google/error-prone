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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.FindIdentifiers.findAllIdents;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;

/** Suggests reusing an existing {@code Clock} in scope rather than creating a new one. */
@BugPattern(
    name = "UseTimeInScope",
    summary =
        "Prefer to reuse time sources rather than creating new ones. Having multiple"
            + " unsynchronized time sources in scope risks accidents.",
    severity = WARNING)
public final class UseTimeInScope extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> NEW_SOURCE_OF_TIME =
      anyOf(
          staticMethod()
              .onClass("java.time.Clock")
              .namedAnyOf("system", "systemDefaultZone", "systemUTC", "fixed"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!NEW_SOURCE_OF_TIME.matches(tree, state)) {
      return NO_MATCH;
    }
    AssignmentTree assignmentTree = state.findEnclosing(AssignmentTree.class);
    Symbol excludedSymbol = assignmentTree == null ? null : getSymbol(assignmentTree.getVariable());
    return findAllIdents(state).stream()
        .filter(s -> isSameType(s.type, getSymbol(tree).owner.type, state))
        .filter(s -> !s.equals(excludedSymbol))
        .findFirst()
        .map(
            s ->
                buildDescription(tree)
                    .addFix(SuggestedFix.replace(tree, s.getSimpleName().toString()))
                    .setMessage(
                        String.format(
                            "There is already a %s in scope here. Prefer to reuse it rather than"
                                + " creating a new one. Having multiple unsynchronized time"
                                + " sources in scope risks accidents.",
                            s.type.tsym.getSimpleName()))
                    .build())
        .orElse(NO_MATCH);
  }
}
