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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.Arrays;
import java.util.List;
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
  public static final List<String> IGNORE_PARAMS =
      ImmutableList.of("message", "counter", "index", "object", "value");

  static final Set<Kind> VALID_KINDS =
      ImmutableSet.of(Kind.MEMBER_SELECT, Kind.IDENTIFIER, Kind.METHOD_INVOCATION);

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
      String argName = getRelevantName(args[ndx]);
      // If the parameter for this argument is under 4 characters or in our ignore list, just
      // presume it is correct.
      if (paramName.length() <= 4 || IGNORE_PARAMS.contains(paramName)) {
        suggestion[ndx] = args[ndx].toString();
        continue;
      }

      // Get all possible matches for the parameter in question that have the same kind as the
      // argument and get the relevant part of their name for matching up.
      ExpressionTree[] possibleMatches =
          getPossibleMatches(params[ndx], args, args[ndx].getKind(), state);
      String[] relevantMatchNames =
          Arrays.stream(possibleMatches).map(exp -> getRelevantName(exp)).toArray(String[]::new);

      // Figure out which one is best. Use the existing if nothing else works.
      int best = findBestMatch(relevantMatchNames, argName, paramName);
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

  /** Gets the part of the expression that represents the "name" for each value in expressions. */
  private String getRelevantName(ExpressionTree exp) {
    if (exp instanceof MemberSelectTree) {
      return ((MemberSelectTree) exp).getIdentifier().toString();
    } else if (exp instanceof MethodInvocationTree) {
      return ASTHelpers.getSymbol(exp).getSimpleName().toString();
    } else {
      return exp.toString();
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
   * Splits a string into a Set of terms. If the name starts with a lower-case letter, it is
   * presumed to be in lowerCamelCase format. Otherwise, it presumes UPPER_UNDERSCORE format.
   */
  @VisibleForTesting
  static Set<String> splitStringTerms(String name) {
    // TODO(ciera): Handle lower_underscore
    CaseFormat caseFormat =
        Character.isLowerCase(name.charAt(0))
            ? CaseFormat.LOWER_CAMEL
            : CaseFormat.UPPER_UNDERSCORE;
    String nameSplit = caseFormat.to(CaseFormat.LOWER_UNDERSCORE, name);
    return Sets.newHashSet(
        Splitter.on('_').trimResults().omitEmptyStrings().splitToList(nameSplit));
  }
}
