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
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.WhileLoopTree;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;

/**
 * A {@link UTree} representation of a {@link WhileLoopTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UWhileLoop extends USimpleStatement implements WhileLoopTree {
  public static UWhileLoop create(UExpression condition, UStatement body) {
    return new AutoValue_UWhileLoop(condition, (USimpleStatement) body);
  }

  @Override
  public abstract UExpression getCondition();

  @Override
  public abstract USimpleStatement getStatement();

  @Override
  public JCWhileLoop inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .WhileLoop(getCondition().inline(inliner), getStatement().inline(inliner));
  }

  @Override
  public Choice<Unifier> visitWhileLoop(WhileLoopTree loop, Unifier unifier) {
    return getCondition()
        .unify(loop.getCondition(), unifier)
        .thenChoose(unifications(getStatement(), loop.getStatement()));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitWhileLoop(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.WHILE_LOOP;
  }
}
