/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.predicates.type;

import com.google.errorprone.VisitorState;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Type;

/** Matches types that are a sub-type of one of the given types. */
public class DescendantOfAny implements TypePredicate {

  public final Iterable<Supplier<Type>> types;

  public DescendantOfAny(Iterable<Supplier<Type>> types) {
    this.types = types;
  }

  @Override
  public boolean apply(Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    for (Supplier<Type> supplier : types) {
      Type expected = supplier.get(state);
      if (expected == null) {
        continue;
      }
      if (ASTHelpers.isSubtype(type, expected, state)) {
        return true;
      }
    }
    return false;
  }
}
