/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_RUNNER_CLASS;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_RUN_WITH_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_TEST_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestCases;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argumentCount;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.kindAnyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.sun.source.tree.Tree.Kind.BREAK;
import static com.sun.source.tree.Tree.Kind.CONTINUE;
import static com.sun.source.tree.Tree.Kind.RETURN;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.OptionalInt;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Migrate loops in tests to use github.com/google/TestParameterInjector. Test"
            + " parameterization executes each input case in strict isolation, ensuring that a"
            + " single failure doesn't halt the rest of your test case while providing clear,"
            + " per-case reporting without the need for manual loops.",
    severity = WARNING)
public final class LoopToTestParameter extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ExpressionTree> ENUM_VALUES =
      staticMethod().onDescendantOf("java.lang.Enum").named("values").withNoParameters();

  /**
   * Matches common wrappers around {@code Enum.values()} like {@code Arrays.asList(Enum.values())}.
   */
  private static final Matcher<MethodInvocationTree> WRAPPED_ENUM_VALUES =
      allOf(
          argumentCount(1),
          hasArguments(MatchType.ALL, ENUM_VALUES),
          anyOf(
              staticMethod().onClass("java.util.Arrays").named("asList"),
              staticMethod().onClass("com.google.common.collect.ImmutableList").named("copyOf"),
              staticMethod().onClass("com.google.common.collect.ImmutableSet").named("copyOf")));

  /** Matches common ways of iterating over all enum values. */
  private static final Matcher<ExpressionTree> ENUM_VALUES_ITERABLE =
      anyOf(
          ENUM_VALUES,
          staticMethod().onClass("java.util.EnumSet").named("allOf"),
          toType(MethodInvocationTree.class, WRAPPED_ENUM_VALUES));

  // NOTE: don't use JUnitMatchers.TEST_CASE because we don't want to match JUnit3 test methods!
  private static final Matcher<MethodTree> TEST_METHOD = hasAnnotation(JUNIT4_TEST_ANNOTATION);

  private static final String TEST_PARAMETER_INJECTOR_TYPE =
      "com.google.testing.junit.testparameterinjector.TestParameterInjector";

  private static final String TEST_PARAMETER_TYPE =
      "com.google.testing.junit.testparameterinjector.TestParameter";

  private static final Matcher<AnnotationTree> COMPATIBLE_RUNNER =
      allOf(
          isType(JUNIT4_RUN_WITH_ANNOTATION),
          hasArgumentWithValue(
              "value",
              classLiteral(
                  anyOf(
                      isSameType(JUNIT4_RUNNER_CLASS), isSameType(TEST_PARAMETER_INJECTOR_TYPE)))));

  /**
   * Skipping refactoring if the loop body contains custom control flow (continue, break, return)
   * because unwrapping them can lead to compile errors (continue/break outside loop) or semantic
   * changes (return stopping all iterations vs one parameter set).
   */
  private static final Matcher<Tree> CONTAINS_CUSTOM_CONTROL_FLOW =
      contains(kindAnyOf(ImmutableSet.of(CONTINUE, BREAK, RETURN)));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    // Skip non-testonly builds and classes that don't have any JUnit4 test cases.
    if (!state.errorProneOptions().isTestOnlyTarget() || !hasJUnit4TestCases.matches(tree, state)) {
      return NO_MATCH;
    }

    // Skip classes with a non-compatible @RunWith annotation.
    AnnotationTree runWithAnnotation =
        getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "RunWith");
    if (runWithAnnotation != null && !COMPATIBLE_RUNNER.matches(runWithAnnotation, state)) {
      return NO_MATCH;
    }

    // Process each method and collect suggested fixes.
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (Tree member : tree.getMembers()) {
      if (member instanceof MethodTree method) {
        processMethod(method, state, fix);
      }
    }
    if (fix.isEmpty()) {
      return NO_MATCH;
    }

    // Update the @RunWith annotation (or add one if there isn't already a compatible one).
    String runWith = SuggestedFixes.qualifyType(state, fix, JUNIT4_RUN_WITH_ANNOTATION);
    String testParameterInjector =
        SuggestedFixes.qualifyType(state, fix, TEST_PARAMETER_INJECTOR_TYPE);

    String newRunWith = String.format("@%s(%s.class)", runWith, testParameterInjector);
    if (runWithAnnotation == null) {
      fix.prefixWith(tree, newRunWith + "\n");
    } else {
      fix.replace(runWithAnnotation, newRunWith);
    }

    return describeMatch(tree, fix.build());
  }

  /**
   * Returns true if the method's parameters are compatible with {@code TestParameterInjector}
   * (either no parameters, or all annotated with {@code @TestParameter}).
   */
  private static boolean compatibleParameters(MethodTree method, VisitorState state) {
    return method.getParameters().stream()
        .allMatch(param -> hasAnnotation(getSymbol(param), TEST_PARAMETER_TYPE, state));
  }

  /**
   * Processes a single method, checking if it is a test method with a loop that can be migrated,
   * and appends suggested fixes to the builder.
   */
  private static void processMethod(MethodTree tree, VisitorState state, SuggestedFix.Builder fix) {
    // Restrict to @Test methods with compatible parameters and exactly one statement (the loop).
    // This is a safety measure to avoid breaking tests with complex setup/teardown.
    if (!TEST_METHOD.matches(tree, state)
        || !compatibleParameters(tree, state)
        || tree.getBody() == null
        || tree.getBody().getStatements().size() != 1) {
      return;
    }

    StatementTree statement = getOnlyElement(tree.getBody().getStatements());
    if (statement instanceof EnhancedForLoopTree loopTree
        && ENUM_VALUES_ITERABLE.matches(loopTree.getExpression(), state)) {
      // Don't refactor if there's a custom control flow statement inside the loop.
      if (!CONTAINS_CUSTOM_CONTROL_FLOW.matches(loopTree.getStatement(), state)) {
        applyFix(tree, loopTree, state, fix);
      }
    }
  }

  /**
   * Applies the refactoring fix to the method: unwraps the loop body and adds the loop variable as
   * a method parameter.
   */
  private static void applyFix(
      MethodTree tree, EnhancedForLoopTree loopTree, VisitorState state, SuggestedFix.Builder fix) {
    // TODO(kak): we currently bail out if the loop contains any comments, but we could collect the
    // comments that would be deleted, and stick them somewhere before or after the fix (so they're
    // preserved even if the positioning is weird).
    if (state.getOffsetTokensForNode(loopTree).stream()
        .anyMatch(token -> !token.comments().isEmpty())) {
      return;
    }

    // Unwrap loop to extract the body
    StatementTree body = loopTree.getStatement();
    String bodySource =
        (body instanceof BlockTree blockTree)
            ? blockTree.getStatements().stream().map(state::getSourceForNode).collect(joining("\n"))
            : state.getSourceForNode(body);
    fix.replace(loopTree, bodySource.trim());

    // Add parameter to the method signature by scanning tokens between the return type and the
    // method body to find the opening and closing parentheses.
    int basePos = state.getEndPosition(tree.getReturnType());
    int endPos = getStartPosition(tree.getBody());
    ImmutableList<ErrorProneToken> methodTokens = state.getOffsetTokens(basePos, endPos);

    OptionalInt leftParen = OptionalInt.empty();
    OptionalInt rightParen = OptionalInt.empty();
    for (ErrorProneToken token : methodTokens) {
      if (token.kind() == TokenKind.LPAREN) {
        leftParen = OptionalInt.of(token.pos());
      } else if (token.kind() == TokenKind.RPAREN) {
        rightParen = OptionalInt.of(token.pos());
        break; // Found both, we can stop looking
      }
    }
    if (leftParen.isPresent() && rightParen.isPresent()) {
      String newMethodParam = getNewMethodParam(loopTree, state, fix);
      if (tree.getParameters().isEmpty()) {
        fix.replace(leftParen.getAsInt() + 1, rightParen.getAsInt(), newMethodParam);
      } else {
        fix.replace(rightParen.getAsInt(), rightParen.getAsInt(), ", " + newMethodParam);
      }
    }
  }

  /**
   * Generates the string representation of the new method parameter for the loop variable,
   * including the {@code @TestParameter} annotation.
   */
  private static String getNewMethodParam(
      EnhancedForLoopTree loopTree, VisitorState state, SuggestedFix.Builder fix) {
    VariableTree varTree = loopTree.getVariable();
    String varType = SuggestedFixes.qualifyType(state, fix, getType(varTree));
    String varName = varTree.getName().toString();
    String testParameter = SuggestedFixes.qualifyType(state, fix, TEST_PARAMETER_TYPE);
    return String.format("@%s %s %s", testParameter, varType, varName);
  }
}
