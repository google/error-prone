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
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCTypeIntersection;

/**
 * {@code UTree} representation of an {@code IntersectionTypeTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UIntersectionType extends UExpression implements IntersectionTypeTree {
  @VisibleForTesting
  static UIntersectionType create(UExpression... bounds) {
    return create(ImmutableList.copyOf(bounds));
  }

  static UIntersectionType create(Iterable<? extends UExpression> bounds) {
    return new AutoValue_UIntersectionType(ImmutableList.copyOf(bounds));
  }

  @Override
  public abstract ImmutableList<UExpression> getBounds();

  @Override
  public JCTypeIntersection inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeIntersection(inliner.inlineList(getBounds()));
  }

  @Override
  public Kind getKind() {
    return Kind.INTERSECTION_TYPE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitIntersectionType(this, data);
  }

  @Override
  public Choice<Unifier> visitIntersectionType(IntersectionTypeTree node, Unifier unifier) {
    return unifyList(unifier, getBounds(), node.getBounds());
  }
}
