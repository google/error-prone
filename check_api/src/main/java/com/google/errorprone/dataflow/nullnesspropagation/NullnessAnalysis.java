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

import com.google.errorprone.dataflow.DataFlow;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;

/** An interface to the nullness analysis. */
public final class NullnessAnalysis implements Serializable {

  private static final Context.Key<NullnessAnalysis> NULLNESS_ANALYSIS_KEY = new Context.Key<>();

  private final NullnessPropagationTransfer nullnessPropagation;

  /**
   * Retrieve an instance of {@link NullnessAnalysis} from the {@code context}. If there is no
   * {@link NullnessAnalysis} currently in the {@code context}, create one, insert it, and return
   * it.
   */
  public static NullnessAnalysis instance(Context context) {
    NullnessAnalysis instance = context.get(NULLNESS_ANALYSIS_KEY);
    if (instance == null) {
      instance = new NullnessAnalysis();
      context.put(NULLNESS_ANALYSIS_KEY, instance);
    }
    return instance;
  }

  private NullnessAnalysis() {
    nullnessPropagation = new NullnessPropagationTransfer();
  }

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
}
