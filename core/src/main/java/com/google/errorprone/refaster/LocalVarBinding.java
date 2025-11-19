/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Name;

/**
 * Binding for a local variable in a template.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public record LocalVarBinding(VarSymbol symbol, ModifiersTree modifiers) {
  public static LocalVarBinding create(VarSymbol symbol, ModifiersTree modifiers) {
    return new LocalVarBinding(symbol, modifiers);
  }

  public Name getName() {
    return this.symbol().getSimpleName();
  }

  @Override
  public final String toString() {
    return this.symbol().getSimpleName().toString();
  }
}
