package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

import java.util.HashSet;
import java.util.Set;

/**
 * Matcher for a binary operator expression. Does not match compound-assignment binary operators
 * (e.g. +=).
 *
 * @see CompoundAssignment
 */
public class BinaryOperator implements Matcher<BinaryTree> {

  private static final Set<Kind> BINARY_OPERATORS = new HashSet<Kind>(19);
  static {
    BINARY_OPERATORS.add(Kind.AND);
    BINARY_OPERATORS.add(Kind.CONDITIONAL_AND);
    BINARY_OPERATORS.add(Kind.CONDITIONAL_OR);
    BINARY_OPERATORS.add(Kind.DIVIDE);
    BINARY_OPERATORS.add(Kind.EQUAL_TO);
    BINARY_OPERATORS.add(Kind.GREATER_THAN);
    BINARY_OPERATORS.add(Kind.GREATER_THAN_EQUAL);
    BINARY_OPERATORS.add(Kind.LEFT_SHIFT);
    BINARY_OPERATORS.add(Kind.LESS_THAN);
    BINARY_OPERATORS.add(Kind.LESS_THAN_EQUAL);
    BINARY_OPERATORS.add(Kind.MINUS);
    BINARY_OPERATORS.add(Kind.MULTIPLY);
    BINARY_OPERATORS.add(Kind.NOT_EQUAL_TO);
    BINARY_OPERATORS.add(Kind.OR);
    BINARY_OPERATORS.add(Kind.PLUS);
    BINARY_OPERATORS.add(Kind.REMAINDER);
    BINARY_OPERATORS.add(Kind.RIGHT_SHIFT);
    BINARY_OPERATORS.add(Kind.UNSIGNED_RIGHT_SHIFT);
    BINARY_OPERATORS.add(Kind.XOR);
  }

  private final Set<Kind> operators;
  private final Matcher<ExpressionTree> leftOperandMatcher;
  private final Matcher<ExpressionTree> rightOperandMatcher;

  /**
   * Creates a new binary operator matcher, which matches a binary expression with one of a set of
   * operators and whose operands match the provided order-specific matchers. Does not match
   * compound-assignment operators (e.g. +=).
   *
   * @param operators The set of matching binary operators. These are drawn from the
   *        {@link Kind} enum values which link to {@link BinaryTree} in their javadoc.
   * @param leftOperandMatcher The matcher which must match the left operand to the binary operator
   *        expression.
   * @param rightOperandMatcher The matcher which must match the right operand to the binary
   *        operator expression.
   */
  public BinaryOperator(
      Set<Kind> operators,
      Matcher<ExpressionTree> leftOperandMatcher,
      Matcher<ExpressionTree> rightOperandMatcher) {
    this.operators = validateOperators(operators);
    if (leftOperandMatcher == null) {
      throw new NullPointerException("Left operand to BinaryOperator matcher is null");
    }
    if (rightOperandMatcher == null) {
      throw new NullPointerException("Right operand to BinaryOperator matcher is null");
    }
    this.leftOperandMatcher = leftOperandMatcher;
    this.rightOperandMatcher = rightOperandMatcher;
  }

  @Override public boolean matches(BinaryTree binaryTree, VisitorState state) {
    if (!operators.contains(binaryTree.getKind())) {
      return false;
    }
    return leftOperandMatcher.matches(binaryTree.getLeftOperand(), state)
        && rightOperandMatcher.matches(binaryTree.getRightOperand(), state);
  }

  /**
   * Returns the provided set of operators if they are all binary operators. Otherwise, throws an
   * IllegalArgumentException.
   */
  private static Set<Kind> validateOperators(Set<Kind> kinds) {
    for (Kind kind : kinds) {
      if (!BINARY_OPERATORS.contains(kind)) {
        throw new IllegalArgumentException(kind.name() + " is not a binary operator.");
      }
    }
    return kinds;
  }
}
