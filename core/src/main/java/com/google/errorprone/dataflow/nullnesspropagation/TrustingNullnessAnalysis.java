/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.errorprone.dataflow.DataFlow;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * An interface to the "trusting" nullness analysis. This variant "trusts" {@code Nullabe}
 * annotations, similar to how a modular nullness checker like the checkerframework's would, meaning
 * method parameters, fields, and method returns are assumed {@link Nullness#NULLABLE} only if
 * annotated so.
 */
public final class TrustingNullnessAnalysis implements Serializable {

  private static final Context.Key<TrustingNullnessAnalysis> TRUSTING_NULLNESS_KEY =
      new Context.Key<>();

  /**
   * Retrieve an instance of {@link TrustingNullnessAnalysis} from the {@code context}. If there is
   * no {@link TrustingNullnessAnalysis} currently in the {@code context}, create one, insert it,
   * and return it.
   */
  public static TrustingNullnessAnalysis instance(Context context) {
    TrustingNullnessAnalysis instance = context.get(TRUSTING_NULLNESS_KEY);
    if (instance == null) {
      instance = new TrustingNullnessAnalysis();
      context.put(TRUSTING_NULLNESS_KEY, instance);
    }
    return instance;
  }

  private final TrustingNullnessPropagation nullnessPropagation = new TrustingNullnessPropagation();

  // Use #instance to instantiate
  private TrustingNullnessAnalysis() {}

  /**
   * Returns the {@link Nullness} of the leaf of {@code exprPath}.
   *
   * <p>If the leaf required the compiler to generate autoboxing or autounboxing calls, {@code
   * getNullness} returns the {@code Nullness} <i>after</i> the boxing/unboxing. This implies that,
   * in those cases, it will always return {@code NONNULL}.
   */
  public Nullness getNullness(TreePath exprPath, Context context) {
    try {
      nullnessPropagation.setContext(context).setCompilationUnit(exprPath.getCompilationUnit());
      return DataFlow.expressionDataflow(exprPath, context, nullnessPropagation);
    } finally {
      nullnessPropagation.setContext(null).setCompilationUnit(null);
    }
  }

  /**
   * Returns nullability based on the presence of a {@code Nullable} annotation.
   */
  public static Nullness nullnessFromAnnotations(Element element) {
    for (AnnotationMirror anno : element.getAnnotationMirrors()) {
      // Check for Nullable like ReturnValueIsNonNull
      if (anno.getAnnotationType().toString().endsWith(".Nullable")) {
        return Nullness.NULLABLE;
      }
    }
    return Nullness.NONNULL;
  }
}
