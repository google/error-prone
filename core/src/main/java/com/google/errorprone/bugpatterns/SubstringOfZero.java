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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Objects;

/** @author seibelsabrina@google.com (Sabrina Seibel) */
/** Check for calls to String's {@code foo.substring(0)}. */
@BugPattern(
    name = "SubstringOfZero",
    summary = "String.substring(0) returns the original String",
    explanation =
        "String.substring(int) gives you the substring from the index to the end, inclusive."
            + "Calling that method with an index of 0 will return the same String.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class SubstringOfZero extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> SUBSTRING_CALLS =
      Matchers.instanceMethod()
          .onExactClass("java.lang.String")
          .named("substring")
          .withParameters("int");

  private static final Matcher<MethodInvocationTree> ARGUMENT_IS_ZERO =
      Matchers.argument(0, (tree, state) -> Objects.equals(ASTHelpers.constValue(tree), 0));

  private static final Matcher<MethodInvocationTree> SUBSTRING_CALLS_WITH_ZERO_ARG =
      Matchers.allOf(SUBSTRING_CALLS, ARGUMENT_IS_ZERO);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!SUBSTRING_CALLS_WITH_ZERO_ARG.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, removeSubstringCall(tree, state));
  }

  private static Fix removeSubstringCall(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree originalString = ASTHelpers.getReceiver(tree);
    return SuggestedFix.replace(tree, state.getSourceForNode(originalString));
  }
}
