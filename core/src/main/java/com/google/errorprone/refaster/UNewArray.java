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
import static com.google.errorprone.refaster.Unifier.unifyNullable;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import java.util.List;
import javax.annotation.Nullable;

/** {@link UTree} version of {@link NewArrayTree}, which represents an array instantiation. */
@AutoValue
abstract class UNewArray extends UExpression implements NewArrayTree {

  public static UNewArray create(
      UExpression type,
      List<? extends UExpression> dimensions,
      List<? extends UExpression> initializers) {
    return new AutoValue_UNewArray(
        type,
        dimensions != null ? ImmutableList.copyOf(dimensions) : null,
        initializers != null ? ImmutableList.copyOf(initializers) : null);
  }

  @Nullable
  @Override
  public abstract UExpression getType();

  @Nullable
  @Override
  public abstract List<UExpression> getDimensions();

  @Nullable
  @Override
  public abstract List<UExpression> getInitializers();

  @Override
  @Nullable
  public Choice<Unifier> visitNewArray(NewArrayTree newArray, @Nullable Unifier unifier) {
    boolean hasRepeated =
        getInitializers() != null
            && Iterables.any(getInitializers(), Predicates.instanceOf(URepeated.class));
    return unifyNullable(unifier, getType(), newArray.getType())
        .thenChoose(unifications(getDimensions(), newArray.getDimensions()))
        .thenChoose(unifications(getInitializers(), newArray.getInitializers(), hasRepeated));
  }

  @Override
  public Kind getKind() {
    return Kind.NEW_ARRAY;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitNewArray(this, data);
  }

  @Override
  public JCNewArray inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .NewArray(
            (getType() == null) ? null : getType().inline(inliner),
            (getDimensions() == null) ? null : inliner.<JCExpression>inlineList(getDimensions()),
            (getInitializers() == null)
                ? null
                : inliner.<JCExpression>inlineList(getInitializers()));
  }

  @Override
  public List<? extends AnnotationTree> getAnnotations() {
    return ImmutableList.of();
  }

  @Override
  public List<? extends List<? extends AnnotationTree>> getDimAnnotations() {
    return ImmutableList.of();
  }
}
