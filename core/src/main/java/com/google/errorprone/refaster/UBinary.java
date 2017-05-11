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
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;

/**
 * {@link UTree} version of {@link BinaryTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UBinary extends UExpression implements BinaryTree {
  static final ImmutableBiMap<Kind, JCTree.Tag> OP_CODES =
      new ImmutableBiMap.Builder<Kind, JCTree.Tag>()
          .put(Kind.PLUS, JCTree.Tag.PLUS)
          .put(Kind.MINUS, JCTree.Tag.MINUS)
          .put(Kind.MULTIPLY, JCTree.Tag.MUL)
          .put(Kind.DIVIDE, JCTree.Tag.DIV)
          .put(Kind.REMAINDER, JCTree.Tag.MOD)
          .put(Kind.LEFT_SHIFT, JCTree.Tag.SL)
          .put(Kind.RIGHT_SHIFT, JCTree.Tag.SR)
          .put(Kind.UNSIGNED_RIGHT_SHIFT, JCTree.Tag.USR)
          .put(Kind.OR, JCTree.Tag.BITOR)
          .put(Kind.AND, JCTree.Tag.BITAND)
          .put(Kind.XOR, JCTree.Tag.BITXOR)
          .put(Kind.CONDITIONAL_AND, JCTree.Tag.AND)
          .put(Kind.CONDITIONAL_OR, JCTree.Tag.OR)
          .put(Kind.LESS_THAN, JCTree.Tag.LT)
          .put(Kind.LESS_THAN_EQUAL, JCTree.Tag.LE)
          .put(Kind.GREATER_THAN, JCTree.Tag.GT)
          .put(Kind.GREATER_THAN_EQUAL, JCTree.Tag.GE)
          .put(Kind.EQUAL_TO, JCTree.Tag.EQ)
          .put(Kind.NOT_EQUAL_TO, JCTree.Tag.NE)
          .build();

  static final ImmutableBiMap<Kind, Kind> NEGATION =
      new ImmutableBiMap.Builder<Kind, Kind>()
          .put(Kind.LESS_THAN, Kind.GREATER_THAN_EQUAL)
          .put(Kind.LESS_THAN_EQUAL, Kind.GREATER_THAN)
          .put(Kind.GREATER_THAN, Kind.LESS_THAN_EQUAL)
          .put(Kind.GREATER_THAN_EQUAL, Kind.LESS_THAN)
          .put(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO)
          .put(Kind.NOT_EQUAL_TO, Kind.EQUAL_TO)
          .build();

  static final ImmutableBiMap<Kind, Kind> DEMORGAN =
      new ImmutableBiMap.Builder<Kind, Kind>()
          .put(Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR)
          .put(Kind.CONDITIONAL_OR, Kind.CONDITIONAL_AND)
          .put(Kind.AND, Kind.OR)
          .put(Kind.OR, Kind.AND)
          .build();

  public static UBinary create(Kind binaryOp, UExpression lhs, UExpression rhs) {
    checkArgument(
        OP_CODES.containsKey(binaryOp), "%s is not a supported binary operation", binaryOp);
    return new AutoValue_UBinary(binaryOp, lhs, rhs);
  }

  @Override
  public abstract Kind getKind();

  @Override
  public abstract UExpression getLeftOperand();

  @Override
  public abstract UExpression getRightOperand();

  @Override
  public Choice<Unifier> visitBinary(BinaryTree binary, Unifier unifier) {
    return Choice.condition(getKind().equals(binary.getKind()), unifier)
        .thenChoose(unifications(getLeftOperand(), binary.getLeftOperand()))
        .thenChoose(unifications(getRightOperand(), binary.getRightOperand()));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitBinary(this, data);
  }

  @Override
  public JCBinary inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Binary(
            OP_CODES.get(getKind()),
            getLeftOperand().inline(inliner),
            getRightOperand().inline(inliner));
  }

  @Override
  public UExpression negate() {
    if (NEGATION.containsKey(getKind())) {
      return UBinary.create(NEGATION.get(getKind()), getLeftOperand(), getRightOperand());
    } else if (DEMORGAN.containsKey(getKind())) {
      return UBinary.create(
          DEMORGAN.get(getKind()), getLeftOperand().negate(), getRightOperand().negate());
    } else {
      return super.negate();
    }
  }
}
