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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of a {@code TypeParameterTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UTypeParameter extends UTree<JCTypeParameter> implements TypeParameterTree {
  @VisibleForTesting
  static UTypeParameter create(CharSequence name, UExpression... bounds) {
    return create(name, ImmutableList.copyOf(bounds), ImmutableList.<UAnnotation>of());
  }

  static UTypeParameter create(
      CharSequence name,
      Iterable<? extends UExpression> bounds,
      Iterable<? extends UAnnotation> annotations) {
    return new AutoValue_UTypeParameter(
        StringName.of(name), ImmutableList.copyOf(bounds), ImmutableList.copyOf(annotations));
  }

  @Override
  public abstract StringName getName();

  @Override
  public abstract ImmutableList<UExpression> getBounds();

  @Override
  public abstract ImmutableList<UAnnotation> getAnnotations();

  @Override
  public Kind getKind() {
    return Kind.TYPE_PARAMETER;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitTypeParameter(this, data);
  }

  @Override
  public JCTypeParameter inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .TypeParameter(getName().inline(inliner), inliner.inlineList(getBounds()));
  }

  @Override
  @Nullable
  public Choice<Unifier> visitTypeParameter(TypeParameterTree node, @Nullable Unifier unifier) {
    return getName()
        .unify(node.getName(), unifier)
        .thenChoose(unifications(getBounds(), node.getBounds()))
        .thenChoose(unifications(getAnnotations(), node.getAnnotations()));
  }
}
