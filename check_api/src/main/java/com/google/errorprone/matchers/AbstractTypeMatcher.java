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

import static com.google.errorprone.suppliers.Suppliers.typeFromString;

import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** Base class for type matchers. */
public abstract class AbstractTypeMatcher<T extends Tree> implements Matcher<T> {

  protected Supplier<Type> typeToCompareSupplier;

  public AbstractTypeMatcher(Supplier<Type> typeToCompareSupplier) {
    this.typeToCompareSupplier = typeToCompareSupplier;
  }

  public AbstractTypeMatcher(String typeString) {
    this(typeFromString(typeString));
  }

  @Override
  public abstract boolean matches(T tree, VisitorState state);
}
