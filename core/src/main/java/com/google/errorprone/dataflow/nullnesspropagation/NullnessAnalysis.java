/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import com.google.errorprone.dataflow.DataFlow;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

/**
 * An interface to the nullness analysis.
 */
public final class NullnessAnalysis {

  /**
   * Represents a Java method.  Used for custom predicates to match non-null-returning methods.
   */
  public interface MethodInfo {
    String clazz();
    String method();
    boolean isStatic();
    boolean isPrimitive();
  }

  private final NullnessPropagationTransfer nullnessPropagation;

  /**
   * Initializes a nullness analysis with a built-in set of non-null returning methods.
   */
  public NullnessAnalysis() {
    nullnessPropagation = new NullnessPropagationTransfer();
  }

  /**
   * Initializes a nullness analysis with an additional set of non-null returning methods that
   * are or'ed with the built-in set of non-null returning methods.
   *
   * @param additionalNonNullReturningMethods A predicate matching methods that never return null
   */
  public NullnessAnalysis(Predicate<MethodInfo> additionalNonNullReturningMethods) {
    nullnessPropagation = new NullnessPropagationTransfer(additionalNonNullReturningMethods);
  }

  /**
   * Returns the {@link Nullness} of the leaf of {@code exprPath}.
   *
   * <p>If the leaf required the compiler to generate autoboxing or autounboxing calls,
   * {@code getNullness} returns the {@code Nullness} <i>after</i> the boxing/unboxing. This implies
   * that, in those cases, it will always return {@code NONNULL}.
   */
  public Nullness getNullness(TreePath exprPath, Context context) {
    return DataFlow.expressionDataflow(exprPath, context, nullnessPropagation);
  }
}
