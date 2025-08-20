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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "String.join(CharSequence) performs no joining (it always returns the empty string);"
            + " String.join(CharSequence, CharSequence) performs no joining (it just returns the"
            + " 2nd parameter).",
    severity = ERROR)
public final class StringJoin extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("java.lang.String").named("join");

  private static final Supplier<Type> CHAR_SEQUENCE_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.CharSequence"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (MATCHER.matches(tree, state)) {
      if (tree.getArguments().size() == 1) {
        return describeMatch(tree, replace(tree, "\"\""));
      }
      if (tree.getArguments().size() == 2) {
        ExpressionTree arg1 = tree.getArguments().get(1);
        if (isSubtype(getType(arg1), CHAR_SEQUENCE_TYPE.get(state), state)) {
          return describeMatch(tree, replace(tree, state.getSourceForNode(arg1)));
        }
      }
    }
    return NO_MATCH;
  }
}
