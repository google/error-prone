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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.suppliers.Suppliers.BYTE_TYPE;
import static com.google.errorprone.suppliers.Suppliers.CHAR_TYPE;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.matchBinaryTree;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Comparison to value that is out of range for the compared type",
    explanation =
        "This checker looks for equality comparisons to values that are out of "
            + "range for the compared type.  For example, bytes may have a value in the range "
            + Byte.MIN_VALUE
            + " to "
            + Byte.MAX_VALUE
            + ". Comparing a byte for equality with a value "
            + "outside that range will always evaluate to false and usually indicates an error in "
            + "the code.",
    severity = ERROR)
public class ComparisonOutOfRange extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    BOUNDS_FOR_PRIMITIVES.forEach(
        (testedValueType, range) -> {
          // Match trees that have one literal operand and another of the specified type.
          List<ExpressionTree> binaryTreeMatches =
              matchBinaryTree(
                  tree,
                  Arrays.asList(
                      ComparisonOutOfRange::hasNumericLiteralValue, isSameType(testedValueType)),
                  state);
          if (binaryTreeMatches == null) {
            return;
          }
          Tree literal = binaryTreeMatches.get(0);

          Number numericLiteralValue =
              constValue(literal) instanceof Character
                  ? Long.valueOf(((Character) constValue(literal)).charValue())
                  : (Number) constValue(literal);

          // We define a class whose first method we'll call immediately.
          // This lets us have a bunch of fields and parameters in scope for our helper methods.
          class MatchAttempt<N extends Number & Comparable<? super N>> {
            final N literalValue;
            final N minValue;
            final N maxValue;

            MatchAttempt(Function<Number, N> numericPromotion) {
              this.literalValue = numericPromotion.apply(numericLiteralValue);
              this.minValue = numericPromotion.apply(range.lowerEndpoint());
              this.maxValue = numericPromotion.apply(range.upperEndpoint());
            }

            Description matchConstantResult() {
              switch (tree.getKind()) {
                case EQUAL_TO:
                  return matchOutOfBounds(/* willEvaluateTo= */ false);
                case NOT_EQUAL_TO:
                  return matchOutOfBounds(/* willEvaluateTo= */ true);

                default:
                  return NO_MATCH;
              }
            }

            Description matchOutOfBounds(boolean willEvaluateTo) {
              return literalValue.compareTo(minValue) < 0 || literalValue.compareTo(maxValue) > 0
                  ? describeMatch(willEvaluateTo)
                  : NO_MATCH;
            }

            /**
             * Suggested fixes are as follows. For the byte equality case, convert the literal to
             * its byte representation. For example, "255" becomes "-1. For other cases, replace the
             * comparison with "true"/"false" since it's not clear what was intended and that is
             * semantically equivalent.
             *
             * <p>TODO(eaftan): Suggested fixes don't handle side-effecting expressions, such as (d
             * = reader.read()) == -1. Maybe add special case handling for assignments.
             */
            Description describeMatch(boolean willEvaluateTo) {
              boolean byteEqualityMatch =
                  isSameType(testedValueType.get(state), state.getSymtab().byteType, state);

              return buildDescription(tree)
                  .addFix(
                      byteEqualityMatch
                          ? replace(literal, Byte.toString(literalValue.byteValue()))
                          : replace(tree, Boolean.toString(willEvaluateTo)))
                  .setMessage(
                      format(
                          "%ss may have a value in the range %d to %d; "
                              + "therefore, this comparison to %s will always evaluate to %s",
                          testedValueType.get(state).tsym.name,
                          range.lowerEndpoint(),
                          range.upperEndpoint(),
                          state.getSourceForNode(literal),
                          willEvaluateTo))
                  .build();
            }
          }

          // JLS 5.6.2 - Binary Numeric Promotion: - If either is double, other is converted to
          // double. - If either is float, other is converted to float. - If either is long, other
          // is converted to long. - Otherwise, both are converted to int.
          //
          // Here, we're looking only at comparisons between an integral type and some literal.
          // Thus, the only value that can be floating-point is the literal, and that's the only
          // case in which we promote to a floating-point type.
          //
          // We promote both the constant and the bounds.
          //
          // Promoting the constant changes nothing: It it was integral, it remains integral and
          // still has the same value, even if it's represented as a larger type. If it was
          // floating-point, it's either untouched or promoted from float to double, which gives it
          // a more precise type that it can't take advantage of because its source was still a
          // float.
          //
          // Promoting the bound *can* change its value in the case of promoting an integral value
          // to a floating-point value: For example, Integer.MAX_VALUE is rounded up to 2^31,
          // regardless of whether we're promoting to float or to double. Thus, an equality
          // comparison between an int and a float/double value of 2^31 is allowed, since it can be
          // either true or false, even though you'd need to move from int to long in order to
          // actually hold the value 2^31.
          //
          // Thus:
          //
          // When we promote integral types, we can always promote to long.
          //
          // When we promote to floating-point types, I *think* we could get away with always
          // promoting to double. That would work because the bounds of int, etc. round to the same
          // value, whether we're promoting them to float or to double (thanks to how close the
          // bounds are to a power of two). However, that feels scarier to rely on, especially if we
          // might ever use this promotion code for other purposes, like to determine whether a
          // value promoted to float has any fractional part.
          MatchAttempt<?> matchAttempt;
          if (numericLiteralValue instanceof Double) {
            matchAttempt = new MatchAttempt<>(Number::doubleValue);
          } else if (numericLiteralValue instanceof Float) {
            matchAttempt = new MatchAttempt<>(Number::floatValue);
          } else {
            // It's an Integer or a Long (sometimes because we replaced a Character with a Long).
            matchAttempt = new MatchAttempt<>(Number::longValue);
          }
          state.reportMatch(matchAttempt.matchConstantResult());
        });
    return NO_MATCH;
  }

  private static final ImmutableMap<Supplier<Type>, Range<Long>> BOUNDS_FOR_PRIMITIVES =
      ImmutableMap.of(
          BYTE_TYPE, range(Byte.MIN_VALUE, Byte.MAX_VALUE),
          CHAR_TYPE, range(Character.MIN_VALUE, Character.MAX_VALUE));

  private static Range<Long> range(long min, long max) {
    return Range.closed(min, max);
  }

  private static boolean hasNumericLiteralValue(ExpressionTree tree, VisitorState state) {
    return (tree instanceof JCLiteral)
        && (constValue(tree) instanceof Number || constValue(tree) instanceof Character);
  }
}
