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

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * Matches if the given matcher matches all of/any of the parameters to this method.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class MethodHasParameters implements Matcher<MethodTree> {

  private final boolean anyOf;
  private final Matcher<VariableTree> parameterMatcher;

  public MethodHasParameters(boolean anyOf, Matcher<VariableTree> parameterMatcher) {
    this.anyOf = anyOf;
    this.parameterMatcher = parameterMatcher;
  }

  @Override
  public boolean matches(MethodTree methodTree, VisitorState state) {
    // Iterate over members of class (methods and fields).
    for (VariableTree member : methodTree.getParameters()) {
      boolean matches = parameterMatcher.matches(member, state);
      if (anyOf && matches) {
        return true;
      }
      if (!anyOf && !matches) {
        return false;
      }
    }
    if (anyOf) {
      return false;
    } else {
      // In allOf case, return true only if there was at least one parameter.
      return methodTree.getParameters().size() >= 1;
    }
  }
}
