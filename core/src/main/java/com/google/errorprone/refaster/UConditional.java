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
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link ConditionalExpressionTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UConditional extends UExpression implements ConditionalExpressionTree {
  public static UConditional create(
      UExpression conditionExpr, UExpression trueExpr, UExpression falseExpr) {
    return new AutoValue_UConditional(conditionExpr, trueExpr, falseExpr);
  }

  @Override
  public abstract UExpression getCondition();

  @Override
  public abstract UExpression getTrueExpression();

  @Override
  public abstract UExpression getFalseExpression();

  @Override
  @Nullable
  public Choice<Unifier> visitConditionalExpression(
      ConditionalExpressionTree conditional, Unifier unifier) {
    return getCondition()
        .unify(conditional.getCondition(), unifier.fork())
        .thenChoose(unifications(getTrueExpression(), conditional.getTrueExpression()))
        .thenChoose(unifications(getFalseExpression(), conditional.getFalseExpression()))
        .or(
            getCondition()
                .negate()
                .unify(conditional.getCondition(), unifier.fork())
                .thenChoose(unifications(getFalseExpression(), conditional.getTrueExpression()))
                .thenChoose(unifications(getTrueExpression(), conditional.getFalseExpression())));
  }

  @Override
  public Kind getKind() {
    return Kind.CONDITIONAL_EXPRESSION;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitConditionalExpression(this, data);
  }

  @Override
  public JCConditional inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Conditional(
            getCondition().inline(inliner),
            getTrueExpression().inline(inliner),
            getFalseExpression().inline(inliner));
  }
}
