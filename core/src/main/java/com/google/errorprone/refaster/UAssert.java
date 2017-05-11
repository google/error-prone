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
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of an assertion.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UAssert extends USimpleStatement implements AssertTree {

  static UAssert create(UExpression condition, @Nullable UExpression detail) {
    return new AutoValue_UAssert(condition, detail);
  }

  @Override
  public Kind getKind() {
    return Kind.ASSERT;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitAssert(this, data);
  }

  @Override
  public abstract UExpression getCondition();

  @Override
  @Nullable
  public abstract UExpression getDetail();

  @Override
  public Choice<Unifier> visitAssert(AssertTree node, Unifier unifier) {
    return getCondition()
        .unify(node.getCondition(), unifier)
        .thenChoose(unifications(getDetail(), node.getDetail()));
  }

  @Override
  public JCStatement inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Assert(
            getCondition().inline(inliner),
            (getDetail() == null) ? null : getDetail().inline(inliner));
  }
}
