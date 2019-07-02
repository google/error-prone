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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;

/**
 * Checks for unnecessarily performing null checks on expressions which can't be null.
 *
 * @author awturner@google.com (Andy Turner)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "UnnecessaryCheckNotNull",
    summary = "This null check is unnecessary; the expression can never be null",
    severity = ERROR,
    altNames = "PreconditionsCheckNotNull",
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class UnnecessaryCheckNotNull extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> CHECK_NOT_NULL_MATCHER =
      Matchers.<MethodInvocationTree>anyOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          staticMethod().onClass("com.google.common.base.Verify").named("verifyNotNull"),
          staticMethod().onClass("java.util.Objects").named("requireNonNull"));

  private static final Matcher<MethodInvocationTree> NEW_INSTANCE_MATCHER =
      argument(
          0, Matchers.<ExpressionTree>kindAnyOf(ImmutableSet.of(Kind.NEW_CLASS, Kind.NEW_ARRAY)));

  private static final Matcher<MethodInvocationTree> STRING_LITERAL_ARG_MATCHER =
      argument(0, Matchers.<ExpressionTree>kindIs(STRING_LITERAL));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CHECK_NOT_NULL_MATCHER.matches(tree, state) || tree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (NEW_INSTANCE_MATCHER.matches(tree, state)) {
      return matchNewInstance(tree, state);
    }
    if (STRING_LITERAL_ARG_MATCHER.matches(tree, state)) {
      return matchStringLiteral(tree, state);
    }
    return Description.NO_MATCH;
  }

  private Description matchNewInstance(MethodInvocationTree tree, VisitorState state) {
    Fix fix = SuggestedFix.replace(tree, state.getSourceForNode(tree.getArguments().get(0)));
    return describeMatch(tree, fix);
  }

  private Description matchStringLiteral(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
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
