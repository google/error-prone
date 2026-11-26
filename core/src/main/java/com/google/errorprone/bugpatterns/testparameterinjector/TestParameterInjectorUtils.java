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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getAnnotationMirror;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.unboxedTypeOrType;
import static com.google.errorprone.util.MoreAnnotations.getStrings;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.jspecify.annotations.Nullable;

/// Shared constants and utility methods for `TestParameterInjector` checks.
final class TestParameterInjectorUtils {

  /// `TestParameterInjector` class name.
  static final ClassName TEST_PARAMETER_INJECTOR =
      ClassName.of("com.google.testing.junit.testparameterinjector.TestParameterInjector");

  /// `@TestParameter` annotation class name.
  static final ClassName TEST_PARAMETER =
      ClassName.of("com.google.testing.junit.testparameterinjector.TestParameter");

  /// `@TestParameters` annotation class name.
  static final ClassName TEST_PARAMETERS =
      ClassName.of("com.google.testing.junit.testparameterinjector.TestParameters");

  /// `@TestParameters.RepeatedTestParameters` annotation class name.
  static final ClassName REPEATED_TEST_PARAMETERS =
      ClassName.of(
          "com.google.testing.junit.testparameterinjector.TestParameters.RepeatedTestParameters");

  /// Lowercase string representations of both boolean values.
  static final ImmutableSet<String> BOOLEAN_VALUES = ImmutableSet.of("true", "false");

  /// Returns `true` if `type` is primitive `boolean` or boxed `Boolean`.
  static boolean isBooleanType(@Nullable Type type, VisitorState state) {
    return type != null
        && isSameType(unboxedTypeOrType(type, state), state.getSymtab().booleanType, state);
  }

  /// Returns `true` if `type` is an enum type.
  static boolean isEnumType(@Nullable Type type) {
    return type != null && type.tsym != null && type.tsym.isEnum();
  }

  /// Returns `true` if `values` contains `"true"` and `"false"` (case-insensitively, in either
  /// order) and nothing else.
  static boolean isExhaustiveBooleanValues(List<String> values) {
    return values.size() == 2
        && values.stream().map(Ascii::toLowerCase).collect(toImmutableSet()).equals(BOOLEAN_VALUES);
  }

  /// Returns `true` if `values` exhaustively covers all values of `type` (for a `boolean`/`Boolean`
  /// or `Enum` type).
  static boolean isExhaustiveValues(@Nullable Type type, List<String> values, VisitorState state) {
    if (isBooleanType(type, state)) {
      return isExhaustiveBooleanValues(values);
    }
    if (isEnumType(type)) {
      Set<String> constants = enumValues(type.tsym);
      return values.size() == constants.size() && ImmutableSet.copyOf(values).equals(constants);
    }
    return false;
  }

  /// Extracted `value` strings and annotation trees from one or more `@TestParameters` annotations
  /// on a method.
  record TestParametersAnnotations(
      ImmutableList<String> values, ImmutableList<AnnotationTree> annotationsToRemove) {}

  /// Extracts all `value` strings and annotation trees from `@TestParameters` annotations on
  /// `methodTree`. Returns [Optional#empty()] if there are none, if any
  /// `@TestParameters.RepeatedTestParameters` annotation is present, or if any `@TestParameters`
  /// annotation specifies attributes other than `value` (such as `customName`).
  static Optional<TestParametersAnnotations> extractTestParametersAnnotations(
      MethodTree methodTree, VisitorState state) {
    ImmutableList.Builder<String> values = ImmutableList.builder();
    ImmutableList.Builder<AnnotationTree> annotationsToRemove = ImmutableList.builder();
    for (AnnotationTree annotationTree : methodTree.getModifiers().getAnnotations()) {
      AnnotationMirror attribute = getAnnotationMirror(annotationTree);
      if (attribute == null) {
        continue;
      }
      Type annotationType = (Type) attribute.getAnnotationType();
      if (isSameType(annotationType, REPEATED_TEST_PARAMETERS, state)) {
        return Optional.empty();
      }
      if (isSameType(annotationType, TEST_PARAMETERS, state)) {
        if (attribute.getElementValues().size() != 1
            || !getOnlyElement(attribute.getElementValues().keySet())
                .getSimpleName()
                .contentEquals("value")) {
          return Optional.empty();
        }
        values.addAll(getStrings(attribute, "value"));
        annotationsToRemove.add(annotationTree);
      }
    }
    ImmutableList<AnnotationTree> toRemove = annotationsToRemove.build();
    return toRemove.isEmpty()
        ? Optional.empty()
        : Optional.of(new TestParametersAnnotations(values.build(), toRemove));
  }

  /// Returns the explicit string values if `tree` is a `@TestParameter` annotation with only the
  /// `value` attribute specified. Returns an empty list if no arguments are present, or if other
  /// attributes (like `valuesProvider`) are present.
  static ImmutableList<String> getExplicitTestParameterValues(
      AnnotationTree tree, VisitorState state) {
    if (tree.getArguments().isEmpty()) {
      return ImmutableList.of();
    }
    if (!isSameType(getType(tree), TEST_PARAMETER, state)) {
      return ImmutableList.of();
    }
    AnnotationMirror attribute = getAnnotationMirror(tree);
    if (attribute == null || attribute.getElementValues().size() != 1) {
      return ImmutableList.of();
    }
    return MoreAnnotations.getStrings(attribute, "value");
  }

  /// Returns the replacement source for an implicit `@TestParameter` without arguments.
  static String implicitTestParameterAnnotation(AnnotationTree tree, VisitorState state) {
    return "@" + state.getSourceForNode(tree.getAnnotationType());
  }

  private TestParameterInjectorUtils() {}
}
