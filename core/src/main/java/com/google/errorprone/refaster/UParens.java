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
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCParens;

/**
 * {@link UTree} version of {@link ParenthesizedTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UParens extends UExpression implements ParenthesizedTree {
  public static UParens create(UExpression expression) {
    return new AutoValue_UParens(expression);
  }

  @Override
  public abstract UExpression getExpression();

  private static final TreeVisitor<Tree, Void> SKIP_PARENS = new SimpleTreeVisitor<Tree, Void>() {
    @Override
    protected Tree defaultAction(Tree node, Void v) {
      return node;
    }

    @Override
    public Tree visitParenthesized(ParenthesizedTree node, Void v) {
      return node.getExpression().accept(this, null);
    }
  };
  
  static Tree skipParens(Tree tree) {
    return tree.accept(SKIP_PARENS, null);
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree tree, Unifier unifier) {
    return getExpression().unify(skipParens(tree), unifier);
  }

  @Override
  public Kind getKind() {
    return Kind.PARENTHESIZED;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitParenthesized(this, data);
  }

  @Override
  public JCParens inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Parens(getExpression().inline(inliner));
  }

  @Override
  public UExpression negate() {
    return create(getExpression().negate());
  }
}
