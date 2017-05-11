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
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/**
 * Matches an instance method that is a descendant of a method with the given class and name.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class DescendantOf implements Matcher<ExpressionTree> {
  private final String fullClassName;
  private final String methodName;

  public DescendantOf(String fullClassName, String methodName) {
    this.fullClassName = fullClassName;
    this.methodName = methodName;
  }

  @Override
  public boolean matches(ExpressionTree expressionTree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(expressionTree);
    if (sym == null) {
      return false;
    }
    if (!(sym instanceof MethodSymbol)) {
      throw new IllegalArgumentException(
          "DescendantOf matcher expects a method call but found "
              + sym.getClass()
              + ". Expression: "
              + expressionTree);
    }
    if (sym.isStatic()) {
      return false;
    }

    if (methodName.equals(sym.toString())) {
      Type accessedReferenceType = sym.owner.type;
      Type collectionType = state.getTypeFromString(fullClassName);
      if (collectionType != null) {
        return state
            .getTypes()
            .isSubtype(accessedReferenceType, state.getTypes().erasure(collectionType));
      }
    }

    return false;
  }
}
