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

import static com.google.errorprone.refaster.Unifier.unifyNullable;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of a {@link ReturnTree}.
 *
 * @author lowasser@google.com
 */
@AutoValue
public abstract class UReturn extends USimpleStatement implements ReturnTree {
  public static UReturn create(UExpression expression) {
    return new AutoValue_UReturn(expression);
  }

  @Override
  @Nullable
  public abstract UExpression getExpression();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitReturn(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.RETURN;
  }

  @Override
  public JCReturn inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Return(getExpression().inline(inliner));
  }

  @Override
  @Nullable
  public Choice<Unifier> visitReturn(ReturnTree ret, @Nullable Unifier unifier) {
    return unifyNullable(unifier, getExpression(), ret.getExpression());
  }
}
