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
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;
import javax.annotation.Nullable;

/** A variable that can match a sequence of expressions. */
@AutoValue
abstract class URepeated extends UExpression {
  public static URepeated create(CharSequence identifier, UExpression expression) {
    return new AutoValue_URepeated(identifier.toString(), expression);
  }

  abstract String identifier();

  abstract UExpression expression();

  @Override
  @Nullable
  protected Choice<Unifier> defaultAction(Tree node, @Nullable Unifier unifier) {
    return expression().unify(node, unifier);
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    throw new UnsupportedOperationException(
        "@CountConstraint variables should be inlined inside method invocations or newArray");
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return expression().accept(visitor, data);
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  /** Gets the binding of the underlying identifier in the unifier. */
  public JCExpression getUnderlyingBinding(Unifier unifier) {
    return (unifier == null) ? null : unifier.getBinding(new UFreeIdent.Key(identifier()));
  }

  public Key key() {
    return new Key(identifier());
  }

  /** A key for a variable with count constraints. It maps to a list of expressions in a binding. */
  public static final class Key extends Bindings.Key<List<JCExpression>> {
    public Key(String name) {
      super(name);
    }
  }
}
