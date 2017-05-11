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
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCAssign;

/**
 * {@link UTree} representation of a {@link AssignmentTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UAssign extends UExpression implements AssignmentTree {
  public static UAssign create(UExpression variable, UExpression expression) {
    return new AutoValue_UAssign(variable, expression);
  }

  @Override
  public abstract UExpression getVariable();

  @Override
  public abstract UExpression getExpression();

  @Override
  public JCAssign inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Assign(getVariable().inline(inliner), getExpression().inline(inliner));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitAssignment(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.ASSIGNMENT;
  }

  @Override
  public Choice<Unifier> visitAssignment(AssignmentTree assign, Unifier unifier) {
    return getVariable()
        .unify(assign.getVariable(), unifier)
        .thenChoose(unifications(getExpression(), assign.getExpression()));
  }
}
