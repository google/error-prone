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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/**
 * {@code UExpression} allowing a match against any of a list of expressions.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UAnyOf extends UExpression {
  public static UAnyOf create(UExpression... expressions) {
    return create(ImmutableList.copyOf(expressions));
  }

  public static UAnyOf create(Iterable<? extends UExpression> expressions) {
    return new AutoValue_UAnyOf(ImmutableList.copyOf(expressions));
  }

  abstract ImmutableList<UExpression> expressions();

  @Override
  public UExpression negate() {
    ImmutableList.Builder<UExpression> negations = ImmutableList.builder();
    for (UExpression expression : expressions()) {
      negations.add(expression.negate());
    }
    return create(negations.build());
  }

  @Override
  protected Choice<Unifier> defaultAction(final Tree tree, final Unifier unifier) {
    return Choice.from(expressions())
        .thenChoose(
            (UExpression expression) ->
                expression.unify(ASTHelpers.stripParentheses(tree), unifier.fork()));
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    throw new UnsupportedOperationException("anyOf should not appear in an @AfterTemplate");
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return expressions().get(0).accept(visitor, data);
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }
}
