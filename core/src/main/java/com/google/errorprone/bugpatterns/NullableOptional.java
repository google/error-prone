/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;

/**
 * A Checker that catches {@link java.util.Optional}/{@link com.google.common.base.Optional} with
 * {@code Nullable} annotation.
 */
@BugPattern(
    summary =
        "Using an Optional variable which is expected to possibly be null is discouraged. It is"
            + " best to indicate the absence of the value by assigning it an empty optional.",
    severity = SeverityLevel.WARNING)
public final class NullableOptional extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {
  private static final TypePredicate IS_OPTIONAL_TYPE =
      TypePredicates.isExactTypeAny(
          ImmutableSet.of(
              java.util.Optional.class.getCanonicalName(),
              com.google.common.base.Optional.class.getCanonicalName()));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (hasNullableAnnotation(tree.getModifiers())
        && isOptional(ASTHelpers.getType(tree.getReturnType()), state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (hasNullableAnnotation(tree.getModifiers()) && isOptional(ASTHelpers.getType(tree), state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  /** Check if the input ModifiersTree has any kind of "Nullable" annotation. */
  private static boolean hasNullableAnnotation(ModifiersTree modifiersTree) {
    return ASTHelpers.getAnnotationWithSimpleName(modifiersTree.getAnnotations(), "Nullable")
        != null;
  }

  /**
   * Check if the input Type is either {@link java.util.Optional} or{@link
   * com.google.common.base.Optional}.
   */
  private static boolean isOptional(Type type, VisitorState state) {
    return IS_OPTIONAL_TYPE.apply(type, state);
  }
}
