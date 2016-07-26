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

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of a {@link ExpressionStatementTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UExpressionStatement extends USimpleStatement implements ExpressionStatementTree {
  public static UExpressionStatement create(UExpression expression) {
    return new AutoValue_UExpressionStatement(expression);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  @Nullable
  public Choice<Unifier> visitExpressionStatement(
      ExpressionStatementTree expressionStatement, @Nullable Unifier unifier) {
    return getExpression().unify(expressionStatement.getExpression(), unifier);
  }

  @Override
  public JCExpressionStatement inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Exec(getExpression().inline(inliner));
  }

  @Override
  public Kind getKind() {
    return Kind.EXPRESSION_STATEMENT;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitExpressionStatement(this, data);
  }
}
