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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeKind;

/**
 * Detects floating-point assertEquals() calls that will not work in JUnit 4.
 *
 * <p>JUnit 4 bans most but not all floating-point comparisons without a delta argument. This check
 * will be as strict as JUnit 4, no more and no less.
 *
 * @author mwacker@google.com (Mike Wacker)
 */
@BugPattern(
  name = "JUnit3FloatingPointComparisonWithoutDelta",
  summary = "Floating-point comparison without error tolerance",
  // First sentence copied directly from JUnit 4.
  explanation =
      "Use assertEquals(expected, actual, delta) to compare floating-point numbers. "
          + "This call to assertEquals() will either fail or not compile in JUnit 4. "
          + "Use assertEquals(expected, actual, 0.0) if the delta must be 0.",
  category = JUNIT,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class JUnit3FloatingPointComparisonWithoutDelta extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSERT_EQUALS_MATCHER =
      MethodMatchers.staticMethod().onClass("junit.framework.TestCase").named("assertEquals");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!ASSERT_EQUALS_MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    List<Type> argumentTypes = getArgumentTypesWithoutMessage(methodInvocationTree, state);
    if (canBeConvertedToJUnit4(state, argumentTypes)) {
      return Description.NO_MATCH;
    }
    Fix fix = addDeltaArgument(methodInvocationTree, state, argumentTypes);
    return describeMatch(methodInvocationTree, fix);
  }

  /** Gets the argument types, excluding the message argument if present. */
  private List<Type> getArgumentTypesWithoutMessage(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<Type> argumentTypes = new ArrayList<>();
    for (ExpressionTree argument : methodInvocationTree.getArguments()) {
      JCTree tree = (JCTree) argument;
      argumentTypes.add(tree.type);
    }
    removeMessageArgumentIfPresent(state, argumentTypes);
    return argumentTypes;
  }

  /** Removes the message argument if it is present. */
  private void removeMessageArgumentIfPresent(VisitorState state, List<Type> argumentTypes) {
    if (argumentTypes.size() == 2) {
      return;
    }
    Types types = state.getTypes();
    Type firstType = argumentTypes.get(0);
    if (types.isSameType(firstType, state.getSymtab().stringType)) {
      argumentTypes.remove(0);
    }
  }

  /**
   * Determines if the invocation can be safely converted to JUnit 4 based on its argument types.
   */
  private boolean canBeConvertedToJUnit4(VisitorState state, List<Type> argumentTypes) {
    // Delta argument is used.
    if (argumentTypes.size() > 2) {
      return true;
    }
    Type firstType = argumentTypes.get(0);
    Type secondType = argumentTypes.get(1);
    // Neither argument is floating-point.
    if (!isFloatingPoint(state, firstType) && !isFloatingPoint(state, secondType)) {
      return true;
    }
    // One argument is not numeric.
    if (!isNumeric(state, firstType) || !isNumeric(state, secondType)) {
      return true;
    }
    // Neither argument is primitive.
    if (!firstType.isPrimitive() && !secondType.isPrimitive()) {
      return true;
    }
    return false;
  }

  /** Determines if the type is a floating-point type, including reference types. */
  private boolean isFloatingPoint(VisitorState state, Type type) {
    Type trueType = unboxedTypeOrType(state, type);
    return (trueType.getKind() == TypeKind.DOUBLE) || (trueType.getKind() == TypeKind.FLOAT);
  }

  /**
   * Determines if the type is a numeric type, including reference types.
   *
   * <p>Type.isNumeric() does not handle reference types properly.
   */
  private boolean isNumeric(VisitorState state, Type type) {
    Type trueType = unboxedTypeOrType(state, type);
    return trueType.isNumeric();
  }

  /** Gets the unboxed type, or the original type if it is not unboxable. */
  private Type unboxedTypeOrType(VisitorState state, Type type) {
    Types types = state.getTypes();
    return types.unboxedTypeOrType(type);
  }

  /** Creates the fix to add a delta argument. */
  private Fix addDeltaArgument(
      MethodInvocationTree methodInvocationTree, VisitorState state, List<Type> argumentTypes) {
    int insertionIndex = getDeltaInsertionIndex(methodInvocationTree, state);
    String deltaArgument = getDeltaArgument(state, argumentTypes);
    return SuggestedFix.replace(insertionIndex, insertionIndex, deltaArgument);
  }

  /** Gets the index of where to insert the delta argument. */
  private int getDeltaInsertionIndex(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    JCTree lastArgument = (JCTree) Iterables.getLast(methodInvocationTree.getArguments());
    return state.getEndPosition(lastArgument);
  }

  /** Gets the text for the delta argument to be added. */
  private String getDeltaArgument(VisitorState state, List<Type> argumentTypes) {
    Type firstType = argumentTypes.get(0);
    Type secondType = argumentTypes.get(1);
    boolean doublePrecisionUsed = isDouble(state, firstType) || isDouble(state, secondType);
    return doublePrecisionUsed ? ", 0.0" : ", 0.0f";
  }

  /** Determines if the type is a double, including reference types. */
  private boolean isDouble(VisitorState state, Type type) {
    Type trueType = unboxedTypeOrType(state, type);
    return trueType.getKind() == TypeKind.DOUBLE;
  }
}
