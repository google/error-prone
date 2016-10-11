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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.util.stream.Collectors.toSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.Arrays;

import java.util.Optional;
import java.util.Set;

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
public class ArgumentParameterSwap extends BugChecker
    implements NewClassTreeMatcher, MethodInvocationTreeMatcher {
  /** Commonly overloaded parameter names which should not be considered for swapping into */
  private final Set<String> ignoreParams;

  static final Set<Kind> VALID_KINDS =
      ImmutableSet.of(Kind.MEMBER_SELECT, Kind.IDENTIFIER, Kind.METHOD_INVOCATION);

  public ArgumentParameterSwap() {
    this(ImmutableSet.of("index", "item", "key", "value"));
  }

  public ArgumentParameterSwap(Set<String> ignoreParams) {
    this.ignoreParams = ignoreParams;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    return findSwaps(
        tree.getArguments().stream().toArray(ExpressionTree[]::new),
        symbol.getParameters().stream().toArray(VarSymbol[]::new),
        tree,
        state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    return findSwaps(
        tree.getArguments().stream().toArray(ExpressionTree[]::new),
        symbol.getParameters().stream().toArray(VarSymbol[]::new),
        tree,
        state);
  }

  private Description findSwaps(
      ExpressionTree[] args, VarSymbol[] params, Tree tree, VisitorState state) {
    String[] suggestion = new String[params.length];

    // Create a list of suggested best arguments for the method in question.
    for (int ndx = 0; ndx < params.length; ndx++) {
      String paramName = params[ndx].getSimpleName().toString();
      Optional<String> argName = getRelevantName(args[ndx]);

      // If the parameter for this argument is not something we can extract a argument name from, is
      // under 4 characters or in our ignore list, just presume it is correct.
      if (!argName.isPresent() || paramName.length() <= 4 || ignoreParams.contains(paramName)) {
        suggestion[ndx] = args[ndx].toString();
        continue;
      }

      // Get all possible matches for the parameter in question that have the same kind as the
      // argument and get the relevant part of their name for matching up.
      ExpressionTree[] possibleMatches =
          getPossibleMatches(params[ndx], args, args[ndx].getKind(), state);
      String[] relevantMatchNames =
          Arrays.stream(possibleMatches)
              .map(exp -> getRelevantName(exp))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .toArray(String[]::new);

      // Figure out which one is best. Use the existing if nothing else works.
      int best = findBestMatch(relevantMatchNames, argName.get(), paramName);
      suggestion[ndx] = best == -1 ? args[ndx].toString() : possibleMatches[best].toString();
    }

    // If the suggestions match what we already have, then don't suggest any changes.
    String[] current = Arrays.stream(args).map(ExpressionTree::toString).toArray(String[]::new);
    if (Arrays.equals(current, suggestion)) {
      return Description.NO_MATCH;
    }

    Fix fix =
        SuggestedFix.replace(
            ((JCTree) args[0]).getStartPosition(),
            state.getEndPosition(args[args.length - 1]),
            Joiner.on(", ").join(suggestion));
    return describeMatch(tree, fix);
  }

  /**
   * Gets the part of the expression that represents the "name" for each value in expressions. If no
   * name can be extracted then an empty value is returned instead.
   */
  private Optional<String> getRelevantName(ExpressionTree exp) {
    if (exp instanceof MemberSelectTree) {
      // if we have a 'field access' we use the name of the field (ignore the name of the receiver
      // object)
      return Optional.of(((MemberSelectTree) exp).getIdentifier().toString());
    } else if (exp instanceof MethodInvocationTree) {
      // if we have a 'call expression' we use the name of the method we are calling
      return Optional.of(ASTHelpers.getSymbol(exp).getSimpleName().toString());
    } else if (exp instanceof IdentifierTree) {
      IdentifierTree idTree = (IdentifierTree) exp;
      if (idTree.getName().contentEquals("this")) {
        // for the 'this' keyword the argument name is the name of the object's class
        return Optional.of(ASTHelpers.enclosingClass(ASTHelpers.getSymbol(idTree)).name.toString());
      } else {
        // if we have a variable, just extract its name
        return Optional.of(((IdentifierTree) exp).getName().toString());
      }
    } else {
      return Optional.empty();
    }
  }
  /**
   * Finds all possible matches for param that have the same kind as the original argument, have a
   * kind that we can find replacements for, and are subtype compatible with the parameter.
   */
  private ExpressionTree[] getPossibleMatches(
      VarSymbol param, ExpressionTree[] args, Kind originalKind, VisitorState state) {
    // TODO(eaftan): Capture the list of potential matches for a parameter based on what is in
    // scope with the same type and kind.
    return Arrays.stream(args)
        .filter(
            arg ->
                (arg.getKind().equals(originalKind)
                    && VALID_KINDS.contains(arg.getKind())
                    && state.getTypes().isSubtype(ASTHelpers.getType(arg), param.asType())))
        .toArray(ExpressionTree[]::new);
  }

  /**
   * Given a parameter index, returns the argument index with the highest match. If there is a tie,
   * it will always favor the original index, followed by the first highest index.
   */
  @VisibleForTesting
  static int findBestMatch(String[] potentialMatches, String original, String param) {
    double maxMatch = calculateSimilarity(original, param);
    int maxNdx = -1;
    for (int ndx = 0; ndx < potentialMatches.length; ndx++) {
      // TODO(andrewrice): Use a beta value and require that anything better than existing must be
      // at least beta better than existing.
      double similarity = calculateSimilarity(potentialMatches[ndx], param);
      if (similarity > maxMatch) {
        maxNdx = ndx;
      }
    }
    return maxNdx;
  }

  /**
   * Calculates, for the provided argument and parameter, how close the argument and parameter are
   * to each other. The exact range of the returned values doesn't matter: larger values indicate a
   * better match, if the argument and parameter are identical this should get the highest score.
   *
   * <p>Current formula is |argTerms intersect paramTerms| / (|argTerms| + |paramTerms|) * 2.
   */
  @VisibleForTesting
  static double calculateSimilarity(String arg, String param) {
    // TODO(ciera): consider also using edit distance on individual words.
    Set<String> argSplit = splitStringTerms(arg);
    Set<String> paramSplit = splitStringTerms(param);
    // TODO(andrewrice): Handle the substring cases so that lenList matches listLength
    double commonTerms = Sets.intersection(argSplit, paramSplit).size() * 2;
    double totalTerms = argSplit.size() + paramSplit.size();
    return commonTerms / totalTerms;
  }

  /**
   * Divides a string into a Set of terms by splitting on underscores and transitions from lower to
   * upper case.
   */
  @VisibleForTesting
  static Set<String> splitStringTerms(String name) {
    // TODO(andrewrice): switch over to toImmutableSet if guava provides it in future
    return Arrays.stream(name.split("_|(?<=[a-z0-9])(?=[A-Z])"))
        .map(String::toLowerCase)
        .collect(toSet());
  }
}

