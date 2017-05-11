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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableBiMap;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;

/**
 * {@link UTree} representation of a {@link CompoundAssignmentTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UAssignOp extends UExpression implements CompoundAssignmentTree {
  private static final ImmutableBiMap<Kind, JCTree.Tag> TAG =
      ImmutableBiMap.<Kind, JCTree.Tag>builder()
          .put(Kind.PLUS_ASSIGNMENT, JCTree.Tag.PLUS_ASG)
          .put(Kind.MINUS_ASSIGNMENT, JCTree.Tag.MINUS_ASG)
          .put(Kind.MULTIPLY_ASSIGNMENT, JCTree.Tag.MUL_ASG)
          .put(Kind.DIVIDE_ASSIGNMENT, JCTree.Tag.DIV_ASG)
          .put(Kind.REMAINDER_ASSIGNMENT, JCTree.Tag.MOD_ASG)
          .put(Kind.LEFT_SHIFT_ASSIGNMENT, JCTree.Tag.SL_ASG)
          .put(Kind.RIGHT_SHIFT_ASSIGNMENT, JCTree.Tag.SR_ASG)
          .put(Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, JCTree.Tag.USR_ASG)
          .put(Kind.OR_ASSIGNMENT, JCTree.Tag.BITOR_ASG)
          .put(Kind.AND_ASSIGNMENT, JCTree.Tag.BITAND_ASG)
          .put(Kind.XOR_ASSIGNMENT, JCTree.Tag.BITXOR_ASG)
          .build();

  public static UAssignOp create(UExpression variable, Kind operator, UExpression expression) {
    checkArgument(
        TAG.containsKey(operator),
        "Tree kind %s does not represent a compound assignment operator",
        operator);
    return new AutoValue_UAssignOp(variable, operator, expression);
  }

  @Override
  public abstract UExpression getVariable();

  @Override
  public abstract Kind getKind();

  @Override
  public abstract UExpression getExpression();

  @Override
  public JCAssignOp inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Assignop(
            TAG.get(getKind()), getVariable().inline(inliner), getExpression().inline(inliner));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitCompoundAssignment(this, data);
  }

  // TODO(lowasser): consider matching x = x ? y as well as x ?= y

  @Override
  public Choice<Unifier> visitCompoundAssignment(CompoundAssignmentTree assignOp, Unifier unifier) {
    return Choice.condition(getKind() == assignOp.getKind(), unifier)
        .thenChoose(unifications(getVariable(), assignOp.getVariable()))
        .thenChoose(unifications(getExpression(), assignOp.getExpression()));
  }
}
