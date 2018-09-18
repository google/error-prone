/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.TRUTH;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.Optional;

/**
 * Detects usages of {@code Float,DoubleSubject.isWithin(TOLERANCE).of(EXPECTED)} where there are no
 * other floating point values other than {@code EXPECTED} with satisfy the assertion, but {@code
 * TOLERANCE} is not zero. Likewise for older-style JUnit assertions ({@code assertEquals(double,
 * double, double)}).
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "FloatingPointAssertionWithinEpsilon",
    summary =
        "This fuzzy equality check is using a tolerance less than the gap to the next number. "
            + "You may want a less restrictive tolerance, or to assert equality.",
    category = TRUTH,
    severity = WARNING,
    tags = StandardTags.SIMPLIFICATION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class FloatingPointAssertionWithinEpsilon extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String DESCRIPTION =
      "This fuzzy equality check is using a tolerance less than the gap to the next number "
          + "(which is ~%.2g). You may want a less restrictive tolerance, or to assert equality.";

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    for (FloatingPointType floatingPointType : FloatingPointType.values()) {
      Optional<Description> description = floatingPointType.match(this, tree, state);
      if (description.isPresent()) {
        return description.get();
      }
    }
    return Description.NO_MATCH;
  }

  @SuppressWarnings("ImmutableEnumChecker")
  private enum FloatingPointType {
    FLOAT(
        TypeTag.FLOAT, "float", "com.google.common.truth.FloatSubject", "TolerantFloatComparison") {
      @Override
      Float nextNumber(Number actual) {
        float number = actual.floatValue();
        return Math.min(Math.nextUp(number) - number, number - Math.nextDown(number));
      }

      @Override
      boolean isIntolerantComparison(Number tolerance, Number actual) {
        return tolerance.floatValue() != 0 && tolerance.floatValue() < nextNumber(actual);
      }

      @Override
      Optional<String> suffixLiteralIfPossible(LiteralTree literal, VisitorState state) {
        // If the value passed to #of was being converted to a float, we can make that explicit with
        // an "f" qualifier.
        return Optional.of(removeSuffixes(state.getSourceForNode(literal)) + "f");
      }
    },
    DOUBLE(
        TypeTag.DOUBLE,
        "double",
        "com.google.common.truth.DoubleSubject",
        "TolerantDoubleComparison") {
      @Override
      Double nextNumber(Number actual) {
        double number = actual.doubleValue();
        return Math.min(Math.nextUp(number) - number, number - Math.nextDown(number));
      }

      @Override
      boolean isIntolerantComparison(Number tolerance, Number actual) {
        return tolerance.doubleValue() != 0 && tolerance.doubleValue() < nextNumber(actual);
      }

      @Override
      Optional<String> suffixLiteralIfPossible(LiteralTree literal, VisitorState state) {
        String literalString = removeSuffixes(state.getSourceForNode(literal));
        double asDouble;
        try {
          asDouble = Double.parseDouble(literalString);
        } catch (NumberFormatException nfe) {
          return Optional.empty();
        }
        // We need to double-check that the value with a "d" suffix has the same value. For example,
        // 0.1f != 0.1d, so must be replaced with (double) 0.1f
        if (asDouble == ASTHelpers.constValue(literal, Number.class).doubleValue()) {
          return Optional.of(literalString.contains(".") ? literalString : literalString + "d");
        }
        return Optional.empty();
      }
    };

    private final TypeTag typeTag;
    private final String typeName;
    private final Matcher<MethodInvocationTree> truthOfCall;
    private final Matcher<ExpressionTree> junitWithoutMessage;
    private final Matcher<ExpressionTree> junitWithMessage;

    FloatingPointType(
        TypeTag typeTag, String typeName, String subjectClass, String tolerantSubclass) {
      this.typeTag = typeTag;
      this.typeName = typeName;
      String tolerantClass = subjectClass + "." + tolerantSubclass;
      truthOfCall =
          allOf(
              instanceMethod().onDescendantOf(tolerantClass).named("of").withParameters(typeName),
              Matchers.receiverOfInvocation(
                  instanceMethod()
                      .onDescendantOf(subjectClass)
                      .namedAnyOf("isWithin", "isNotWithin")
                      .withParameters(typeName)));
      MethodNameMatcher junitAssert =
          staticMethod().onClass("org.junit.Assert").named("assertEquals");
      junitWithoutMessage = junitAssert.withParameters(typeName, typeName, typeName);
      junitWithMessage =
          junitAssert.withParameters("java.lang.String", typeName, typeName, typeName);
    }

    abstract Number nextNumber(Number actual);

    abstract boolean isIntolerantComparison(Number tolerance, Number actual);

    abstract Optional<String> suffixLiteralIfPossible(LiteralTree literal, VisitorState state);

    private Optional<Description> match(
        BugChecker bugChecker, MethodInvocationTree tree, VisitorState state) {
      if (junitWithoutMessage.matches(tree, state)) {
        return check(tree.getArguments().get(2), tree.getArguments().get(0))
            .map(
                tolerance ->
                    suggestJunitFix(bugChecker, tree)
                        .setMessage(String.format(DESCRIPTION, tolerance))
                        .build());
      }
      if (junitWithMessage.matches(tree, state)) {
        return check(tree.getArguments().get(3), tree.getArguments().get(1))
            .map(
                tolerance ->
                    suggestJunitFix(bugChecker, tree)
                        .setMessage(String.format(DESCRIPTION, tolerance))
                        .build());
      }
      if (truthOfCall.matches(tree, state)) {
        return check(getReceiverArgument(tree), getOnlyElement(tree.getArguments()))
            .map(
                tolerance ->
                    suggestTruthFix(bugChecker, tree, state)
                        .setMessage(String.format(DESCRIPTION, tolerance))
                        .build());
      }
      return Optional.empty();
    }

    /**
     * Checks whether the provided {@code toleranceArgument} and {@code actualArgument} will lead to
     * an equality check. If so, returns the smallest tolerance that wouldn't for diagnostic
     * purposes.
     */
    private Optional<Double> check(
        ExpressionTree toleranceArgument, ExpressionTree actualArgument) {
      Number actual = ASTHelpers.constValue(actualArgument, Number.class);
      Number tolerance = ASTHelpers.constValue(toleranceArgument, Number.class);
      if (actual == null || tolerance == null) {
        return Optional.empty();
      }
      return isIntolerantComparison(tolerance, actual)
          ? Optional.of(nextNumber(actual).doubleValue())
          : Optional.empty();
    }

    private static ExpressionTree getReceiverArgument(MethodInvocationTree tree) {
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      return getOnlyElement(((MethodInvocationTree) receiver).getArguments());
    }

    /** Suggest replacing the tolerance with {@code 0} for JUnit assertions. */
    private static Description.Builder suggestJunitFix(
        BugChecker bugChecker, MethodInvocationTree tree) {
      SuggestedFix fix = SuggestedFix.replace(getLast(tree.getArguments()), "0");
      return bugChecker.buildDescription(tree).addFix(fix);
    }

    /** Suggest replacing {@code isWithin(..).of(foo)} with {@code isEqualTo(foo)} for Truth. */
    private Description.Builder suggestTruthFix(
        BugChecker bugChecker, MethodInvocationTree tree, VisitorState state) {
      ExpressionTree within = ASTHelpers.getReceiver(tree);
      ExpressionTree assertion = ASTHelpers.getReceiver(within);
      String replacementMethod =
          ASTHelpers.getSymbol(within).getSimpleName().toString().contains("Not")
              ? "isNotEqualTo"
              : "isEqualTo";
      ExpressionTree argument = getOnlyElement(tree.getArguments());
      SuggestedFix fix =
          SuggestedFix.replace(
              tree,
              String.format(
                  "%s.%s(%s)",
                  state.getSourceForNode(assertion),
                  replacementMethod,
                  castArgumentIfNecessary(argument, state)));
      return bugChecker.buildDescription(tree).addFix(fix);
    }

    private String castArgumentIfNecessary(ExpressionTree tree, VisitorState state) {
      String source = state.getSourceForNode(tree);
      Type type = ASTHelpers.getType(tree);
      if (state.getTypes().unboxedTypeOrType(type).getTag() == typeTag) {
        return source;
      }
      if (tree instanceof LiteralTree) {
        Optional<String> suffixed = suffixLiteralIfPossible((LiteralTree) tree, state);
        if (suffixed.isPresent()) {
          return suffixed.get();
        }
      }
      if (ASTHelpers.requiresParentheses(tree, state)) {
        return String.format("(%s) (%s)", typeName, source);
      }
      return String.format("(%s) %s", typeName, source);
    }

    static String removeSuffixes(String source) {
      return source.replaceAll("[fFdDlL]$", "");
    }
  }
}
