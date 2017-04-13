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
import java.util.List;
import java.util.function.BiFunction;

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
  category = JDK,
  severity = WARNING
)
public class ArgumentSelectionDefectChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private final ImmutableList<Heuristic> heuristics;
  private final BiFunction<String, String, Double> nameDistanceFunction;

  private static final BiFunction<String, String, Double> DEFAULT_NAME_DISTANCE_FUNCTION =
      new BiFunction<String, String, Double>() {
        @Override
        public Double apply(String source, String target) {
          String normalizedSource = NamingConventions.convertToLowerUnderscore(source);
          String normalizedTarget = NamingConventions.convertToLowerUnderscore(target);
          return NeedlemanWunschEditDistance.getNormalizedEditDistance(
              /*          source */ normalizedSource,
              /*          target */ normalizedTarget,
              /*   caseSensitive */ false,
              /*      changeCost */ 8,
              /*     openGapCost */ 8,
              /* continueGapCost */ 1);
        }
      };

  public ArgumentSelectionDefectChecker() {
    this(
        DEFAULT_NAME_DISTANCE_FUNCTION,
        ImmutableList.of(
            new LowInformationNameHeuristic(),
            new PenaltyThresholdHeuristic(),
            new EnclosedByReverseHeuristic(),
            new CreatesDuplicateCallHeuristic(),
            new NameInCommentHeuristic()));
  }

  public ArgumentSelectionDefectChecker(
      BiFunction<String, String, Double> nameDistanceFunction,
      ImmutableList<Heuristic> heuristics) {
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

    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (ParameterPair pair : changes.changedPairs()) {
      fix.replace(
          actualParameters.get(pair.formal().index()),
          actualParameters.get(pair.actual().index()).toString());
    }
    return describeMatch(invokedMethodTree, fix.build());
  }

  private Changes findChanges(
      Tree invokedMethodTree,
      MethodSymbol invokedMethodSymbol,
      List<? extends ExpressionTree> actualParameters,
      VisitorState state) {

    List<VarSymbol> formalParameters = invokedMethodSymbol.getParameters();

    /* javac can get argument names from debugging symbols if they are not available from
    other sources. When it does this for an inner class sometimes it returns the implicit this
    pointer for the outer class as the first name (but not the first type). If we see this, then
    just abort */
    if (!formalParameters.isEmpty()
        && formalParameters.get(0).getSimpleName().toString().matches("this\\$[0-9]+")) {
      return Changes.empty();
    }

    /* If we have a varargs method then just ignore the final parameter and trailing actual
    parameters */
    int size =
        invokedMethodSymbol.isVarArgs() ? formalParameters.size() - 1 : formalParameters.size();

    /* Methods with one or fewer parameters cannot possibly have a swap */
    if (size <= 1) {
      return Changes.empty();
    }

    /* Sometimes we don't have enough actual parameters. This seems to happen sometimes with calls
     * to super and javac finds two parameters arg0 and arg1 and no arguments */
    if (actualParameters.size() < size) {
      return Changes.empty();
    }

    ImmutableList<Parameter> formals =
        Parameter.createListFromVarSymbols(formalParameters.subList(0, size));
    ImmutableList<Parameter> actuals =
        Parameter.createListFromExpressionTrees(actualParameters.subList(0, size));

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
    costs.viablePairs().forEach(p -> costs.updatePair(p, computeEditDistance(p)));

    Changes changes = costs.computeAssignments();

    if (changes.isEmpty()) {
      return changes;
    }

    /* Only keep this change if all of the heuristcs match */
    for (Heuristic heuristic : heuristics) {
      if (!heuristic.isAcceptableChange(changes, invokedMethodTree, invokedMethodSymbol, state)) {
        return Changes.empty();
      }
    }

    return changes;
  }

  /**
   * Wraps the provided distance function to deal with wildcard and unknown names and the penalty
   * distance: a wildcard name has distance 0 to everything, an unknown name has infinite distance
   * to every other alternative except itself. The penalty distance is added to the distance between
   * all alternatives and the formal parameter but not the existing actual parameter.
   */
  private double computeEditDistance(ParameterPair pair) {
    Parameter formal = pair.formal();
    Parameter actual = pair.actual();

    if (formal.hasWildcardName() || actual.hasWildcardName()) {
      return 0.0;
    }

    if (formal.hasUnknownName() || actual.hasUnknownName()) {
      return formal.index() == actual.index() ? 0.0 : Double.POSITIVE_INFINITY;
    }

    double score = nameDistanceFunction.apply(formal.name(), actual.name());

    return score;
  }
}
