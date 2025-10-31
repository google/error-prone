/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.collectionincompatibletype.IgnoringCasts.ignoringCasts;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibility;
import com.google.errorprone.bugpatterns.TypeCompatibility.TypeCompatibilityReport;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "The types passed to this assertion are incompatible.", severity = WARNING)
public final class JUnitIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> ASSERT_EQUALS =
      allOf(
          staticMethod()
              .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
              .namedAnyOf("assertEquals", "assertNotEquals"),
          anyOf(
              staticMethod()
                  .anyClass()
                  .withAnyName()
                  .withParameters("java.lang.Object", "java.lang.Object"),
              staticMethod()
                  .anyClass()
                  .withAnyName()
                  .withParameters("java.lang.String", "java.lang.Object", "java.lang.Object")));

  private static final Matcher<ExpressionTree> ASSERT_ARRAY_EQUALS =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .namedAnyOf("assertArrayEquals");

  private final TypeCompatibility typeCompatibility;

  @Inject
  JUnitIncompatibleType(TypeCompatibility typeCompatibility) {
    this.typeCompatibility = typeCompatibility;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    var arguments = tree.getArguments();
    if (ASSERT_EQUALS.matches(tree, state)) {
      int skip = argumentsToSkip(tree, state);
      var typeA = ignoringCasts(arguments.get(skip), state);
      var typeB = ignoringCasts(arguments.get(skip + 1), state);
      return checkCompatibility(tree, typeA, typeB, state);
    } else if (ASSERT_ARRAY_EQUALS.matches(tree, state)) {

      int skip = argumentsToSkip(tree, state);
      var expected = getUpperBound(ignoringCasts(arguments.get(skip), state), state.getTypes());
      var actual = getUpperBound(ignoringCasts(arguments.get(skip + 1), state), state.getTypes());
      if (!(expected instanceof ArrayType expectedArray)
          || !(actual instanceof ArrayType actualArray)) {
        return NO_MATCH;
      }
      return checkCompatibility(tree, expectedArray.elemtype, actualArray.elemtype, state);
    }
    return NO_MATCH;
  }

  /** Returns the number of arguments to skip so we ignore {@code message} arguments. */
  private static int argumentsToSkip(MethodInvocationTree tree, VisitorState state) {
    return isSameType(
            getSymbol(tree).getParameters().get(0).type, state.getSymtab().stringType, state)
        ? 1
        : 0;
  }

  private Description checkCompatibility(
      ExpressionTree tree, Type targetType, Type sourceType, VisitorState state) {
    TypeCompatibilityReport compatibilityReport =
        typeCompatibility.compatibilityOfTypes(targetType, sourceType, state);
    if (compatibilityReport.isCompatible()) {
      return NO_MATCH;
    }
    String sourceTypeName = Signatures.prettyType(sourceType);
    String targetTypeName = Signatures.prettyType(targetType);
    // If the pretty names are the same, fall back to full names.
    if (sourceTypeName.equals(targetTypeName)) {
      sourceTypeName = sourceType.toString();
      targetTypeName = targetType.toString();
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "The types of this assertion are mismatched: type `%s` is not compatible with `%s`"
                    + compatibilityReport.extraReason(),
                sourceTypeName,
                targetTypeName))
        .build();
  }
}
