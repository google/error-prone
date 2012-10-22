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
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

/**
 * Matches an instance method expression.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class InstanceMethod implements Matcher<ExpressionTree> {

  private final Matcher<ExpressionTree> receiverMatcher;
  private final String methodName;

  public InstanceMethod(Matcher<ExpressionTree> receiverMatcher, String methodName) {
    this.receiverMatcher = receiverMatcher;
    this.methodName = methodName;
  }

  @Override
  public boolean matches(ExpressionTree item, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(item);
    if (sym == null || sym.isStatic() ||
        !sym.getQualifiedName().equals(state.getName(methodName))) {
      return false;
    }

    if (item instanceof JCFieldAccess) {
      JCFieldAccess fieldAccess = (JCFieldAccess) item;
      return receiverMatcher.matches(fieldAccess.getExpression(), state);
    } else if (item instanceof JCIdent) {
      // There's no explicit receiver in this case, so try the receiverMatcher against null. If it
      // throws a NullPointerException (i.e., it cares about the input), then return false.
      try {
        return receiverMatcher.matches(null, state);
      } catch (NullPointerException e) {
        return false;
      }
    } else {
      throw new IllegalStateException("Unexpected type in InstanceMethod matcher: "
          + item.getClass().getName());
    }
  }
}
