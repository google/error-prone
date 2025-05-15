/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.matchers.method;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/**
 * The state that is propagated across a match operation for methods.
 *
 * @param sym The method being matched.
 * @param tree The type of the class in which a member method or constructor is declared.
 */
record MethodMatchState(ExpressionTree tree, MethodSymbol sym) implements MatchState {
  @Override
  public Type ownerType() {
    // TODO: b/130658266 - should this be the symbol's owner type, not the receiver's owner type?
    return ASTHelpers.getReceiverType(tree());
  }

  static MatchState create(ExpressionTree tree, MethodSymbol methodSymbol) {
    return new MethodMatchState(tree, methodSymbol);
  }
}
