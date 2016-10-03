/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

/**
 * Matches a symbol with the given symbol as superclass.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
class IsSymbol implements Matcher<Tree> {
  private final Class<? extends Symbol> symbolClass;

  public IsSymbol(Class<? extends Symbol> symbolClass) {
    this.symbolClass = symbolClass;
  }

  @Override
  public boolean matches(Tree item, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(item);
    return symbolClass.isAssignableFrom(sym.getClass());
  }
}
