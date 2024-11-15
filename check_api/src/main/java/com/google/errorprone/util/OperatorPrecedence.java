/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.util;

import com.sun.source.tree.Tree;

/**
 * The precedence for an operator kind in the {@link com.sun.source.tree} API.
 *
 * <p>As documented at: http://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
 */
public enum OperatorPrecedence {
  POSTFIX(13),
  UNARY(12),
  MULTIPLICATIVE(11),
  ADDITIVE(10),
  SHIFT(9),
  RELATIONAL(8),
  EQUALITY(7),
  AND(6),
  XOR(5),
  OR(4),
  CONDITIONAL_AND(3),
  CONDITIONAL_OR(2),
  TERNARY(1),
  ASSIGNMENT(0);

  private final int precedence;

  OperatorPrecedence(int precedence) {
    this.precedence = precedence;
  }

  public boolean isHigher(OperatorPrecedence other) {
    return precedence > other.precedence;
  }

  public static OperatorPrecedence from(Tree.Kind kind) {
    return switch (kind) {
      case POSTFIX_DECREMENT, POSTFIX_INCREMENT -> OperatorPrecedence.POSTFIX;
      case PREFIX_DECREMENT, PREFIX_INCREMENT -> OperatorPrecedence.UNARY;
      case MULTIPLY, DIVIDE, REMAINDER -> OperatorPrecedence.MULTIPLICATIVE;
      case PLUS, MINUS -> OperatorPrecedence.ADDITIVE;
      case RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT, LEFT_SHIFT -> OperatorPrecedence.SHIFT;
      case LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, INSTANCE_OF ->
          OperatorPrecedence.RELATIONAL;
      case EQUAL_TO, NOT_EQUAL_TO -> OperatorPrecedence.EQUALITY;
      case AND -> OperatorPrecedence.AND;
      case XOR -> OperatorPrecedence.XOR;
      case OR -> OperatorPrecedence.OR;
      case CONDITIONAL_AND -> OperatorPrecedence.CONDITIONAL_AND;
      case CONDITIONAL_OR -> OperatorPrecedence.CONDITIONAL_OR;
      case ASSIGNMENT,
          MULTIPLY_ASSIGNMENT,
          DIVIDE_ASSIGNMENT,
          REMAINDER_ASSIGNMENT,
          PLUS_ASSIGNMENT,
          MINUS_ASSIGNMENT,
          LEFT_SHIFT_ASSIGNMENT,
          AND_ASSIGNMENT,
          XOR_ASSIGNMENT,
          OR_ASSIGNMENT,
          RIGHT_SHIFT_ASSIGNMENT,
          UNSIGNED_RIGHT_SHIFT_ASSIGNMENT ->
          OperatorPrecedence.ASSIGNMENT;
      default -> throw new IllegalArgumentException("Unexpected operator kind: " + kind);
    };
  }
}
