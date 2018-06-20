/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.dataflow.AccessPath;
import com.google.errorprone.dataflow.AccessPathStore;
import com.google.errorprone.dataflow.AccessPathValues;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import javax.annotation.Nullable;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

/**
 * Transfer function for {@link TrustingNullnessAnalysis}. It "trusts" annotations, meaning:
 *
 * <ul>
 *   <li>The parameters of the analyzed method are assumed non-null unless annotated {@code
 *       Nullable}.
 *   <li>Field reads and method calls are assumed to return non-null unless annotated.
 * </ul>
 *
 * <p>This transfer function also uses {@link Nullness#NONNULL} as its default, which means:
 *
 * <ul>
 *   <li>all array reads are assumed non-null. In the absence of Java 8 type annotations that
 *       matches what we'll do for the result of {@link List#get} etc. Will need to revisit with
 *       Java 8.
 *   <li>we'll assume non-null for local variables we don't have any information about. Since we
 *       seed {@link #initialStore} based on annotations on method parameters, the only known source
 *       of unknown locals would be "local variables" from outer scopes accessed in anonymous and
 *       local inner classes.
 *   <li>in the case of missing method or field symbols, non-null is assumed as well.
 * </ul>
 */
// TODO(b/71812955): Respect type annotations on arrays
// TODO(kmb): Use annotations on captured locals from outer scopes
class TrustingNullnessPropagation extends NullnessPropagationTransfer {

  private static final long serialVersionUID = -3128676755493202966L;

  TrustingNullnessPropagation() {
    super(Nullness.NONNULL, TrustReturnAnnotation.INSTANCE);
  }

  @Override
  public AccessPathStore<Nullness> initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    if (parameters == null) {
      // Documentation of this method states, "parameters is only set if the underlying AST is a
      // method"
      return AccessPathStore.empty();
    }
    AccessPathStore.Builder<Nullness> result = AccessPathStore.<Nullness>empty().toBuilder();
    for (LocalVariableNode param : parameters) {
      Nullness assumed = nullnessFromAnnotations((Symbol) param.getElement());
      result.setInformation(AccessPath.fromLocalVariable(param), assumed);
    }
    return result.build();
  }

  @Override
  Nullness fieldNullness(
      @Nullable ClassAndField accessed,
      @Nullable AccessPath path,
      AccessPathValues<Nullness> store) {
    if (accessed == null) {
      return Nullness.NONNULL; // optimistically assume non-null if we can't resolve
    }
    // In the absence of annotations or dataflow information, this will do the right thing for
    // things like primitives, array length, .class, etc.
    Nullness defaultValue = nullnessFromAnnotations(accessed.symbol);
    return store.valueOfAccessPath(path, defaultValue);
  }

  // TODO(b/79270313): consolidate hard-coded names of @Nullable annotations
  private static final ImmutableSet<String> NULLABLE_ANNOTATION_SIMPLE_NAMES =
      ImmutableSet.of("Nullable", "NullableDecl");

  /** Returns nullability based on the presence of a {@code Nullable} annotation. */
  static Nullness nullnessFromAnnotations(Symbol symbol) {
    if (MoreAnnotations.getDeclarationAndTypeAttributes(symbol)
        .anyMatch(
            c ->
                NULLABLE_ANNOTATION_SIMPLE_NAMES.contains(
                    c.getAnnotationType().asElement().getSimpleName().toString()))) {
      return Nullness.NULLABLE;
    }
    return Nullness.NONNULL;
  }

  private enum TrustReturnAnnotation implements Predicate<MethodInfo> {
    INSTANCE;

    /**
     * Returns {@code true} where {@link #nullnessFromAnnotations} would return {@link
     * Nullness#NONNULL}.
     */
    @Override
    public boolean apply(MethodInfo input) {
      for (String annotation : input.annotations()) {
        if (annotation.endsWith(".Nullable") || annotation.endsWith(".NullableDecl")) {
          return false;
        }
      }
      return true;
    }
  }
}
