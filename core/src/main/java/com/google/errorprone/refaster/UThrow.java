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
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of a {@link ThrowTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UThrow extends USimpleStatement implements ThrowTree {
  public static UThrow create(UExpression expression) {
    return new AutoValue_UThrow(expression);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitThrow(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.THROW;
  }

  @Override
  @Nullable
  public Choice<Unifier> visitThrow(ThrowTree throwStmt, @Nullable Unifier unifier) {
    return getExpression().unify(throwStmt.getExpression(), unifier);
  }

  @Override
  public JCThrow inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Throw(getExpression().inline(inliner));
  }
}
