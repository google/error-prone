/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import java.util.Objects;

/**
 * Identifier corresponding to a template local variable.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class ULocalVarIdent extends UIdent {
  /** A key in a {@code Bindings} associated with a local variable of the specified name. */
  static final class Key extends Bindings.Key<LocalVarBinding> {
    Key(CharSequence name) {
      super(name.toString());
    }
  }

  public static ULocalVarIdent create(CharSequence identifier) {
    return new AutoValue_ULocalVarIdent(StringName.of(identifier));
  }

  @Override
  public abstract StringName getName();

  private Key key() {
    return new Key(getName());
  }

  @Override
  public Choice<Unifier> visitIdentifier(IdentifierTree ident, Unifier unifier) {
    LocalVarBinding binding = unifier.getBinding(key());
    return Choice.condition(
        binding != null && Objects.equals(ASTHelpers.getSymbol(ident), binding.getSymbol()),
        unifier);
  }

  @Override
  public JCIdent inline(Inliner inliner) throws CouldNotResolveImportException {
    Optional<LocalVarBinding> binding = inliner.getOptionalBinding(key());
    return inliner
        .maker()
        .Ident(binding.isPresent() ? binding.get().getName() : getName().inline(inliner));
  }
}
