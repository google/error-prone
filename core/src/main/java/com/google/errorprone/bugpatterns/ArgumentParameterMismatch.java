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
package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checks the lexical similarity between method parameter names and the argument names at call
 * sites. If there exists an identifier in scope that is a subtype of the parameter type and whose
 * name is significantly more similar to the parameter name than the existing argument, suggests
 * replacing it.
 *
 * <p>This is an implementation of the argument recommendation analysis from Liu et al., "Nomen est
 * Omen: Exploring and Exploiting Similarities between Argument and Parameter Names," ICSE 2016.
 */
@BugPattern(
  name = "ArgumentParameterMismatch",
  summary =
      "A different potential argument is more similar to the name of the parameter than the "
          + "existing argument; this may be an error",
  category = JDK,
  severity = WARNING
)
public class ArgumentParameterMismatch extends AbstractArgumentParameterChecker {

  private static final ImmutableSet<Kind> VALID_KINDS = ImmutableSet.of(Kind.IDENTIFIER);

  public ArgumentParameterMismatch() {
    this(ImmutableSet.of("index", "item", "key", "value"), 0.667);
  }

  public ArgumentParameterMismatch(ImmutableSet<String> ignoreParams, double beta) {
    super(
        POTENTIAL_REPLACEMENTS_FUNCTION,
        new ParameterPredicate(ignoreParams, 4),
        beta,
        ArgumentParameterSimilarityMetrics::computeNormalizedTermIntersection,
        VALID_KINDS);
  }

  /**
   * Returns a set of potential replacements consisting of all the bare identifiers that are in
   * scope at the leaf node of the {@link TreePath} in the given {@code state}.
   */
  private static final Function<VisitorState, ImmutableSet<PotentialReplacement>>
      POTENTIAL_REPLACEMENTS_FUNCTION =
          state -> {
            return ImmutableSet.copyOf(
                FindIdentifiers.findAllIdents(state)
                    .stream()
                    .map(ArgumentParameterMismatch::potentialReplacement)
                    .collect(Collectors.toSet()));
          };

  private static PotentialReplacement potentialReplacement(VarSymbol sym) {
    return PotentialReplacement.create(
        sym.getSimpleName().toString(), sym.getSimpleName().toString(), sym);
  }
}
