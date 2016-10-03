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

import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class IsSubtypeOf<T extends Tree> extends AbstractTypeMatcher<T> {

  public IsSubtypeOf(Supplier<Type> typeToCompareSupplier) {
    super(typeToCompareSupplier);
  }

  public IsSubtypeOf(String typeString) {
    super(typeString);
  }

  @Override
  public boolean matches(T tree, VisitorState state) {
    return isSubtype(getType(tree), typeToCompareSupplier.get(state), state);
  }
}
