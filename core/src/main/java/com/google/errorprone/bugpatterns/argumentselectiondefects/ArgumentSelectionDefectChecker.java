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

import com.google.common.collect.ImmutableList;
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
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
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
  summary = "Arguments to this method call may be in the wrong order",
  explanation =
      "If permuting the arguments of a method call means that the argument names are a better "
          + "match for the parameter names than the original ordering then this might indicate "
          + "that they have been accidentally swapped.  There are also legitimate reasons for the "
          + "names not to match such as when rotating an image (swap width and height).  In this "
          + "case we would recommend annotating the names with a comment to make the deliberate "
          + "swap clear to future readers of the code. Argument names annotated with a comment "
          + "containing the parameter name will not generate a warning.",
  category = JDK,
  severity = WARNING
)
public class ArgumentSelectionDefectChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private final ImmutableList<Heuristic> heuristics;
  private final Function<ParameterPair, Double> nameDistanceFunction;

  public ArgumentSelectionDefectChecker() {
    this(
        buildDefaultDistanceFunction(),
        ImmutableList.of(
            new LowInformationNameHeuristic(),
            new PenaltyThresholdHeuristic(),
            new EnclosedByReverseHeuristic(),
            new CreatesDuplicateCallHeuristic(),
            new NameInCommentHeuristic()));
  }

  public ArgumentSelectionDefectChecker(
      Function<ParameterPair, Double> nameDistanceFunction, ImmutableList<Heuristic> heuristics) {
    this.nameDistanceFunction = nameDistanceFunction;
    this.heuristics = heuristics;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }
    return visitNewClassOrMethodInvocation(tree, symbol, tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }
    return visitNewClassOrMethodInvocation(tree, symbol, tree.getArguments(), state);
  }

  private Description visitNewClassOrMethodInvocation(
      Tree invokedMethodTree,
      MethodSymbol invokedMethodSymbol,
      List<? extends ExpressionTree> actualParameters,
      VisitorState state) {

    Changes changes = findChanges(invokedMethodTree, invokedMethodSymbol, actualParameters, state);

    if (changes.isEmpty()) {
      return Description.NO_MATCH;
    }

    // Fix1: permute the arguments as required
    SuggestedFix permuteArgumentsFix = buildPermuteArgumentsFix(changes, actualParameters, state);

    // Fix2: apply comments with parameter names to potentially-swapped arguments of the method
    SuggestedFix commentArgumentsFix =
        buildCommentArgumentsFix(changes, invokedMethodSymbol, actualParameters);

    return buildDescription(invokedMethodTree)
        .addFix(permuteArgumentsFix)
        .addFix(commentArgumentsFix)
        .build();
  }

  private Changes findChanges(
      Tree invokedMethodTree,
      MethodSymbol invokedMethodSymbol,
      List<? extends ExpressionTree> actualParameters,
      VisitorState state) {

    List<VarSymbol> formalParameters = getFormalParametersWithoutVarArgs(invokedMethodSymbol);

    /* Methods with one or fewer parameters cannot possibly have a swap */
    if (formalParameters.size() <= 1) {
      return Changes.empty();
    }

    /* Sometimes we don't have enough actual parameters. This seems to happen sometimes with calls
     * to super and javac finds two parameters arg0 and arg1 and no arguments */
    if (actualParameters.size() < formalParameters.size()) {
      return Changes.empty();
    }

    ImmutableList<Parameter> formals = Parameter.createListFromVarSymbols(formalParameters);
    ImmutableList<Parameter> actuals =
        Parameter.createListFromExpressionTrees(
            actualParameters.subList(0, formalParameters.size()));

    Costs costs = new Costs(formals, actuals);

    /* Set the distance between a pair to Inf if not assignable */
    costs
        .viablePairs()
        .filter(ParameterPair::isAlternativePairing)
        .filter(p -> !p.actual().isAssignableTo(p.formal(), state))
        .forEach(p -> costs.invalidatePair(p));

    /* If there are no formal parameters which are assignable to any alternative actual parameters
    then we can stop without trying to look for permutations */
    if (costs.viablePairs().noneMatch(ParameterPair::isAlternativePairing)) {
      return Changes.empty();
    }

    /* Set the lexical distance between pairs */
    costs.viablePairs().forEach(p -> costs.updatePair(p, nameDistanceFunction.apply(p)));

    Changes changes = costs.computeAssignments();

    if (changes.isEmpty()) {
      return changes;
    }

    /* Only keep this change if all of the heuristics match */
    for (Heuristic heuristic : heuristics) {
      if (!heuristic.isAcceptableChange(changes, invokedMethodTree, invokedMethodSymbol, state)) {
        return Changes.empty();
      }
    }

    return changes;
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

        if (pair.formal().isNamed() && pair.actual().isNamed()) {
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

  private static List<VarSymbol> getFormalParametersWithoutVarArgs(
      MethodSymbol invokedMethodSymbol) {
    List<VarSymbol> formalParameters = invokedMethodSymbol.getParameters();

    /* javac can get argument names from debugging symbols if they are not available from
    other sources. When it does this for an inner class sometimes it returns the implicit this
    pointer for the outer class as the first name (but not the first type). If we see this, then
    just abort */
    if (!formalParameters.isEmpty()
        && formalParameters.get(0).getSimpleName().toString().matches("this\\$[0-9]+")) {
      return ImmutableList.of();
    }

    /* If we have a varargs method then just ignore the final parameter and trailing actual
    parameters */
    int size =
        invokedMethodSymbol.isVarArgs() ? formalParameters.size() - 1 : formalParameters.size();

    return formalParameters.subList(0, size);
  }

  private static SuggestedFix buildCommentArgumentsFix(
      Changes changes,
      MethodSymbol invokedMethodSymbol,
      List<? extends ExpressionTree> actualParameters) {
    SuggestedFix.Builder commentArgumentsFixBuilder = SuggestedFix.builder();
    List<VarSymbol> formalParameters = getFormalParametersWithoutVarArgs(invokedMethodSymbol);
    for (ParameterPair change : changes.changedPairs()) {
      int index = change.formal().index();
      ExpressionTree actual = actualParameters.get(index);
      int startPosition = ((JCTree) actual).getStartPosition();
      String formal = formalParameters.get(index).getSimpleName().toString();
      commentArgumentsFixBuilder.replace(
          startPosition, startPosition, NamedParameterComment.toCommentText(formal));
    }
    return commentArgumentsFixBuilder.build();
  }

  private static SuggestedFix buildPermuteArgumentsFix(
      Changes changes, List<? extends ExpressionTree> actualParameters, VisitorState state) {
    SuggestedFix.Builder permuteArgumentsFixBuilder = SuggestedFix.builder();
    for (ParameterPair pair : changes.changedPairs()) {
      permuteArgumentsFixBuilder.replace(
          actualParameters.get(pair.formal().index()),
          // use getSourceForNode to avoid javac pretty printing the replacement (pretty printing
          // converts unicode characters to unicode escapes)
          state.getSourceForNode(actualParameters.get(pair.actual().index())));
    }
    return permuteArgumentsFixBuilder.build();
  }
}
