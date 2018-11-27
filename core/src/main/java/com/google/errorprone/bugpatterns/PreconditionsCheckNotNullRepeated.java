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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

/**
 * Checks that Precondition.checkNotNull is not invoked with same arg twice.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "PreconditionsCheckNotNullRepeated",
    summary =
        "Including this argument in the failure message isn't helpful,"
            + " since its value will always be `null`.",
    category = GUAVA,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class PreconditionsCheckNotNullRepeated extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    if (methodInvocationTree.getArguments().size() < 2) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
    int numArgs = args.size();
    for (int i = 1; i < numArgs; i++) {
      if (!ASTHelpers.sameVariable(args.get(0), args.get(i))) {
        continue;
      }
      // Special case in case there are only two args and they're same.
      // checkNotNull(T reference, Object errorMessage)
      if (numArgs == 2 && i == 1) {
        return describeMatch(
            args.get(1),
            SuggestedFix.replace(
                args.get(1),
                String.format("\"%s must not be null\"", state.getSourceForNode(args.get(0)))));
      }
      return describeMatch(methodInvocationTree);
    }
    return Description.NO_MATCH;
  }
}
