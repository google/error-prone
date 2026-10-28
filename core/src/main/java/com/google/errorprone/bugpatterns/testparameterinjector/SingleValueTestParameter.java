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

package com.google.errorprone.bugpatterns.testparameterinjector;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.BOOLEAN_VALUES;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.REPEATED_TEST_PARAMETERS;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.TEST_PARAMETERS;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.TEST_PARAMETER_TYPE;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.getExplicitTestParameterValues;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isEnumType;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_TEST_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.ignoreParens;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.unboxedTypeOrType;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        """
        "A test parameter with only a single value can be a local variable in the test method. \
        You may prefer to leave it as it is, if you expect there will be further values later, or \
        if you want to preserve a format similar to other test methods in this class.\
        """,
    severity = SUGGESTION)
public final class SingleValueTestParameter extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<AnnotationTree> HAS_STRING_LITERAL_VALUE =
      hasArgumentWithValue("value", ignoreParens(kindIs(STRING_LITERAL)));

  // TODO(kak): consider replacing @RunWith(TestParameterInjector.class) with @RunWith(JUnit4.class)
  // if inlining removes the last parameterized test, field, or constructor in the test class.
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    BlockTree body = methodTree.getBody();
    List<? extends VariableTree> params = methodTree.getParameters();
    MethodSymbol methodSymbol = getSymbol(methodTree);
    // Note: We don't need to match single-value @TestParameters annotations here, because
    // SingleTestParameter rewrites single-parameter @TestParameters to @TestParameter, which this
    // check will then match.
    if (params.isEmpty()
        || body == null
        || body.getStatements().isEmpty()
        || TEST_PARAMETER_TYPE.get(state) == null
        || !hasAnnotation(methodSymbol, JUNIT4_TEST_ANNOTATION, state)
        || hasAnnotation(methodSymbol, TEST_PARAMETERS, state)
        || hasAnnotation(methodSymbol, REPEATED_TEST_PARAMETERS, state)
        || !findSuperMethods(methodSymbol, state.getTypes()).isEmpty()) {
      return NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<String> keptParams = new ArrayList<>();
    List<String> localDeclarations = new ArrayList<>();

    for (VariableTree param : params) {
      Optional<String> localDeclaration = toLocalDeclaration(param, fix, state);
      if (localDeclaration.isPresent()) {
        localDeclarations.add(localDeclaration.get());
      } else {
        keptParams.add(state.getSourceForNode(param));
      }
    }
    if (localDeclarations.isEmpty()) {
      return NO_MATCH;
    }

    // Note: We intentionally don't check whether removing parameters creates an overload
    // collision (e.g., having both `testFoo(@TestParameter ...)` and `testFoo()`) or breaks
    // direct calls/method references to the @Test method, as both are rare in practice.
    fix.replace(
        getStartPosition(params.getFirst()),
        state.getEndPosition(params.getLast()),
        String.join(", ", keptParams));
    fix.prefixWith(body.getStatements().getFirst(), String.join("\n", localDeclarations) + "\n");
    return describeMatch(methodTree, fix.build());
  }

  private static Optional<String> toLocalDeclaration(
      VariableTree param, SuggestedFix.Builder fix, VisitorState state) {
    // TODO(kak): add support for @Nullable annotations on parameters.
    if (param.getModifiers().getAnnotations().size() != 1) {
      return Optional.empty();
    }
    AnnotationTree testParamAnno = getOnlyElement(param.getModifiers().getAnnotations());
    ImmutableList<String> explicitValues = getExplicitTestParameterValues(testParamAnno, state);
    Type paramType = getType(param);
    String typeSource = state.getSourceForNode(param.getType());
    // TODO(kak): support constant expressions (e.g., @TestParameter(MY_CONST) or
    // @TestParameter({"" + MY_INT_CONST})) by preserving the symbolic constant reference.
    if (explicitValues.size() != 1
        || !HAS_STRING_LITERAL_VALUE.matches(testParamAnno, state)
        || paramType == null
        || typeSource == null) {
      return Optional.empty();
    }
    return testParameterValueToJavaExpression(getOnlyElement(explicitValues), paramType, fix, state)
        .map(init -> String.format("%s %s = %s;", typeSource, param.getName(), init));
  }

  private static Optional<String> testParameterValueToJavaExpression(
      String rawValue, Type paramType, SuggestedFix.Builder fix, VisitorState state) {
    if (rawValue.equals("null")) {
      return paramType.isPrimitive() ? Optional.empty() : Optional.of("null");
    }
    if (isSameType(paramType, state.getSymtab().stringType, state)) {
      return Optional.of(state.getConstantExpression(rawValue));
    }
    if (isEnumType(paramType)) {
      return enumValues(paramType.tsym).contains(rawValue)
          ? Optional.of(qualifyType(state, fix, paramType.tsym) + "." + rawValue)
          : Optional.empty();
    }
    return switch (unboxedTypeOrType(paramType, state).getKind()) {
      case BOOLEAN -> Optional.of(Ascii.toLowerCase(rawValue)).filter(BOOLEAN_VALUES::contains);
      case INT -> Optional.ofNullable(Ints.tryParse(rawValue)).map(state::getConstantExpression);
      case LONG -> Optional.ofNullable(Longs.tryParse(rawValue)).map(state::getConstantExpression);
      case FLOAT ->
          Optional.ofNullable(Floats.tryParse(rawValue))
              .filter(Float::isFinite)
              .map(state::getConstantExpression);
      case DOUBLE ->
          Optional.ofNullable(Doubles.tryParse(rawValue))
              .filter(Double::isFinite)
              .map(state::getConstantExpression);
      default -> Optional.empty();
    };
  }
}
