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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.Maps;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of a {@code WildcardTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UWildcard extends UExpression implements WildcardTree {
  private static final BiMap<Kind, BoundKind> BOUND_KINDS;

  static {
    EnumBiMap<Kind, BoundKind> validKinds = EnumBiMap.create(Kind.class, BoundKind.class);
    validKinds.put(Kind.UNBOUNDED_WILDCARD, BoundKind.UNBOUND);
    validKinds.put(Kind.EXTENDS_WILDCARD, BoundKind.EXTENDS);
    validKinds.put(Kind.SUPER_WILDCARD, BoundKind.SUPER);
    BOUND_KINDS = Maps.unmodifiableBiMap(validKinds);
  }

  static UWildcard create(Kind kind, @Nullable UTree<?> bound) {
    checkArgument(BOUND_KINDS.containsKey(kind));
    // verify bound is null iff kind is UNBOUNDED_WILDCARD
    checkArgument((bound == null) == (kind == Kind.UNBOUNDED_WILDCARD));
    return new AutoValue_UWildcard(kind, bound);
  }

  @Override
  public abstract Kind getKind();

  @Override
  @Nullable
  public abstract UTree<?> getBound();

  @Override
  public JCWildcard inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Wildcard(
            inliner.maker().TypeBoundKind(BOUND_KINDS.get(getKind())),
            (getBound() == null) ? null : getBound().inline(inliner));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitWildcard(this, data);
  }

  @Override
  public Choice<Unifier> visitWildcard(WildcardTree node, Unifier unifier) {
    return Choice.condition(getKind() == node.getKind(), unifier)
        .thenChoose(unifications(getBound(), node.getBound()));
  }
}
