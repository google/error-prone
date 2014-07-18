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

import static com.google.errorprone.matchers.MultiMatcher.MatchType.ALL;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * Matches if the given matcher matches all of/any of the parameters to this method.
 *
 * TODO(user): All MultiMatchers seem to have a similar looping structure, applying a given
 * matcher to a set of nodes.  Consider refactoring this code into the base class for better
 * reuse.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class MethodHasParameters extends MultiMatcher<MethodTree, VariableTree> {

  public MethodHasParameters(MatchType matchType, Matcher<VariableTree> nodeMatcher) {
    super(matchType, nodeMatcher);
  }

  @Override
  public boolean matches(MethodTree methodTree, VisitorState state) {
    // Iterate over members of class (methods and fields).
    for (VariableTree member : methodTree.getParameters()) {
      boolean matches = nodeMatcher.matches(member, state);
      if (matchType == ANY && matches) {
        matchingNode = member;
        return true;
      }
      if (matchType == ALL && !matches) {
        return false;
      }
    }
    return matchType == ALL && methodTree.getParameters().size() >= 1;
  }
}
