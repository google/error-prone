/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasArity;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;

/** @author irogers@google.com (Ian Rogers) */
@BugPattern(
  name = "BadComparable",
  summary = "Possible sign flip from narrowing conversion",
  explanation =
      "A narrowing integral conversion can cause a sign flip, since it simply discards all"
          + " but the n lowest order bits, where n is the number of bits used to represent"
          + " the target type (JLS 5.1.3). In a compare or compareTo method, this can cause"
          + " incorrect and unstable sort orders.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class BadComparable extends BugChecker implements TypeCastTreeMatcher {
  /** Matcher for the overriding method of 'int java.lang.Comparable.compareTo(T other)' */
  private static final Matcher<MethodTree> COMPARABLE_METHOD_MATCHER =
      allOf(
          methodIsNamed("compareTo"),
          methodHasVisibility(PUBLIC),
          methodReturns(INT_TYPE),
          methodHasArity(1));

  private static final Matcher<ClassTree> COMPARABLE_CLASS_MATCHER =
      isSubtypeOf("java.lang.Comparable");

  /** Matcher for the overriding method of 'int java.util.Comparator.compare(T o1, T o2)' */
  private static final Matcher<MethodTree> COMPARATOR_METHOD_MATCHER =
      allOf(
          methodIsNamed("compare"),
          methodHasVisibility(PUBLIC),
          methodReturns(INT_TYPE),
          methodHasArity(2));

  private static final Matcher<ClassTree> COMPARATOR_CLASS_MATCHER =
      isSubtypeOf("java.util.Comparator");

  /**
   * Compute the type of the subtract BinaryTree. We use the type of the left/right operand except
   * when they're not the same, in which case we prefer the type of the expression. This ensures
   * that a byte/short subtracted from another byte/short isn't regarded as an int.
   */
  private static Type getTypeOfSubtract(BinaryTree expression) {
    Type expressionType = ASTHelpers.getType(expression.getLeftOperand());
    if (!expressionType.equals(ASTHelpers.getType(expression.getRightOperand()))) {
      return ASTHelpers.getType(expression);
    }
    return expressionType;
  }

  /**
   * Matches if this is a narrowing integral cast between signed types where the expression is a
   * subtract.
   */
  private boolean matches(TypeCastTree tree, VisitorState state) {
    Type treeType = ASTHelpers.getType(tree.getType());

    // If the cast isn't narrowing to an int then don't implicate it in the bug pattern.
    if (treeType.getTag() != TypeTag.INT) {
      return false;
    }

    // The expression should be a subtract but remove parentheses.
    ExpressionTree expression = ASTHelpers.stripParentheses(tree.getExpression());
    if (expression.getKind() != Kind.MINUS) {
      return false;
    }

    // Ensure the expression type is wider and signed (ie a long) than the cast type ignoring
    // boxing.
    Type expressionType = getTypeOfSubtract((BinaryTree) expression);
    TypeTag expressionTypeTag = state.getTypes().unboxedTypeOrType(expressionType).getTag();
    return (expressionTypeTag == TypeTag.LONG);
  }

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    // Check for a narrowing match first as its simplest match to test.
    if (!matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // Test that the match is in a Comparable.compareTo or Comparator.compare method.
    ClassTree declaringClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (!COMPARABLE_CLASS_MATCHER.matches(declaringClass, state)
        && !COMPARATOR_CLASS_MATCHER.matches(declaringClass, state)) {
      return Description.NO_MATCH;
    }
    MethodTree method = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (method == null) {
      return Description.NO_MATCH;
    }
    if (!COMPARABLE_METHOD_MATCHER.matches(method, state)
        && !COMPARATOR_METHOD_MATCHER.matches(method, state)) {
      return Description.NO_MATCH;
    }

    // Get the unparenthesized expression.
    BinaryTree subtract = (BinaryTree) ASTHelpers.stripParentheses(tree.getExpression());
    ExpressionTree lhs = subtract.getLeftOperand();
    ExpressionTree rhs = subtract.getRightOperand();
    Fix fix;
    if (ASTHelpers.getType(lhs).isPrimitive()) {
      fix = SuggestedFix.replace(tree, "Long.compare(" + lhs + ", " + rhs + ")");
    } else {
      fix = SuggestedFix.replace(tree, lhs + ".compareTo(" + rhs + ")");
    }
    return describeMatch(tree, fix);
  }
}
