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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Matches usages of {@code new BigDecimal(double)} which lose precision.
 *
 * @author endobson@google.com (Eric Dobson)
 */
@BugPattern(
    name = "BigDecimalLiteralDouble",
    summary = "new BigDecimal(double) loses precision in this case.",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class BigDecimalLiteralDouble extends BugChecker implements NewClassTreeMatcher {

  private static final String ACTUAL_VALUE = " The exact value here is `new BigDecimal(\"%s\")`.";

  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

  private static final String BIG_DECIMAL = BigDecimal.class.getName();

  private static final Matcher<ExpressionTree> BIGDECIMAL_DOUBLE_CONSTRUCTOR =
      Matchers.constructor().forClass(BIG_DECIMAL).withParameters("double");

  // Matches literals and unary +/- followed by a literal, since most people conceptually think of
  // -1.0 as a literal. Doesn't handle nested unary operators as new BigDecimal(String) doesn't
  // accept multiple unary prefixes.
  private static final Matcher<ExpressionTree> FLOATING_POINT_ARGUMENT =
      (tree, state) -> {
        if (tree.getKind() == Kind.UNARY_PLUS || tree.getKind() == Kind.UNARY_MINUS) {
          tree = ((UnaryTree) tree).getExpression();
        }
        return tree.getKind() == Kind.DOUBLE_LITERAL || tree.getKind() == Kind.FLOAT_LITERAL;
      };

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!BIGDECIMAL_DOUBLE_CONSTRUCTOR.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree arg = getOnlyElement(tree.getArguments());
    if (!FLOATING_POINT_ARGUMENT.matches(arg, state)) {
      return Description.NO_MATCH;
    }

    return createDescription(arg, state);
  }

  private Description createDescription(ExpressionTree arg, VisitorState state) {
    Number literalNumber = ASTHelpers.constValue(arg, Number.class);

    if (literalNumber == null) {
      return Description.NO_MATCH;
    }
    Double literal = literalNumber.doubleValue();

    // Strip off 'd', 'f' suffixes and _ separators from the source.
    String literalString = state.getSourceForNode(arg).replaceAll("[_dDfF]", "");

    // We assume that the expected value of `new BigDecimal(double)` is precisely the BigDecimal
    // which stringifies to the same String as `double`'s literal.
    BigDecimal intendedValue;
    try {
      intendedValue = new BigDecimal(literalString);
    } catch (ArithmeticException e) {
      return Description.NO_MATCH;
    }

    // Compute the actual BigDecimal produced by the expression, and bail if they're equivalent.
    BigDecimal actualValue = new BigDecimal(literal);
    if (actualValue.compareTo(intendedValue) == 0) {
      return Description.NO_MATCH;
    }

    Optional<BigInteger> integralValue = asBigInteger(intendedValue);

    if (integralValue.map(BigDecimalLiteralDouble::isWithinLongRange).orElse(false)) {
      long longValue = integralValue.get().longValue();
      return suggestReplacement(arg, actualValue, String.format("%sL", longValue));
    }
    return suggestReplacement(arg, actualValue, String.format("\"%s\"", literalString));
  }

  private Description suggestReplacement(
      ExpressionTree tree, BigDecimal actualValue, String replacement) {
    return buildDescription(tree)
        .setMessage(message() + String.format(ACTUAL_VALUE, actualValue))
        .addFix(SuggestedFix.replace(tree, replacement))
        .build();
  }

  private static Optional<BigInteger> asBigInteger(BigDecimal v) {
    try {
      return Optional.of(v.toBigIntegerExact());
    } catch (ArithmeticException e) {
      return Optional.empty();
    }
  }

  private static boolean isWithinLongRange(BigInteger v) {
    return LONG_MIN.compareTo(v) <= 0 && v.compareTo(LONG_MAX) <= 0;
  }
}
