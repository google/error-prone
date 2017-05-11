/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

  private OperatorPrecedence(int precedence) {
    this.precedence = precedence;
  }

  public boolean isHigher(OperatorPrecedence other) {
    return precedence > other.precedence;
  }

  public static OperatorPrecedence from(Tree.Kind kind) {
    switch (kind) {
      case POSTFIX_DECREMENT:
      case POSTFIX_INCREMENT:
        return OperatorPrecedence.POSTFIX;
      case PREFIX_DECREMENT:
      case PREFIX_INCREMENT:
        return OperatorPrecedence.UNARY;
      case MULTIPLY:
      case DIVIDE:
      case REMAINDER:
        return OperatorPrecedence.MULTIPLICATIVE;
      case PLUS:
      case MINUS:
        return OperatorPrecedence.ADDITIVE;
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
      case LEFT_SHIFT:
        return OperatorPrecedence.SHIFT;
      case LESS_THAN:
      case LESS_THAN_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
      case INSTANCE_OF:
        return OperatorPrecedence.RELATIONAL;
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        return OperatorPrecedence.EQUALITY;
      case AND:
        return OperatorPrecedence.AND;
      case XOR:
        return OperatorPrecedence.XOR;
      case OR:
        return OperatorPrecedence.OR;
      case CONDITIONAL_AND:
        return OperatorPrecedence.CONDITIONAL_AND;
      case CONDITIONAL_OR:
        return OperatorPrecedence.CONDITIONAL_OR;
      case ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case AND_ASSIGNMENT:
      case XOR_ASSIGNMENT:
      case OR_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return OperatorPrecedence.ASSIGNMENT;
      default:
        throw new IllegalArgumentException("Unexpected operator kind: " + kind);
    }
  }
}
