/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.JUnitMatchers.wouldRunInJUnit4;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks if the methods specified in {@code junitparams.Parameters} annotation to provide
 * parameters exists.
 *
 * <p>This checks for the required method in the current class and all the base classes. In case the
 * required method is present in a superclass, this check would generate a false positive.
 */
@BugPattern(
    name = "JUnitParameterMethodNotFound",
    summary = "The method for providing parameters was not found.",
    severity = ERROR)
public class JUnitParameterMethodNotFound extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<AnnotationTree> PARAMETERS_ANNOTATION_MATCHER =
      Matchers.isSameType("junitparams.Parameters");

  private static final MultiMatcher<ClassTree, AnnotationTree> PARAMETERIZED_TEST_RUNNER =
      Matchers.annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue(
              /* argumentName= */ "value",
              isJUnit4TestRunnerOfType(ImmutableSet.of("junitparams.JUnitParamsRunner"))));

  private static final Matcher<MethodTree> ENCLOSING_CLASS_PARAMETERIZED_TEST_RUNNER_MATCHER =
      Matchers.enclosingClass(PARAMETERIZED_TEST_RUNNER);

  private static final Matcher<MethodTree> POSSIBLE_PARAMETERIZED_TEST_METHOD_MATCHER =
      allOf(ENCLOSING_CLASS_PARAMETERIZED_TEST_RUNNER_MATCHER, wouldRunInJUnit4);

  private static final String JUNIT_PARAMETER_METHOD_PREFIX = "parametersFor";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!POSSIBLE_PARAMETERIZED_TEST_METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Optional<? extends AnnotationTree> parametersAnnotation =
        tree.getModifiers().getAnnotations().stream()
            .filter(annotationTree -> PARAMETERS_ANNOTATION_MATCHER.matches(annotationTree, state))
            .findFirst();

    if (!parametersAnnotation.isPresent()) {
      return Description.NO_MATCH;
    }

    Set<String> methodsInSourceClass = ImmutableSet.of();
    Set<String> requiredMethods = new TreeSet<>();

    ImmutableList<? extends AssignmentTree> annotationsArguments =
        parametersAnnotation.get().getArguments().stream()
            .filter(expressionTree -> expressionTree.getKind() == Kind.ASSIGNMENT)
            .map(expressionTree -> (AssignmentTree) expressionTree)
            .collect(toImmutableList());

    if (annotationsArguments.isEmpty()) {
      requiredMethods.add(JUNIT_PARAMETER_METHOD_PREFIX + toPascalCase(tree.getName().toString()));
    } else {
      Optional<? extends AssignmentTree> paramMethodAssignmentTree =
          getParamAssignmentTree(annotationsArguments, /* parameterName= */ "method");

      if (paramMethodAssignmentTree.isPresent()) {
        String paramMethods =
            (String) ASTHelpers.constValue(paramMethodAssignmentTree.get().getExpression());
        Splitter.on(',').trimResults().splitToStream(paramMethods).forEach(requiredMethods::add);

        // If source argument is present in the annotation the method should be searched in the
        // class specified by the argument.
        methodsInSourceClass = getMethodIdentifiersInSourceAnnotation(annotationsArguments, state);
      }
    }

    if (methodsInSourceClass.isEmpty()) {
      methodsInSourceClass = getAllMethodIdentifiersForType(getClassType(tree), state);
    }

    Set<String> missingMethods = Sets.difference(requiredMethods, methodsInSourceClass);

    if (missingMethods.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(String.format("%s method(s) not found", String.join(",", missingMethods)))
        .build();
  }

  private static Type getClassType(MethodTree tree) {
    return ASTHelpers.enclosingClass(ASTHelpers.getSymbol(tree)).type;
  }

  private static ImmutableSet<String> getMethodIdentifiersInSourceAnnotation(
      ImmutableList<? extends AssignmentTree> annotationsArguments, VisitorState state) {
    Optional<? extends AssignmentTree> paramSourceAssignmentTree =
        getParamAssignmentTree(annotationsArguments, /* parameterName= */ "source");

    if (!paramSourceAssignmentTree.isPresent()) {
      return ImmutableSet.of();
    }

    ClassType classType = (ClassType) getType(paramSourceAssignmentTree.get().getExpression());
    Type typeArgument = classType.getTypeArguments().get(0);
    return getAllMethodIdentifiersForType(typeArgument, state);
  }

  private static Optional<? extends AssignmentTree> getParamAssignmentTree(
      ImmutableList<? extends AssignmentTree> annotationsArguments, String parameterName) {
    return annotationsArguments.stream()
        .filter(
            assignmentTree ->
                ((IdentifierTree) assignmentTree.getVariable())
                    .getName()
                    .contentEquals(parameterName))
        .findFirst();
  }

  private static ImmutableSet<String> getAllMethodIdentifiersForType(
      Type type, VisitorState state) {
    // As we require only the method identifier set for the given type, the value of skipInterface
    // argument would not affect the final computed set.
    return stream(state.getTypes().membersClosure(type, /* skipInterface= */ false).getSymbols())
        .filter(MethodSymbol.class::isInstance)
        .map(methodSymbol -> methodSymbol.getSimpleName().toString())
        .collect(toImmutableSet());
  }

  private static String toPascalCase(String methodName) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, methodName);
  }
}
