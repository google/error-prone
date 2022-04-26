/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Set;

/** Matcher for a compound-assignment operator expression. */
public class CompoundAssignment implements Matcher<CompoundAssignmentTree> {

  private static final ImmutableSet<Kind> COMPOUND_ASSIGNMENT_OPERATORS =
      ImmutableSet.of(
          Kind.AND_ASSIGNMENT,
          Kind.DIVIDE_ASSIGNMENT,
          Kind.LEFT_SHIFT_ASSIGNMENT,
          Kind.MINUS_ASSIGNMENT,
          Kind.MULTIPLY_ASSIGNMENT,
          Kind.OR_ASSIGNMENT,
          Kind.PLUS_ASSIGNMENT,
          Kind.REMAINDER_ASSIGNMENT,
          Kind.RIGHT_SHIFT_ASSIGNMENT,
          Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
          Kind.XOR_ASSIGNMENT);

  private final Set<Kind> operators;
  private final Matcher<ExpressionTree> receiverMatcher;
  private final Matcher<ExpressionTree> expressionMatcher;

  /**
   * Creates a new compound-assignment operator matcher, which matches a compound assignment
   * expression with one of a set of operators and whose receiver and expression match the given
   * matchers.
   *
   * @param operators The set of matching compound-assignment operators. These are drawn from the
   *     {@link Kind} enum values which link to {@link CompoundAssignmentTree} in their javadoc.
   * @param receiverMatcher The matcher which must match the receiver which will be assigned to.
   * @param expressionMatcher The matcher which must match the right-hand expression to the compound
   *     assignment.
   */
  public CompoundAssignment(
      Set<Kind> operators,
      Matcher<ExpressionTree> receiverMatcher,
      Matcher<ExpressionTree> expressionMatcher) {
    this.operators = validateOperators(operators);
    if (receiverMatcher == null) {
      throw new NullPointerException("CompoundAssignment receiver matcher is null");
    }
    if (expressionMatcher == null) {
      throw new NullPointerException("CompoundAssignment expression matcher is null");
    }
    this.receiverMatcher = receiverMatcher;
    this.expressionMatcher = expressionMatcher;
  }

  @Override
  public boolean matches(CompoundAssignmentTree compoundAssignmentTree, VisitorState state) {
    if (!operators.contains(compoundAssignmentTree.getKind())) {
      return false;
    }
    return receiverMatcher.matches(compoundAssignmentTree.getVariable(), state)
        && expressionMatcher.matches(compoundAssignmentTree.getExpression(), state);
  }

  /**
   * Returns the provided set of operators if they are all compound-assignment operators. Otherwise,
   * throws an IllegalArgumentException.
   */
  private static Set<Kind> validateOperators(Set<Kind> kinds) {
    for (Kind kind : kinds) {
      if (!COMPOUND_ASSIGNMENT_OPERATORS.contains(kind)) {
        throw new IllegalArgumentException(kind.name() + " is not a compound-assignment operator.");
      }
    }
    return kinds;
  }
}
