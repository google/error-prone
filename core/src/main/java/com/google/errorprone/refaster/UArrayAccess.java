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
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;

/**
 * {@link UTree} version of {@link ArrayAccessTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UArrayAccess extends UExpression implements ArrayAccessTree {
  public static UArrayAccess create(UExpression arrayExpr, UExpression indexExpr) {
    return new AutoValue_UArrayAccess(arrayExpr, indexExpr);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  public abstract UExpression getIndex();

  @Override
  public Choice<Unifier> visitArrayAccess(ArrayAccessTree arrayAccess, Unifier unifier) {
    return getExpression()
        .unify(arrayAccess.getExpression(), unifier)
        .thenChoose(unifications(getIndex(), arrayAccess.getIndex()));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitArrayAccess(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.ARRAY_ACCESS;
  }

  @Override
  public JCArrayAccess inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Indexed(getExpression().inline(inliner), getIndex().inline(inliner));
  }
}
