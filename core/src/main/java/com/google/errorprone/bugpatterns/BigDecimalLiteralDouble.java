/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.math.BigDecimal;
import java.math.BigInteger;

/** @author endobson@google.com (Eric Dobson) */
@BugPattern(
  name = "BigDecimalLiteralDouble",
  summary =
      "BigDecimal(double) and BigDecimal.valueOf(double) may lose precision, "
          + "prefer BigDecimal(String) or BigDecimal(long)",
  category = JDK,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class BigDecimalLiteralDouble extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

  private static final String BIG_DECIMAL = BigDecimal.class.getName();

  private static final Matcher<ExpressionTree> valueOfMethod =
      staticMethod().onClass(BIG_DECIMAL).named("valueOf").withParameters("double");

  private static final Matcher<ExpressionTree> constructor =
      Matchers.constructor().forClass(BIG_DECIMAL).withParameters("double");

  // Matches literals and unary +/- and a literal, since most people conceptually think of -1.0 as a
  // literal. Doesn't handle nested unary operators as new BigDecimal(String) doesn't accept
  // multiple unary prefixes.
  private static final Matcher<ExpressionTree> literalArgument =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          if (tree.getKind() == Kind.UNARY_PLUS || tree.getKind() == Kind.UNARY_MINUS) {
            tree = ((UnaryTree) tree).getExpression();
          }
          return tree.getKind() == Kind.DOUBLE_LITERAL;
        }
      };

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!valueOfMethod.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree arg = tree.getArguments().get(0);
    if (!literalArgument.matches(arg, state)) {
      return Description.NO_MATCH;
    }

    // Don't suggest an integral replacement in this case as it may change the scale of the
    // resulting BigDecimal.
    return createDescription(tree, arg, state, false);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!constructor.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree arg = tree.getArguments().get(0);
    if (!literalArgument.matches(arg, state)) {
      return Description.NO_MATCH;
    }

    return createDescription(tree, arg, state, true);
  }

  public Description createDescription(
      ExpressionTree tree, ExpressionTree arg, VisitorState state, boolean suggestIntegral) {
    String literal = state.getSourceForNode(arg);

    if (literal == null) {
      return describeMatch(tree);
    }

    // BigDecimal doesn't seem to support underscores or terminal Ds in its string parsing
    literal = literal.replaceAll("[_dD]", "");

    BigDecimal intendedValue = new BigDecimal(literal);
    Optional<BigInteger> integralValue = asBigInteger(intendedValue);

    Description.Builder description = buildDescription(tree);
    if (suggestIntegral && integralValue.isPresent() && isWithinLongRange(integralValue.get())) {
      long longValue = integralValue.get().longValue();
      String suggestedString;
      switch (Ints.saturatedCast(longValue)) {
        case 0:
          suggestedString = "BigDecimal.ZERO";
          break;
        case 1:
          suggestedString = "BigDecimal.ONE";
          break;
        case 10:
          suggestedString = "BigDecimal.TEN";
          break;
        default:
          suggestedString = "new BigDecimal(" + longValue + "L)";
      }

      description.addFix(
          SuggestedFix.builder()
              .addImport("java.math.BigDecimal")
              .replace(tree, suggestedString)
              .build());
    }
    description.addFix(
        SuggestedFix.builder()
            .addImport("java.math.BigDecimal")
            .replace(tree, "new BigDecimal(\"" + literal + "\")")
            .build());
    return description.build();
  }

  public static Optional<BigInteger> asBigInteger(BigDecimal v) {
    try {
      return Optional.of(v.toBigIntegerExact());
    } catch (ArithmeticException e) {
      return Optional.absent();
    }
  }

  private boolean isWithinLongRange(BigInteger v) {
    return LONG_MIN.compareTo(v) <= 0 && v.compareTo(LONG_MAX) <= 0;
  }
}
