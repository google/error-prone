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
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getAnnotationMirror;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.unboxedTypeOrType;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.jspecify.annotations.Nullable;

/** Shared constants and utility methods for {@code TestParameterInjector} checks. */
final class TestParameterInjectorUtils {

  /** Fully qualified name of {@code TestParameterInjector}. */
  static final String TEST_PARAMETER_INJECTOR =
      "com.google.testing.junit.testparameterinjector.TestParameterInjector";

  /** Fully qualified name of {@code @TestParameter}. */
  static final String TEST_PARAMETER =
      "com.google.testing.junit.testparameterinjector.TestParameter";

  /** Fully qualified name of {@code @TestParameters}. */
  static final String TEST_PARAMETERS =
      "com.google.testing.junit.testparameterinjector.TestParameters";

  /** Supplies the {@code @TestParameter} annotation {@link Type}. */
  static final Supplier<Type> TEST_PARAMETER_TYPE = typeFromString(TEST_PARAMETER);

  /** Supplies the {@code @TestParameters} annotation {@link Type}. */
  static final Supplier<Type> TEST_PARAMETERS_TYPE = typeFromString(TEST_PARAMETERS);

  /** Lowercase string representations of both boolean values. */
  static final ImmutableSet<String> BOOLEAN_VALUES = ImmutableSet.of("true", "false");

  /** Returns {@code true} if {@code type} is primitive {@code boolean} or boxed {@code Boolean}. */
  static boolean isBooleanType(@Nullable Type type, VisitorState state) {
    return type != null
        && isSameType(unboxedTypeOrType(type, state), state.getSymtab().booleanType, state);
  }

  /** Returns {@code true} if {@code type} is an enum type. */
  static boolean isEnumType(@Nullable Type type) {
    return type != null && type.tsym != null && type.tsym.isEnum();
  }

  /**
   * Returns {@code true} if {@code values} contains {@code "true"} and {@code "false"}
   * (case-insensitively, in either order) and nothing else.
   */
  static boolean isExhaustiveBooleanValues(List<String> values) {
    return values.size() == 2
        && values.stream().map(Ascii::toLowerCase).collect(toImmutableSet()).equals(BOOLEAN_VALUES);
  }

  /**
   * Returns the explicit string values if {@code tree} is a {@code @TestParameter} annotation with
   * only the {@code value} attribute specified. Returns an empty list if no arguments are present,
   * or if other attributes (like {@code valuesProvider}) are present.
   */
  static ImmutableList<String> getExplicitTestParameterValues(
      AnnotationTree tree, VisitorState state) {
    if (tree.getArguments().isEmpty()) {
      return ImmutableList.of();
    }
    if (!isSameType(getType(tree), TEST_PARAMETER_TYPE.get(state), state)) {
      return ImmutableList.of();
    }
    AnnotationMirror attribute = getAnnotationMirror(tree);
    if (attribute == null || attribute.getElementValues().size() != 1) {
      return ImmutableList.of();
    }
    return MoreAnnotations.getStrings(attribute, "value");
  }

  /** Returns the replacement source for an implicit {@code @TestParameter} without arguments. */
  static String implicitTestParameterAnnotation(AnnotationTree tree, VisitorState state) {
    return "@" + state.getSourceForNode(tree.getAnnotationType());
  }

  private TestParameterInjectorUtils() {}
}
