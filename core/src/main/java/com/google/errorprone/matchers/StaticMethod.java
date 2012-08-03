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
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Name;

/**
 * Matches a static method expression.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class StaticMethod implements Matcher<ExpressionTree> {
  private final String fullClass;
  private final String methodName;

  public StaticMethod(String fullClass, String methodName) {
    this.fullClass = fullClass;
    this.methodName = methodName;
  }

  @Override
  public boolean matches(ExpressionTree item, VisitorState state) {
    if (!(item instanceof JCFieldAccess)) {
      return false;
    }
    JCFieldAccess memberSelectTree = (JCFieldAccess) item;

    // Is method static?
    if (memberSelectTree.sym == null || !memberSelectTree.sym.isStatic()) {
      return false;
    }

    Name fullClassName = state.getName(fullClass);
    boolean methodSame = memberSelectTree.sym.getQualifiedName().equals(state.getName(methodName));
    if (methodSame &&
        memberSelectTree.sym.owner.getQualifiedName().equals(fullClassName)) {
      return true;
    }

    if (!(memberSelectTree.getExpression() instanceof JCIdent)) {
      return false;
    }
    JCIdent expressionTree = (JCIdent) memberSelectTree.getExpression();
    return methodSame && expressionTree.sym.getQualifiedName().equals(fullClassName);
  }
}
