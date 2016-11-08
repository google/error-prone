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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
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
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import javax.annotation.Nullable;

/**
 * Abstract superclass for checks that inspect method calls and flag places where the wrong
 * arguments were passed, based on lexical similarity to the parameter names.
 *
 * <p>This is the basis for several implementations of the argument recommendation analysis from Liu
 * et al., "Nomen est Omen: Exploring and Exploiting Similarities between Argument and Parameter
 * Names," ICSE 2016.
 */
abstract class AbstractArgumentParameterChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  /**
   * A threshold for how much more similar a proposed argument must be than the existing one for
   * this check to suggest it as a replacement. A proposed replacement must have {@code (similarity
   * - curr_similarity) >= beta} to be suggested.
   *
   * <p>This corresponds to the beta parameter defined in sections 4.1.1 and 4.1.2 of the paper.
   *
   * <p>Note that an appropriate value for beta depends on the choice of {@link #similarityMetric}.
   */
  private final double beta;

  /**
   * A function that, given a {@link VisitorState}, returns a list of potential replacements for any
   * of the parameters to this method. The {@link VisitorState} must include a TreePath whose leaf
   * is one of the arguments to potentially replace, so that implementations can correctly compute
   * what is in scope.
   */
  private final Function<VisitorState, ImmutableSet<PotentialReplacement>>
      potentialReplacementsFunction;

  /**
   * A {@link Predicate} for parameters for which we want to suggest possible replacements.
   *
   * <p>This can be used to, for example, skip parameters with names that typically have low
   * similarity with their arguments, as in Section 3.5 of the paper.
   */
  private final Predicate<VarSymbol> parameterPredicate;

  /**
   * A similarity metric that compares two strings and returns a value between 0.0 and 1.0, where
   * lower scores indicate less similarity.
   */
  private final ToDoubleBiFunction<String, String> similarityMetric;

  /** A set of AST node {@link Kind}s for which this check may suggest a replacement. */
  private final ImmutableSet<Kind> validKinds;

  /**
   * A predicate that accepts parameters whose names have length &gt; {@code minLength} and are not
   * in the set of low-similarity parameter names.
   */
  protected static class ParameterPredicate implements Predicate<VarSymbol> {
    private final ImmutableSet<String> lowSimilarityNames;
    private final int minLength;

    protected ParameterPredicate(ImmutableSet<String> lowSimilarityNames, int minLength) {
      this.lowSimilarityNames = lowSimilarityNames;
      this.minLength = minLength;
    }

    @Override
    public boolean test(VarSymbol sym) {
      String paramName = sym.getSimpleName().toString();
      return paramName.length() > minLength && !lowSimilarityNames.contains(paramName);
    }
  }

  protected AbstractArgumentParameterChecker(
      Function<VisitorState, ImmutableSet<PotentialReplacement>> potentialReplacementsFunction,
      Predicate<VarSymbol> parameterPredicate,
      double beta,
      ToDoubleBiFunction<String, String> similarityMetric,
      ImmutableSet<Kind> validKinds) {
    this.potentialReplacementsFunction = potentialReplacementsFunction;
    this.parameterPredicate = parameterPredicate;
    this.beta = beta;
    this.similarityMetric = similarityMetric;
    this.validKinds = validKinds;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol methodSym = ASTHelpers.getSymbol(tree);
    if (methodSym == null) {
      return Description.NO_MATCH;
    }
    return findReplacements(
        tree.getArguments(), methodSym.getParameters(), methodSym.isVarArgs(), state, tree);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol methodSym = ASTHelpers.getSymbol(tree);
    if (methodSym == null) {
      return Description.NO_MATCH;
    }
    return findReplacements(
        tree.getArguments(), methodSym.getParameters(), methodSym.isVarArgs(), state, tree);
  }

  private Description findReplacements(
      List<? extends ExpressionTree> args,
      com.sun.tools.javac.util.List<VarSymbol> params,
      boolean isVarArgs,
      VisitorState state,
      Tree tree) {
    if (args.isEmpty()) {
      return Description.NO_MATCH;
    }

    ImmutableSet<PotentialReplacement> potentialReplacements =
        potentialReplacementsFunction.apply(
            state.withPath(new TreePath(state.getPath(), args.get(0))));

    SuggestedFix.Builder fix = SuggestedFix.builder();
    // Don't suggest for the varargs parameter.
    // TODO(eaftan): Reconsider this, especially if the argument is of array type or is itself
    // a varargs parameter.
    int maxArg = isVarArgs ? params.size() - 1 : params.size();
    for (int i = 0; i < maxArg; i++) {
      ExpressionTree arg = args.get(i);
      VarSymbol param = params.get(i);
      if (!validKinds.contains(arg.getKind()) || !parameterPredicate.test(param)) {
        continue;
      }

      String extractedArgumentName = extractArgumentName(arg);
      if (extractedArgumentName == null) {
        continue;
      }
      double currSimilarity =
          similarityMetric.applyAsDouble(extractedArgumentName, param.getSimpleName().toString());
      if (1.0 - currSimilarity < beta) {
        // No way for any replacement to be at least BETA better than the current argument
        continue;
      }

      ReplacementWithSimilarity bestReplacement =
          potentialReplacements
              .stream()
              .filter(replacement -> !replacement.sym().equals(ASTHelpers.getSymbol(arg)))
              .filter(
                  replacement -> isSubtypeHandleCompletionFailures(replacement.sym(), param, state))
              .map(
                  replacement ->
                      ReplacementWithSimilarity.create(
                          replacement,
                          similarityMetric.applyAsDouble(
                              replacement.argumentName(), param.getSimpleName().toString())))
              .max(Comparator.comparingDouble(ReplacementWithSimilarity::similarity))
              .orElse(null);
      if ((bestReplacement != null) && (bestReplacement.similarity() - currSimilarity >= beta)) {
        fix.replace(arg, bestReplacement.replacement().replacementString());
      }
    }
    if (fix.isEmpty()) {
      return Description.NO_MATCH;
    } else {
      return describeMatch(tree, fix.build());
    }
  }

  private static boolean isSubtypeHandleCompletionFailures(
      Symbol replacement, VarSymbol param, VisitorState state) {
    Type replacementType;
    if (replacement instanceof VarSymbol) {
      replacementType = replacement.asType();
    } else if (replacement instanceof MethodSymbol) {
      replacementType = ((MethodType) (replacement.asType())).getReturnType();
    } else {
      return false;
    }
    try {
      return state.getTypes().isSubtype(replacementType, param.asType());
    } catch (CompletionFailure e) {
      // bail out if necessary symbols to do the subtype check are not on the classpath
      return false;
    }
  }

  /**
   * Extracts the "argument name," as defined in section 2.1 of the paper, from the expression. This
   * translates a potentially complex expression into a simple name that can be used by the
   * similarity metric.
   */
  @Nullable
  protected static String extractArgumentName(ExpressionTree expr) {
    switch (expr.getKind()) {
      case MEMBER_SELECT:
        {
          // if we have a 'field access' we use the name of the field (ignore the name of the
          // receiver object)
          return ((MemberSelectTree) expr).getIdentifier().toString();
        }
      case METHOD_INVOCATION:
        {
          // if we have a 'call expression' we use the name of the method we are calling
          Symbol sym = ASTHelpers.getSymbol(expr);
          return (sym == null) ? null : sym.getSimpleName().toString();
        }
      case IDENTIFIER:
        {
          IdentifierTree idTree = (IdentifierTree) expr;
          if (idTree.getName().contentEquals("this")) {
            // for the 'this' keyword the argument name is the name of the object's class
            Symbol sym = ASTHelpers.getSymbol(idTree);
            return (sym == null) ? null : ASTHelpers.enclosingClass(sym).getSimpleName().toString();
          } else {
            // if we have a variable, just extract its name
            return ((IdentifierTree) expr).getName().toString();
          }
        }
      default:
        return null;
    }
  }

  @AutoValue
  abstract static class ReplacementWithSimilarity {
    abstract PotentialReplacement replacement();

    abstract double similarity();

    static ReplacementWithSimilarity create(PotentialReplacement replacement, double similarity) {
      return new AutoValue_AbstractArgumentParameterChecker_ReplacementWithSimilarity(
          replacement, similarity);
    }
  }

  @AutoValue
  protected abstract static class PotentialReplacement {
    /** The (simple) argument name, as defined in section 2.1 of the paper. */
    abstract String argumentName();

    /** The full string that should be used as the replacement. */
    abstract String replacementString();

    /** The {@link Symbol} that represents this potential replacement. */
    abstract Symbol sym();

    protected static PotentialReplacement create(
        String argumentName, String replacementString, Symbol sym) {
      return new AutoValue_AbstractArgumentParameterChecker_PotentialReplacement(
          argumentName, replacementString, sym);
    }
  }
}
