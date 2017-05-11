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
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link ArrayTypeTree}. This is the AST representation of {@link
 * UArrayType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UArrayTypeTree extends UExpression implements ArrayTypeTree {
  public static UArrayTypeTree create(UExpression elementType) {
    return new AutoValue_UArrayTypeTree(elementType);
  }

  @Override
  public abstract UExpression getType();

  @Override
  @Nullable
  public Choice<Unifier> visitArrayType(ArrayTypeTree tree, @Nullable Unifier unifier) {
    return getType().unify(tree.getType(), unifier);
  }

  @Override
  public Kind getKind() {
    return Kind.ARRAY_TYPE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitArrayType(this, data);
  }

  @Override
  public JCArrayTypeTree inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeArray(getType().inline(inliner));
  }
}
