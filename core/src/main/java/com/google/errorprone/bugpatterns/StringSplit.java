/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

/** @author dturner@twosigma.com (David Turner) */
@BugPattern(
  name = "StringSplit",
  category = JDK,
  summary = "String.split should never take only a single argument; it has surprising behavior",
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class StringSplit extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod().onDescendantOf("java.lang.String").withSignature("split(java.lang.String)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    return describeMatch(
        tree, SuggestedFix.builder().postfixWith(getOnlyElement(arguments), ", -1").build());
  }
}
