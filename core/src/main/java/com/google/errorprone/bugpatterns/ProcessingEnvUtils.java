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

import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.stream.Stream;

/** Utilities for working with TypeMirrors in bug checkers. */
final class ProcessingEnvUtils {

  static final Supplier<Type> TYPE_MIRROR_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("javax.lang.model.type.TypeMirror"));
  private static final Supplier<Type> TYPES_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("javax.lang.model.util.Types"));
  private static final Supplier<Type> ELEMENTS_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("javax.lang.model.util.Elements"));
  private static final Supplier<Type> PROCESSING_ENV_TYPE =
      VisitorState.memoize(
          state -> state.getTypeFromString("javax.annotation.processing.ProcessingEnvironment"));

  private static Optional<String> findVariableInScope(VisitorState state, Type type) {
    if (type == null) {
      return Optional.empty();
    }
    return Streams.concat(
            Optional.ofNullable(ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class))
                .map(methodtree -> methodtree.getParameters().stream())
                .orElse(Stream.empty()),
            Optional.ofNullable(ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class))
                .map(
                    classTree ->
                        classTree.getMembers().stream()
                            .filter(member -> member instanceof VariableTree))
                .orElse(Stream.empty()))
        .filter(tree -> ASTHelpers.isSubtype(ASTHelpers.getType(tree), type, state))
        .findFirst()
        .map(tree -> getSymbol(tree).getSimpleName().toString());
  }

  static Optional<String> getElementsExpr(VisitorState state) {
    return findVariableInScope(state, ELEMENTS_TYPE.get(state))
        .or(
            () ->
                findVariableInScope(state, PROCESSING_ENV_TYPE.get(state))
                    .map(processingEnvVar -> processingEnvVar + ".getElementUtils()"));
  }

  static Optional<String> getTypesExpr(VisitorState state) {
    return findVariableInScope(state, TYPES_TYPE.get(state))
        .or(
            () ->
                findVariableInScope(state, PROCESSING_ENV_TYPE.get(state))
                    .map(processingEnvVar -> processingEnvVar + ".getTypeUtils()"));
  }

  private ProcessingEnvUtils() {}
}
