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
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "DescribeMatch",
    summary =
        "`describeMatch(tree, fix)` is equivalent to and simpler than"
            + " `buildDescription(tree).addFix(fix).build()`",
    severity = WARNING)
public class DescribeMatch extends BugChecker implements MethodInvocationTreeMatcher {

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
    if (withinBugChecker(state)) {
      return NO_MATCH;
    }
    ExpressionTree addFix = getReceiver(build);
    if (!ADD_FIX.matches(addFix, state)) {
      return NO_MATCH;
    }
    ExpressionTree buildDescription = getReceiver(addFix);
    if (!BUILD_DESCRIPTION.matches(buildDescription, state)) {
      return NO_MATCH;
    }

    return describeMatch(
        build,
        SuggestedFix.replace(
            build,
            String.format(
                "%sdescribeMatch(%s, %s)",
                getReceiverSourceAndDotOrBlank(buildDescription, state),
                getArgumentSource(buildDescription, state),
                getArgumentSource(addFix, state))));
  }

  private static boolean withinBugChecker(VisitorState state) {
    return stream(state.getPath())
        .anyMatch(
            t ->
                t instanceof ClassTree
                    && getSymbol(t)
                        .getQualifiedName()
                        .contentEquals(BugChecker.class.getCanonicalName()));
  }

  private static String getReceiverSourceAndDotOrBlank(ExpressionTree tree, VisitorState state) {
    ExpressionTree receiver = getReceiver(tree);
    return receiver != null ? state.getSourceForNode(receiver) + "." : "";
  }

  private static String getArgumentSource(ExpressionTree tree, VisitorState state) {
    return state.getSourceForNode(getOnlyElement(((MethodInvocationTree) tree).getArguments()));
  }
}
