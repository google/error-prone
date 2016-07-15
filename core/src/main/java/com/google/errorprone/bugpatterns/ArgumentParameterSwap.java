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

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
    LexicalSimilarityMatcher matcher = new LexicalSimilarityMatcher(args, params, state);
    boolean bestMatch = true;

    // TODO(eaftan): Capture the list of potential matches for a parameter based on what is in
    // scope with the same type and kind.

    // TODO(ciera): Instead of the algorithm below, capture all of the potential
    // matches for each argument, then select the best match and store it. Return a match
    // if the suggested replacement is different from the existing
    for (int ndx = 0; ndx < args.length; ndx++) {
      if (!(args[ndx] instanceof IdentifierTree)
          && !(args[ndx] instanceof MemberSelectTree)
          && !(args[ndx] instanceof MethodInvocationTree)) {
        continue;
      }

      if (params[ndx].getSimpleName().toString().length() <= 4) {
        continue;
      }

      if (IGNORE_PARAMS.contains(params[ndx].getSimpleName().toString())) {
        continue;
      }

      if (matcher.findBestArgument(ndx) != ndx) {
        bestMatch = false;
        break;
      }
    }

    if (bestMatch) {
      return Description.NO_MATCH;
    }

    String[] suggestion = new String[params.length];
    for (int pNdx = 0; pNdx < params.length; pNdx++) {
      int match = pNdx;
      if (args[pNdx] instanceof IdentifierTree
          || args[pNdx] instanceof MemberSelectTree
          || args[pNdx] instanceof MethodInvocationTree) {
        match = matcher.findBestArgument(pNdx);
      }
      suggestion[pNdx] = args[match].toString();
    }

    Fix fix =
        SuggestedFix.replace(
            ((JCTree) args[0]).getStartPosition(),
            state.getEndPosition(args[args.length - 1]),
            Joiner.on(", ").join(suggestion));
    return describeMatch(tree, fix);
  }

  static class LexicalSimilarityMatcher {
    private final double[][] simMatrix;
    private final VisitorState state;
    private final VarSymbol[] params;
    private final ExpressionTree[] args;

    private LexicalSimilarityMatcher(
        ExpressionTree[] args, VarSymbol[] params, VisitorState state) {
      this.state = state;
      this.params = params;
      this.args = args;

      String[] paramNames =
          Arrays.stream(params)
              .map(param -> param.getSimpleName().toString())
              .toArray(String[]::new);
      String[] argNames = new String[args.length];
      for (int ndx = 0; ndx < args.length; ndx++) {
        ExpressionTree tree = args[ndx];
        if (tree instanceof MemberSelectTree) {
          argNames[ndx] = ((MemberSelectTree) tree).getIdentifier().toString();
        } else if (tree instanceof MethodInvocationTree) {
          argNames[ndx] = ASTHelpers.getSymbol(tree).getSimpleName().toString();
        } else {
          argNames[ndx] = tree.toString();
        }
      }

      this.simMatrix = calculateSimilarityMatrix(argNames, paramNames);
    }

    /**
     * Given a parameter index, returns the argument index with the highest match. If there is a
     * tie, it will always favor the original index, followed by the first highest index.
     */
    int findBestArgument(int pNdx) {
      int maxNdx = pNdx;
      for (int aNdx = 0; aNdx < params.length; aNdx++) {
        if (!state.getTypes().isSubtype(ASTHelpers.getType(args[aNdx]), params[pNdx].asType())) {
          continue;
        }
        // TODO(ciera): Use a beta value and require that anything better than existing must be at
        // least beta better than existing.
        if (simMatrix[aNdx][pNdx] > simMatrix[maxNdx][pNdx]) {
          maxNdx = aNdx;
        }
      }
      return maxNdx;
    }
  }

  /**
   * Calculates, for each argument/parameter pair, how close the argument and parameter are to each
   * other based on the formula |argTerms intersect paramTerms| / (|argTerms| + |paramTerms|) * 2
   */
  static double[][] calculateSimilarityMatrix(String[] args, String[] params) {
    // TODO(ciera): consider also using edit distance on individual words.
    double[][] similarity = new double[args.length][params.length];
    for (int aNdx = 0; aNdx < args.length; aNdx++) {
      Set<String> argSplit = splitStringTerms(args[aNdx]);
      for (int pNdx = 0; pNdx < params.length; pNdx++) {
        Set<String> paramSplit = splitStringTerms(params[pNdx]);
        // TODO(ciera): Handle the substring cases so that lenList matches listLength
        double commonTerms = Sets.intersection(argSplit, paramSplit).size() * 2;
        double totalTerms = argSplit.size() + paramSplit.size();
        similarity[aNdx][pNdx] = commonTerms / totalTerms;
      }
    }
    return similarity;
  }

  /**
   * Splits a string into a Set of terms. If the name starts with a lower-case letter, it is
   * presumed to be in lowerCamelCase format. Otherwise, it presumes UPPER_UNDERSCORE format.
   */
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
