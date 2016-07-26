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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Applies the given matcher to the constructor(s) of the given class.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ConstructorOfClass extends ChildMultiMatcher<ClassTree, MethodTree> {

  public ConstructorOfClass(MatchType matchType, Matcher<MethodTree> nodeMatcher) {
    super(matchType, nodeMatcher);
  }

  @Override
  protected Iterable<? extends MethodTree> getChildNodes(ClassTree classTree, VisitorState state) {
    ImmutableList.Builder<MethodTree> result = ImmutableList.builder();
    // Iterate over members of class (methods and fields).
    for (Tree member : classTree.getMembers()) {
      // If this member is a constructor...
      if (member instanceof MethodTree && ASTHelpers.getSymbol(member).isConstructor()) {
        result.add((MethodTree) member);
      }
    }
    return result.build();
  }
}
