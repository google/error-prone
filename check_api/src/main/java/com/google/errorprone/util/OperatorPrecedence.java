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
import java.util.Optional;

/**
 * The precedence for an operator kind in the {@link com.sun.source.tree} API.
 *
 * <p>As documented at: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
 */
public enum OperatorPrecedence {
  POSTFIX(14),
  UNARY(13),
  CAST(12),
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
    return optionallyFrom(kind)
        .orElseThrow(() -> new IllegalArgumentException("Unexpected operator kind: " + kind));
  }

  public static Optional<OperatorPrecedence> optionallyFrom(Tree.Kind kind) {
    return switch (kind) {
      case POSTFIX_DECREMENT, POSTFIX_INCREMENT -> Optional.of(OperatorPrecedence.POSTFIX);
      case PREFIX_DECREMENT, PREFIX_INCREMENT -> Optional.of(OperatorPrecedence.UNARY);
      case TYPE_CAST -> Optional.of(OperatorPrecedence.CAST);
      case MULTIPLY, DIVIDE, REMAINDER -> Optional.of(OperatorPrecedence.MULTIPLICATIVE);
      case PLUS, MINUS -> Optional.of(OperatorPrecedence.ADDITIVE);
      case RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT, LEFT_SHIFT -> Optional.of(OperatorPrecedence.SHIFT);
      case LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, INSTANCE_OF ->
          Optional.of(OperatorPrecedence.RELATIONAL);
      case EQUAL_TO, NOT_EQUAL_TO -> Optional.of(OperatorPrecedence.EQUALITY);
      case AND -> Optional.of(OperatorPrecedence.AND);
      case XOR -> Optional.of(OperatorPrecedence.XOR);
      case OR -> Optional.of(OperatorPrecedence.OR);
      case CONDITIONAL_AND -> Optional.of(OperatorPrecedence.CONDITIONAL_AND);
      case CONDITIONAL_OR -> Optional.of(OperatorPrecedence.CONDITIONAL_OR);
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
          Optional.of(OperatorPrecedence.ASSIGNMENT);
      default -> Optional.empty();
    };
  }
}
