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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * This checks the similarity between the arguments and parameters. This version calculates the
 * similarity between argument and parameter names and recommends a different order of the arguments
 * if the similarity is higher.
 *
 * @author yulissa@google.com (Yulissa Arroyo-Paredes)
 */
@BugPattern(
  name = "ArgumentParameterSwap",
  summary =
      "An argument is more similar to a different parameter; the arguments may have been swapped.",
  category = JDK,
  severity = ERROR
)
public class ArgumentParameterSwap extends AbstractArgumentParameterChecker {

  private static final ImmutableSet<Kind> VALID_KINDS =
      ImmutableSet.of(Kind.MEMBER_SELECT, Kind.IDENTIFIER, Kind.METHOD_INVOCATION);

  public ArgumentParameterSwap() {
    this(ImmutableSet.of("index", "item", "key", "value"), 0.667);
  }

  /**
   * Constructor for overriding default behaviour.
   *
   * @param ignoreParams identifies (formal) parameters which should be ignored. Arguments to these
   *     parameters will not be changed. Use this to exclude commonly overloaded variable names such
   *     as index
   * @param swapHandicap gives the amount by which the similarity of an accepted replacement must
   *     beat the similarity of the current parameter. A swapHandicap of 0.5 means that the
   *     similarity score of the replacement must be 0.5 more than that of the current parameter in
   *     order to be swapped in
   */
  public ArgumentParameterSwap(ImmutableSet<String> ignoreParams, double swapHandicap) {
    super(
        REPLACEMENTS_DRAWN_FROM_ARGS,
        new ParameterPredicate(ignoreParams, 4),
        swapHandicap,
        ArgumentParameterSimilarityMetrics::computeNormalizedTermIntersection,
        VALID_KINDS);
  }

  /**
   * Returns a set of potential replacements consisting of all arguments to the method call that are
   * of valid kinds.
   */
  private static final Function<VisitorState, ImmutableSet<PotentialReplacement>>
      REPLACEMENTS_DRAWN_FROM_ARGS =
          state -> {
            Tree parentNode = state.getPath().getParentPath().getLeaf();
            List<? extends ExpressionTree> args;
            switch (parentNode.getKind()) {
              case METHOD_INVOCATION:
                args = ((MethodInvocationTree) parentNode).getArguments();
                break;
              case NEW_CLASS:
                args = ((NewClassTree) parentNode).getArguments();
                break;
              default:
                return ImmutableSet.of();
            }
            return args.stream()
                .filter(expr -> VALID_KINDS.contains(expr.getKind()))
                .map(ArgumentParameterSwap::potentialReplacement)
                .filter(s -> s != null)
                .collect(toImmutableSet());
          };

  @Nullable
  private static PotentialReplacement potentialReplacement(ExpressionTree expr) {
    String extractedArgumentName = extractArgumentName(expr);
    return (extractedArgumentName == null)
        ? null
        : PotentialReplacement.create(
            extractedArgumentName, expr.toString(), ASTHelpers.getSymbol(expr));
  }
}
