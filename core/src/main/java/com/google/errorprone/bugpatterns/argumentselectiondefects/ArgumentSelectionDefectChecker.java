/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.names.NamingConventions;
import com.google.errorprone.names.NeedlemanWunschEditDistance;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.function.Function;

/**
 * Checks the lexical distance between method parameter names and the argument names at call sites.
 * If another permutation of the arguments produces a lower distance then it is possible that the
 * programmer has accidentally reordered them.
 *
 * <p>TODO(andrewrice) add reference to paper here when we have a name
 *
 * <p>Terminology:
 *
 * <ul>
 *   <li>Formal parameter - as given in the definition of the method
 *   <li>Actual parameter - as used in the invocation of the method
 *   <li>Parameter - either a formal or actual parameter
 * </ul>
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@BugPattern(
  name = "ArgumentSelectionDefectChecker",
  summary = "Arguments are in the wrong order or could be commented for clarity.",
  explanation =
      "If permuting the arguments of a method call means that the argument names are a better "
          + "match for the parameter names than the original ordering then this might indicate "
          + "that they have been accidentally swapped.  There are also legitimate reasons for the "
          + "names not to match such as when rotating an image (swap width and height).  In this "
          + "case we suggest annotating the names with a comment to make the deliberate "
          + "swap clear to future readers of the code. Argument names annotated with a comment "
          + "containing the parameter name will not generate a warning.",
  category = JDK,
  severity = WARNING
)
public class ArgumentSelectionDefectChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private final ArgumentChangeFinder argumentchangeFinder;

  public ArgumentSelectionDefectChecker() {
    this(
        ArgumentChangeFinder.builder()
            .setDistanceFunction(buildDefaultDistanceFunction())
            .addHeuristic(new LowInformationNameHeuristic())
            .addHeuristic(new PenaltyThresholdHeuristic())
            .addHeuristic(new EnclosedByReverseHeuristic())
            .addHeuristic(new CreatesDuplicateCallHeuristic())
            .addHeuristic(new NameInCommentHeuristic())
            .build());
  }

  @VisibleForTesting
  ArgumentSelectionDefectChecker(ArgumentChangeFinder argumentChangeFinder) {
    this.argumentchangeFinder = argumentChangeFinder;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }

    // Don't return a match if the AssertEqualsArgumentOrderChecker would match it too
    if (Matchers.ASSERT_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return visitNewClassOrMethodInvocation(
        InvocationInfo.createFromMethodInvocation(tree, symbol, state));
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }

    // Don't return a match if the AutoValueConstructorOrderChecker would match it too
    if (Matchers.AUTOVALUE_CONSTRUCTOR.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return visitNewClassOrMethodInvocation(InvocationInfo.createFromNewClass(tree, symbol, state));
  }

  private Description visitNewClassOrMethodInvocation(InvocationInfo invocationInfo) {

    Changes changes = argumentchangeFinder.findChanges(invocationInfo);

    if (changes.isEmpty()) {
      return Description.NO_MATCH;
    }

    // Fix1: permute the arguments as required
    SuggestedFix permuteArgumentsFix = changes.buildPermuteArgumentsFix(invocationInfo);

    // Fix2: apply comments with parameter names to potentially-swapped arguments of the method
    SuggestedFix commentArgumentsFix = changes.buildCommentArgumentsFix(invocationInfo);

    return buildDescription(invocationInfo.tree())
        .addFix(permuteArgumentsFix)
        .addFix(commentArgumentsFix)
        .build();
  }

  /**
   * Computes the distance between a formal and actual parameter. If either is a null literal then
   * the distance is zero (null matches everything). If both have a name then we compute the
   * normalised NeedlemanWunschEditDistance. Otherwise, one of the names is unknown and so we return
   * 0 distance between it and its original parameter and infinite distance between all others.
   */
  private static final Function<ParameterPair, Double> buildDefaultDistanceFunction() {
    return new Function<ParameterPair, Double>() {
      @Override
      public Double apply(ParameterPair pair) {
        if (pair.formal().isNullLiteral() || pair.actual().isNullLiteral()) {
          return 0.0;
        }

        if (!pair.formal().isUnknownName() && !pair.actual().isUnknownName()) {
          String normalizedSource =
              NamingConventions.convertToLowerUnderscore(pair.formal().name());
          String normalizedTarget =
              NamingConventions.convertToLowerUnderscore(pair.actual().name());
          return NeedlemanWunschEditDistance.getNormalizedEditDistance(
              /*source=*/ normalizedSource,
              /*target=*/ normalizedTarget,
              /*caseSensitive=*/ false,
              /*changeCost=*/ 8,
              /*openGapCost=*/ 8,
              /*continueGapCost=*/ 1);
        }

        return pair.formal().index() == pair.actual().index() ? 0.0 : Double.POSITIVE_INFINITY;
      }
    };
  }
}
