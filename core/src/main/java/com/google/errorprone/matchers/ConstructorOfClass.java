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
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Applies the given matcher to the constructor(s) of the given class.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ConstructorOfClass implements Matcher<ClassTree> {

  private final boolean anyOf;
  private final Matcher<MethodTree> constructorMatcher;

  public ConstructorOfClass(boolean anyOf, Matcher<MethodTree> constructorMatcher) {
    this.anyOf = anyOf;
    this.constructorMatcher = constructorMatcher;
  }

  @Override
  public boolean matches(ClassTree classTree, VisitorState state) {
    int constructorCount = 0;
    // Iterate over members of class (methods and fields).
    for (Tree member : classTree.getMembers()) {
      // If this member is a constructor...
      if (member instanceof MethodTree && ASTHelpers.getSymbol(member).isConstructor()) {
        constructorCount++;
        boolean matches = constructorMatcher.matches((MethodTree) member, state);
        if (anyOf && matches) {
          return true;
        }
        if (!anyOf && !matches) {
          return false;
        }
      }
    }
    if (anyOf) {
      return false;
    } else {
      // In allOf case, return true only if there was at least one constructor.
      return constructorCount >= 1;
    }
  }
}
