/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

/** @author alexeagle@google.com (Alex Eagle) */
@BugPattern(
  name = "PreconditionsCheckNotNull",
  summary = "Literal passed as first argument to Preconditions.checkNotNull() can never be null",
  explanation =
      "Preconditions.checkNotNull() takes two arguments. The first is the reference "
          + "that should be non-null. The second is the error message to print (usually a string "
          + "literal). Often the order of the two arguments is swapped, and the reference is "
          + "never actually checked for nullity. This check ensures that the first argument to "
          + "Preconditions.checkNotNull() is not a literal.",
  category = GUAVA,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class PreconditionsCheckNotNull extends BugChecker implements MethodInvocationTreeMatcher {

  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> matcher =
      allOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          argument(0, Matchers.<ExpressionTree>kindIs(STRING_LITERAL)));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!matcher.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    ExpressionTree stringLiteralValue = arguments.get(0);
    Fix fix;
    if (arguments.size() == 2) {
      fix = SuggestedFix.swap(arguments.get(0), arguments.get(1));
    } else {
      fix = SuggestedFix.delete(state.getPath().getParentPath().getLeaf());
    }
    return describeMatch(stringLiteralValue, fix);
  }
}
