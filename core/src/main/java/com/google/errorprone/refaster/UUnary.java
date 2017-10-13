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
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.UnaryTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link UnaryTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UUnary extends UExpression implements UnaryTree {
  private static final ImmutableBiMap<Kind, JCTree.Tag> UNARY_OP_CODES =
      new ImmutableBiMap.Builder<Kind, JCTree.Tag>()
          .put(Kind.PREFIX_INCREMENT, JCTree.Tag.PREINC)
          .put(Kind.PREFIX_DECREMENT, JCTree.Tag.PREDEC)
          .put(Kind.POSTFIX_INCREMENT, JCTree.Tag.POSTINC)
          .put(Kind.POSTFIX_DECREMENT, JCTree.Tag.POSTDEC)
          .put(Kind.UNARY_PLUS, JCTree.Tag.POS)
          .put(Kind.UNARY_MINUS, JCTree.Tag.NEG)
          .put(Kind.BITWISE_COMPLEMENT, JCTree.Tag.COMPL)
          .put(Kind.LOGICAL_COMPLEMENT, JCTree.Tag.NOT)
          .build();

  public static UUnary create(Kind unaryOp, UExpression expression) {
    checkArgument(
        UNARY_OP_CODES.containsKey(unaryOp), "%s is not a recognized unary operation", unaryOp);
    return new AutoValue_UUnary(unaryOp, expression);
  }

  @Override
  public abstract Kind getKind();

  @Override
  public abstract UExpression getExpression();

  @Override
  @Nullable
  public Choice<Unifier> visitUnary(UnaryTree unary, @Nullable Unifier unifier) {
    return Choice.condition(getKind().equals(unary.getKind()), unifier)
        .thenChoose(
            unifications(getExpression(), ASTHelpers.stripParentheses(unary.getExpression())));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitUnary(this, data);
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    JCExpression expr = getExpression().inline(inliner);
    final TreeMaker maker = inliner.maker();
    if (getKind() == Kind.LOGICAL_COMPLEMENT) {
      return new TreeCopier<Void>(maker) {
        @SuppressWarnings("unchecked")
        // essentially depends on T being a superclass of JCExpression
        @Override
        public <T extends JCTree> T copy(T t, Void v) {
          if (t instanceof BinaryTree
              || t instanceof UnaryTree
              || t instanceof ConditionalExpressionTree) {
            return super.copy(t, v);
          } else {
            return (T) defaultNegation(t);
          }
        }

        public JCExpression defaultNegation(Tree expr) {
          return maker.Unary(JCTree.Tag.NOT, (JCExpression) expr);
        }

        @Override
        public JCExpression visitBinary(BinaryTree tree, Void v) {
          if (UBinary.DEMORGAN.containsKey(tree.getKind())) {
            JCExpression negLeft = copy((JCExpression) tree.getLeftOperand());
            JCExpression negRight = copy((JCExpression) tree.getRightOperand());
            return maker.Binary(
                UBinary.OP_CODES.get(UBinary.DEMORGAN.get(tree.getKind())), negLeft, negRight);
          } else if (UBinary.NEGATION.containsKey(tree.getKind())) {
            JCExpression left = (JCExpression) tree.getLeftOperand();
            JCExpression right = (JCExpression) tree.getRightOperand();
            return maker.Binary(
                UBinary.OP_CODES.get(UBinary.NEGATION.get(tree.getKind())), left, right);
          } else {
            return defaultNegation(tree);
          }
        }

        @Override
        public JCExpression visitUnary(UnaryTree tree, Void v) {
          if (tree.getKind() == Kind.LOGICAL_COMPLEMENT) {
            return (JCExpression) tree.getExpression();
          } else {
            return defaultNegation(tree);
          }
        }

        @Override
        public JCConditional visitConditionalExpression(ConditionalExpressionTree tree, Void v) {
          return maker.Conditional(
              (JCExpression) tree.getCondition(),
              copy((JCExpression) tree.getTrueExpression()),
              copy((JCExpression) tree.getFalseExpression()));
        }
      }.copy(expr);
    } else {
      return inliner.maker().Unary(UNARY_OP_CODES.get(getKind()), getExpression().inline(inliner));
    }
  }

  @Override
  public UExpression negate() {
    return (getKind() == Kind.LOGICAL_COMPLEMENT) ? getExpression() : super.negate();
  }
}
