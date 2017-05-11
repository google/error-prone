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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;

/**
 * {@link UTree} version of {@link TypeCastTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UTypeCast extends UExpression implements TypeCastTree {
  public static UTypeCast create(UTree<?> type, UExpression expression) {
    return new AutoValue_UTypeCast(type, expression);
  }

  @Override
  public abstract UTree<?> getType();

  @Override
  public abstract UExpression getExpression();

  @Override
  public Choice<Unifier> visitTypeCast(TypeCastTree cast, Unifier unifier) {
    return getType()
        .unify(cast.getType(), unifier)
        .thenChoose(unifications(getExpression(), cast.getExpression()));
  }

  @Override
  public Kind getKind() {
    return Kind.TYPE_CAST;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitTypeCast(this, data);
  }

  @Override
  public JCTypeCast inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeCast(getType().inline(inliner), getExpression().inline(inliner));
  }
}
