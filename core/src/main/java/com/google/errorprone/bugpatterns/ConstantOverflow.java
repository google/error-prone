/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Compile-time constant expression overflows", severity = ERROR)
public class ConstantOverflow extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    TreePath path = state.getPath().getParentPath();
    while (path != null && path.getLeaf() instanceof ExpressionTree) {
      if (path.getLeaf() instanceof BinaryTree) {
        // only match on the outermost nested binary expression
        return NO_MATCH;
      }
      path = path.getParentPath();
    }
    try {
      tree.accept(CONSTANT_VISITOR, null);
      return NO_MATCH;
    } catch (ArithmeticException e) {
      Description.Builder description = buildDescription(tree);
      Fix longFix = longFix(tree, state);
      if (longFix != null) {
        description.addFix(longFix);
      }
      return description.build();
    }
  }

  /**
   * If the left operand of an int binary expression is an int literal, suggest making it a long.
   */
  private static @Nullable Fix longFix(ExpressionTree expr, VisitorState state) {
    BinaryTree binExpr = null;
    while (expr instanceof BinaryTree) {
      binExpr = (BinaryTree) expr;
      expr = binExpr.getLeftOperand();
    }
    if (!(expr instanceof LiteralTree) || expr.getKind() != Kind.INT_LITERAL) {
      return null;
    }
    Type intType = state.getSymtab().intType;
    if (!isSameType(getType(binExpr), intType, state)) {
      return null;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder().postfixWith(expr, "L");
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof VariableTree variableTree
        && isSameType(getType(parent), intType, state)) {
      if (!hasImplicitType(variableTree, state)) {
        fix.replace(variableTree.getType(), "long");
      }
    }
    return fix.build();
  }

  /** A compile-time constant expression evaluator that checks for overflow. */
  private static final SimpleTreeVisitor<Number, Void> CONSTANT_VISITOR =
      new SimpleTreeVisitor<Number, Void>() {

        @Override
        public @Nullable Number visitConditionalExpression(ConditionalExpressionTree node, Void p) {
          Number ifTrue = node.getTrueExpression().accept(this, null);
          Number ifFalse = node.getFalseExpression().accept(this, null);
          Boolean condition = ASTHelpers.constValue(node.getCondition(), Boolean.class);
          if (condition == null) {
            return null;
          }
          return condition ? ifTrue : ifFalse;
        }

        @Override
        public Number visitParenthesized(ParenthesizedTree node, Void p) {
          return node.getExpression().accept(this, null);
        }

        @Override
        public @Nullable Number visitUnary(UnaryTree node, Void p) {
          Number value = node.getExpression().accept(this, null);
          if (value == null) {
            return null;
          }
          if (value instanceof Long) {
            return unop(node.getKind(), value.longValue());
          } else {
            return unop(node.getKind(), value.intValue());
          }
        }

        @Override
        public @Nullable Number visitBinary(BinaryTree node, Void p) {
          Number lhs = node.getLeftOperand().accept(this, null);
          Number rhs = node.getRightOperand().accept(this, null);
          if (lhs == null || rhs == null) {
            return null;
          }
          // assume that e.g. `Integer.MIN_VALUE - 1` is intentional
          switch (node.getKind()) {
            case MINUS -> {
              if ((lhs instanceof Long && lhs.longValue() == Long.MIN_VALUE)
                  || (lhs instanceof Integer && lhs.intValue() == Integer.MIN_VALUE)) {
                return null;
              }
            }
            case PLUS -> {
              if ((lhs instanceof Long && lhs.longValue() == Long.MAX_VALUE)
                  || (lhs instanceof Integer && lhs.intValue() == Integer.MAX_VALUE)) {
                return null;
              }
            }
            default -> {}
          }
          if (lhs instanceof Long || rhs instanceof Long) {
            return binop(node.getKind(), lhs.longValue(), rhs.longValue());
          } else {
            return binop(node.getKind(), lhs.intValue(), rhs.intValue());
          }
        }

        @Override
        public @Nullable Number visitTypeCast(TypeCastTree node, Void p) {
          Number value = node.getExpression().accept(this, null);
          if (value == null) {
            return null;
          }
          if (!(node.getType() instanceof PrimitiveTypeTree primitiveTypeTree)) {
            return null;
          }
          TypeKind kind = primitiveTypeTree.getPrimitiveTypeKind();
          return cast(kind, value);
        }

        @Override
        public Number visitMemberSelect(MemberSelectTree node, Void p) {
          return getIntegralConstant(node);
        }

        @Override
        public Number visitIdentifier(IdentifierTree node, Void p) {
          return getIntegralConstant(node);
        }

        @Override
        public Number visitLiteral(LiteralTree node, Void unused) {
          return getIntegralConstant(node);
        }
      };

  private static @Nullable Long unop(Kind kind, long value) {
    return switch (kind) {
      case UNARY_PLUS -> +value;
      case UNARY_MINUS -> -value;
      case BITWISE_COMPLEMENT -> ~value;
      default -> null;
    };
  }

  private static @Nullable Integer unop(Kind kind, int value) {
    return switch (kind) {
      case UNARY_PLUS -> +value;
      case UNARY_MINUS -> -value;
      case BITWISE_COMPLEMENT -> ~value;
      default -> null;
    };
  }

  static @Nullable Long binop(Kind kind, long lhs, long rhs) {
    return switch (kind) {
      case MULTIPLY -> LongMath.checkedMultiply(lhs, rhs);
      case DIVIDE -> lhs / rhs;
      case REMAINDER -> lhs % rhs;
      case PLUS -> LongMath.checkedAdd(lhs, rhs);
      case MINUS -> LongMath.checkedSubtract(lhs, rhs);
      case LEFT_SHIFT -> lhs << rhs;
      case RIGHT_SHIFT -> lhs >> rhs;
      case UNSIGNED_RIGHT_SHIFT -> lhs >>> rhs;
      case AND -> lhs & rhs;
      case XOR -> lhs ^ rhs;
      case OR -> lhs | rhs;
      default -> null;
    };
  }

  static @Nullable Integer binop(Kind kind, int lhs, int rhs) {
    return switch (kind) {
      case MULTIPLY -> IntMath.checkedMultiply(lhs, rhs);
      case DIVIDE -> lhs / rhs;
      case REMAINDER -> lhs % rhs;
      case PLUS -> IntMath.checkedAdd(lhs, rhs);
      case MINUS -> IntMath.checkedSubtract(lhs, rhs);
      case LEFT_SHIFT -> lhs << rhs;
      case RIGHT_SHIFT -> lhs >> rhs;
      case UNSIGNED_RIGHT_SHIFT -> lhs >>> rhs;
      case AND -> lhs & rhs;
      case XOR -> lhs ^ rhs;
      case OR -> lhs | rhs;
      default -> null;
    };
  }

  private static @Nullable Number cast(TypeKind kind, Number value) {
    return switch (kind) {
      case SHORT -> value.shortValue();
      case INT -> value.intValue();
      case LONG -> value.longValue();
      case BYTE -> value.byteValue();
      case CHAR -> (int) (char) value.intValue();
      default -> null;
    };
  }

  private static @Nullable Number getIntegralConstant(Tree node) {
    Number number = ASTHelpers.constValue(node, Number.class);
    if (number instanceof Integer || number instanceof Long) {
      return number;
    }
    return null;
  }
}
