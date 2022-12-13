/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.common.collect.ImmutableMap;
import java.util.Set;

/**
 * An object that can report on the behavior of a CRV-related check for analysis purposes.
 *
 * @param <E> the type of an expression node in the AST
 * @param <C> the type of the context object used during AST analysis
 */
public interface ResultUsePolicyAnalyzer<E, C> {
  /** The canonical name of the check. */
  String canonicalName();

  /**
   * Returns all of the name strings that this checker should respect as part of a
   * {@code @SuppressWarnings} annotation.
   */
  Set<String> allNames();

  /**
   * Returns whether this checker makes any determination about whether the given expression's
   * return value should be used or not. Most checkers either determine that an expression is CRV or
   * make no determination.
   */
  boolean isCovered(E expression, C context);

  /** Returns the {@link ResultUsePolicy} for the method used in the given {@code expression}. */
  ResultUsePolicy getMethodPolicy(E expression, C context);

  /** Returns a map of optional metadata about why this check matched the given expression. */
  default ImmutableMap<String, ? extends Object> getMatchMetadata(E expression, C context) {
    return ImmutableMap.of();
  }
}
