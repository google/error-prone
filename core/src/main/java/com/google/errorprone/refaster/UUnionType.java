/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static com.google.errorprone.refaster.Unifier.unifyList;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.UnionTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeUnion;

/**
 * {@code UTree} representation of {@code UnionTypeTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UUnionType extends UExpression implements UnionTypeTree {
  @VisibleForTesting
  static UUnionType create(UExpression... typeAlternatives) {
    return create(ImmutableList.copyOf(typeAlternatives));
  }

  static UUnionType create(Iterable<? extends UExpression> typeAlternatives) {
    return new AutoValue_UUnionType(ImmutableList.copyOf(typeAlternatives));
  }

  @Override
  public abstract ImmutableList<UExpression> getTypeAlternatives();

  @Override
  public Kind getKind() {
    return Kind.UNION_TYPE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitUnionType(this, data);
  }

  @Override
  public JCTypeUnion inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeUnion(inliner.inlineList(getTypeAlternatives()));
  }

  @Override
  public Choice<Unifier> visitUnionType(UnionTypeTree node, Unifier unifier) {
    return unifyList(unifier, getTypeAlternatives(), node.getTypeAlternatives());
  }
}
