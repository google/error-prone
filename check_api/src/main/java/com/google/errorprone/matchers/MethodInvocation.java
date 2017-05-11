/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Matches a method invocation based on a matcher for the method select (receiver + method
 * identifier) and one for the arguments.
 *
 * @author schmitt@google.com (Peter Schmitt)
 */
public class MethodInvocation implements Matcher<ExpressionTree> {

  private final Matcher<ExpressionTree> methodSelectMatcher;
  private final MethodArgumentMatcher methodArgumentMatcher;

  /**
   * Creates a new matcher for method invocations based on a method select and an argument matcher.
   *
   * @param matchType how to apply the argument matcher to the method's arguments
   */
  public MethodInvocation(
      Matcher<ExpressionTree> methodSelectMatcher,
      MatchType matchType,
      Matcher<ExpressionTree> methodArgumentMatcher) {
    this.methodSelectMatcher = methodSelectMatcher;
    this.methodArgumentMatcher = new MethodArgumentMatcher(matchType, methodArgumentMatcher);
  }

  @Override
  public boolean matches(ExpressionTree expressionTree, VisitorState state) {
    if (!(expressionTree instanceof MethodInvocationTree)) {
      return false;
    }

    MethodInvocationTree tree = (MethodInvocationTree) expressionTree;

    return methodSelectMatcher.matches(tree.getMethodSelect(), state)
        && methodArgumentMatcher.matches(tree, state);
  }

  private static class MethodArgumentMatcher
      extends ChildMultiMatcher<MethodInvocationTree, ExpressionTree> {

    public MethodArgumentMatcher(MatchType matchType, Matcher<ExpressionTree> nodeMatcher) {
      super(matchType, nodeMatcher);
    }

    @Override
    protected Iterable<? extends ExpressionTree> getChildNodes(
        MethodInvocationTree tree, VisitorState state) {
      return tree.getArguments();
    }
  }
}
