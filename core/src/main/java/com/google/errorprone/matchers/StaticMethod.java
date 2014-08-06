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
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

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
    Symbol sym = ASTHelpers.getSymbol(item);
    if (!(sym instanceof MethodSymbol) || !sym.isStatic()) {
      return false;
    }

    boolean methodSame = methodName.equals("*")
        || sym.getSimpleName().toString().equals(methodName)
        || sym.toString().equals(methodName);
    boolean classSame = fullClass.equals("*")
        || sym.owner.getQualifiedName().toString().equals(fullClass);
    return methodSame && classSame;
  }
}
