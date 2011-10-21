/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;

import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;

/**
 * Matches a static method expression.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class StaticMethodMatcher implements Matcher<ExpressionTree> {
  private final String packageName;
  private final String className;
  private final String methodName;

  public StaticMethodMatcher(String packageName, String className, String methodName) {
    this.packageName = packageName;
    this.className = className;
    this.methodName = methodName;
  }

  @Override
  public boolean matches(ExpressionTree item, VisitorState state) {
    try {
      MemberSelectTree memberSelectTree = (MemberSelectTree) item;
      
      // Case 1: Fully-qualified method call
      if (memberSelectTree.getExpression().getKind() == MEMBER_SELECT &&
          memberSelectTree.getExpression().toString().equals(packageName + "." + className) &&
          memberSelectTree.getIdentifier().contentEquals(methodName)) {
        return true;
      }
      
      // Case 2: Not fully qualified method call -- must check imports
      boolean importFound = false;
      for (ImportTree importTree : state.imports) {
        if (importTree.getQualifiedIdentifier().toString().equals(packageName + "." + className)) {
          importFound = true;
          break;
        }
      }
      if (importFound &&
          memberSelectTree.getExpression().getKind() == IDENTIFIER &&
          memberSelectTree.getExpression().toString().equals(className) &&
          memberSelectTree.getIdentifier().contentEquals(methodName)) {
        return true;
      } 
    } catch (ClassCastException e) {
      return false;
    }
    return false;
  }
}
