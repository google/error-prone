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
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of a {@code MemberReferenceTree}
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UMemberReference extends UExpression implements MemberReferenceTree {
  public static UMemberReference create(
      ReferenceMode mode,
      UExpression qualifierExpression,
      CharSequence name,
      @Nullable Iterable<? extends UExpression> typeArguments) {
    return new AutoValue_UMemberReference(
        mode,
        qualifierExpression,
        StringName.of(name),
        (typeArguments == null) ? null : ImmutableList.copyOf(typeArguments));
  }

  @Override
  public Kind getKind() {
    return Kind.MEMBER_REFERENCE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitMemberReference(this, data);
  }

  @Override
  public Choice<Unifier> visitMemberReference(MemberReferenceTree node, Unifier unifier) {
    return Choice.condition(getMode() == node.getMode(), unifier)
        .thenChoose(unifications(getQualifierExpression(), node.getQualifierExpression()))
        .thenChoose(unifications(getName(), node.getName()))
        .thenChoose(unifications(getTypeArguments(), node.getTypeArguments()));
  }

  @Override
  public JCMemberReference inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Reference(
            getMode(),
            getName().inline(inliner),
            getQualifierExpression().inline(inliner),
            (getTypeArguments() == null) ? null : inliner.inlineList(getTypeArguments()));
  }

  @Override
  public abstract ReferenceMode getMode();

  @Override
  public abstract UExpression getQualifierExpression();

  @Override
  public abstract StringName getName();

  @Override
  @Nullable
  public abstract ImmutableList<UExpression> getTypeArguments();
}
