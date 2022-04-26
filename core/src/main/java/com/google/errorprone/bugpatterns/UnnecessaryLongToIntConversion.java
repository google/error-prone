/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_LONG_TYPE;
import static com.google.errorprone.suppliers.Suppliers.LONG_TYPE;
import static com.google.errorprone.util.ASTHelpers.targetType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import javax.lang.model.type.TypeKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Converting a long or Long to an int to pass as a long parameter is usually not necessary."
            + " If this conversion is intentional, consider `Longs.constrainToRange()` instead.",
    severity = WARNING)
public class UnnecessaryLongToIntConversion extends BugChecker
    implements MethodInvocationTreeMatcher {

  // Match static and instance methods differently because we build the suggested fix differently
  // for each case.
  private static final Matcher<ExpressionTree> LONG_TO_INT_STATIC_METHODS =
      anyOf(
          staticMethod().onClass("com.google.common.primitives.Ints").named("checkedCast"),
          staticMethod().onClass("java.lang.Math").named("toIntExact"));

  private static final Matcher<ExpressionTree> LONG_TO_INT_INSTANCE_METHODS =
      anyOf(instanceMethod().onExactClass(JAVA_LANG_LONG_TYPE).named("intValue"));

  private static final Matcher<ExpressionTree> IS_LONG_TYPE =
      anyOf(isSameType(LONG_TYPE), isSameType(JAVA_LANG_LONG_TYPE));

  // Matches calls to (long -> int) converter methods with arguments that are type long or Long.
  private static final Matcher<ExpressionTree> LONG_TO_INT_STATIC_METHOD_ON_LONG_VALUE_MATCHER =
      methodInvocation(LONG_TO_INT_STATIC_METHODS, MatchType.ALL, IS_LONG_TYPE);

  private static final Matcher<ExpressionTree> LONG_TO_INT_INSTANCE_METHOD_ON_LONG_VALUE_MATCHER =
      methodInvocation(LONG_TO_INT_INSTANCE_METHODS, MatchType.ALL, IS_LONG_TYPE);

  // To match casts, create a Matcher of type {@code Matcher<TypeCastTree>}.
  private static final Matcher<TypeCastTree> LONG_TO_INT_CAST =
      typeCast(isSameType(INT_TYPE), isSameType(LONG_TYPE));

  private static final String LONGS_CONSTRAIN_TO_RANGE_PREFIX = "Longs.constrainToRange(";
  private static final String LONGS_CONSTRAIN_TO_RANGE_SUFFIX =
      ", Integer.MIN_VALUE, Integer.MAX_VALUE)";
  private static final String LONGS_CONSTRAIN_TO_RANGE_IMPORT =
      "com.google.common.primitives.Longs";

  /**
   * Matches if a long or Long is converted to an int for a long parameter in a method invocation.
   *
   * <p>Does **not** match if the method parameter is a Long, because passing an int or Integer for
   * a Long parameter produces an incompatible types error. Does **not** match when a long or Long
   * is converted to an Integer because this requires first converting to an int and then to an
   * Integer. This is awkwardly complex and out of scope. Does **not** match when the conversion
   * occurs in a separate statement prior the method invocation.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    for (ExpressionTree arg : tree.getArguments()) {
      // The argument's type must be int.
      if (!ASTHelpers.getType(arg).getKind().equals(TypeKind.INT)) {
        continue;
      }
      // For the method being called, the parameter type must be long.
      ASTHelpers.TargetType targetType =
          targetType(state.withPath(new TreePath(state.getPath(), arg)));
      if (targetType == null) {
        continue;
      }
      if (!targetType.type().getKind().equals(TypeKind.LONG)) {
        continue;
      }

      // Match if the arg is a type cast from a long to an int.
      if (arg instanceof TypeCastTree && LONG_TO_INT_CAST.matches((TypeCastTree) arg, state)) {
        ExpressionTree castedExpression = ((TypeCastTree) arg).getExpression();
        return buildDescription(tree)
            // Remove the type cast.
            .addFix(SuggestedFix.replace(arg, state.getSourceForNode(castedExpression)))
            // Remove the type cast and use Longs.constrainToRange() instead.
            .addFix(
                SuggestedFix.builder()
                    .replace(
                        arg,
                        LONGS_CONSTRAIN_TO_RANGE_PREFIX
                            + state.getSourceForNode(castedExpression)
                            + LONGS_CONSTRAIN_TO_RANGE_SUFFIX)
                    .addImport(LONGS_CONSTRAIN_TO_RANGE_IMPORT)
                    .build())
            .build();
      }

      // Match if the arg is a static method call that converts a long or Long to an int.
      // This is separate from instance methods because we build the suggested fixes differently.
      if (LONG_TO_INT_STATIC_METHOD_ON_LONG_VALUE_MATCHER.matches(arg, state)) {
        // Get the first argument to the method. This works because the methods we are matching have
        // only one parameter, which is the long or Long parameter we care about.
        ExpressionTree methodArgExpression = ((MethodInvocationTree) arg).getArguments().get(0);
        String methodArg = state.getSourceForNode(methodArgExpression);
        return buildDescription(tree)
            // Remove the static method and just keep the arguments (i.e. the long values).
            .addFix(SuggestedFix.replace(arg, methodArg))
            // Remove the static method and suggest Longs.constrainToRange() instead.
            .addFix(
                SuggestedFix.builder()
                    .replace(
                        arg,
                        LONGS_CONSTRAIN_TO_RANGE_PREFIX
                            + methodArg
                            + LONGS_CONSTRAIN_TO_RANGE_SUFFIX)
                    .addImport(LONGS_CONSTRAIN_TO_RANGE_IMPORT)
                    .build())
            .build();
      }

      // Match if the arg is an instance method call that converts a long or Long to an int.
      if (LONG_TO_INT_INSTANCE_METHOD_ON_LONG_VALUE_MATCHER.matches(arg, state)) {
        String receiver = state.getSourceForNode(ASTHelpers.getReceiver(arg));
        return buildDescription(tree)
            // Remove the instance method and just keep the receiver (the instance of the object).
            .addFix(SuggestedFix.replace(arg, receiver))
            // Remove the instance method and wrap in Longs.ConstrainToRange().
            .addFix(
                SuggestedFix.builder()
                    .replace(
                        arg,
                        LONGS_CONSTRAIN_TO_RANGE_PREFIX
                            + receiver
                            + LONGS_CONSTRAIN_TO_RANGE_SUFFIX)
                    .addImport(LONGS_CONSTRAIN_TO_RANGE_IMPORT)
                    .build())
            .build();
      }
    }
    return NO_MATCH;
  }
}
