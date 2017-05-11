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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Applies an Expression matcher to an argument of a MethodInvocation by position.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class MethodInvocationArgument implements Matcher<MethodInvocationTree> {
  private final int position;
  private final Matcher<ExpressionTree> argumentMatcher;

  public MethodInvocationArgument(int position, Matcher<ExpressionTree> argumentMatcher) {
    this.position = position;
    this.argumentMatcher = argumentMatcher;
  }

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (methodInvocationTree.getArguments().size() <= position) {
      return false;
    }
    return argumentMatcher.matches(methodInvocationTree.getArguments().get(position), state);
  }
}
