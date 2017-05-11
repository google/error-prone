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
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import javax.annotation.Nullable;

/**
 * A {@link UTree} representation of a {@link DoWhileLoopTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UDoWhileLoop extends USimpleStatement implements DoWhileLoopTree {
  public static UDoWhileLoop create(UStatement body, UExpression condition) {
    return new AutoValue_UDoWhileLoop((USimpleStatement) body, condition);
  }

  @Override
  public abstract USimpleStatement getStatement();

  @Override
  public abstract UExpression getCondition();

  @Override
  @Nullable
  public Choice<Unifier> visitDoWhileLoop(DoWhileLoopTree loop, @Nullable Unifier unifier) {
    return getStatement()
        .unify(loop.getStatement(), unifier)
        .thenChoose(unifications(getCondition(), loop.getCondition()));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitDoWhileLoop(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.DO_WHILE_LOOP;
  }

  @Override
  public JCDoWhileLoop inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().DoLoop(getStatement().inline(inliner), getCondition().inline(inliner));
  }
}
