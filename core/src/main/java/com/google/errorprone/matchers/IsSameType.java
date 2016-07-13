/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/**
 * Matches an AST node if its erased type is the same as the given type, e.g. If the type of the AST
 * node is {@code HashMap<K,V>} and the given type is {@code HashMap}, then their erased type is the
 * same.
 *
 * @author yanx@google.com (Yan Xie)
 */
public class IsSameType<T extends Tree> extends AbstractTypeMatcher<T> {

  public IsSameType(Supplier<Type> typeToCompareSupplier) {
    super(typeToCompareSupplier);
  }

  public IsSameType(String typeString) {
    super(typeString);
  }

  @Override
  public boolean matches(T tree, VisitorState state) {
    Type typeToCompare = typeToCompareSupplier.get(state);
    return ASTHelpers.isSameType(ASTHelpers.getType(tree), typeToCompare, state);
  }
}
