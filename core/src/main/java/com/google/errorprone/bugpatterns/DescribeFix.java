/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

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

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "DescribeFix",
    summary =
        "`describeFix(tree, fix)` is equivalent to and simpler than"
            + " `buildDescription(tree).addFix(fix).build()`",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class DescribeFix extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> BUILD_DESCRIPTION =
      instanceMethod()
          .onDescendantOf(BugChecker.class.getCanonicalName())
          .named("buildDescription");

  private static final Matcher<ExpressionTree> ADD_FIX =
      instanceMethod().onDescendantOf(Description.Builder.class.getCanonicalName()).named("addFix");

  private static final Matcher<ExpressionTree> BUILD =
      instanceMethod().onDescendantOf(Description.Builder.class.getCanonicalName()).named("build");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree build, VisitorState state) {
    if (!BUILD.matches(build, state)) {
      return NO_MATCH;
    }
    ExpressionTree addFix = ASTHelpers.getReceiver(build);
    if (!ADD_FIX.matches(addFix, state)) {
      return NO_MATCH;
    }
    ExpressionTree buildDescription = ASTHelpers.getReceiver(addFix);
    if (!BUILD_DESCRIPTION.matches(buildDescription, state)) {
      return NO_MATCH;
    }
    return describeMatch(
        build,
        SuggestedFix.replace(
            build,
            String.format(
                "describeMatch(%s, %s)",
                getArgument(state, buildDescription), getArgument(state, addFix))));
  }

  private static String getArgument(VisitorState state, ExpressionTree tree) {
    return state.getSourceForNode(getOnlyElement(((MethodInvocationTree) tree).getArguments()));
  }
}
