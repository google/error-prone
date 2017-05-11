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
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * {@code UExpression} imposing a restriction on the kind of the matched expression.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UOfKind extends UExpression {
  public static UOfKind create(UExpression expression, Set<Kind> allowed) {
    return new AutoValue_UOfKind(expression, allowed);
  }

  abstract UExpression expression();

  abstract Set<Kind> allowed();

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    return expression().inline(inliner);
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return expression().accept(visitor, data);
  }

  @Override
  public Kind getKind() {
    return expression().getKind();
  }

  @Override
  @Nullable
  protected Choice<Unifier> defaultAction(Tree tree, @Nullable Unifier unifier) {
    return Choice.condition(allowed().contains(tree.getKind()), unifier)
        .thenChoose(unifications(expression(), tree));
  }
}
