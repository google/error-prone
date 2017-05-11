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
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of a {@link InstanceOfTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UInstanceOf extends UExpression implements InstanceOfTree {
  public static UInstanceOf create(UExpression expression, UTree<?> type) {
    return new AutoValue_UInstanceOf(expression, type);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  public abstract UTree<?> getType();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitInstanceOf(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.INSTANCE_OF;
  }

  @Override
  public JCInstanceOf inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeTest(getExpression().inline(inliner), getType().inline(inliner));
  }

  @Override
  @Nullable
  public Choice<Unifier> visitInstanceOf(InstanceOfTree instanceOf, @Nullable Unifier unifier) {
    return getExpression()
        .unify(instanceOf.getExpression(), unifier)
        .thenChoose(unifications(getType(), instanceOf.getType()));
  }
}
