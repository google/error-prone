/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.names.LevenshteinEditDistance;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Utility methods to find replacement variables with similar names. */
public final class ReplacementVariableFinder {
  private ReplacementVariableFinder() {}

  /**
   * Suggest replacing {@code input} with a qualified reference to a locally declared field with a
   * similar or the same name as the {@code input} expression.
   *
   * @param input a MEMBER_SELECT or IDENTIFIER expression which should be replaced.
   * @param validFieldPredicate Predicate used to decide which locally declared fields are
   *     appropriate candidates for replacement (e.g.: is the appropriate type)
   */
  public static ImmutableList<Fix> fixesByReplacingExpressionWithLocallyDeclaredField(
      ExpressionTree input, Predicate<JCVariableDecl> validFieldPredicate, VisitorState state) {
    Preconditions.checkState(input.getKind() == IDENTIFIER || input.getKind() == MEMBER_SELECT);

    ImmutableMultimap<Integer, JCVariableDecl> potentialReplacements =
        ASTHelpers.findEnclosingNode(state.getPath(), JCClassDecl.class).getMembers().stream()
            .filter(JCVariableDecl.class::isInstance)
            .map(JCVariableDecl.class::cast)
            .filter(validFieldPredicate)
            .collect(collectByEditDistanceTo(simpleNameOfIdentifierOrMemberAccess(input)));

    return buildValidReplacements(
        potentialReplacements, var -> SuggestedFix.replace(input, "this." + var.sym));
  }

  /**
   * Suggest replacing {@code input} with a reference to a method parameter in the nearest enclosing
   * method declaration with a similar or the same name as the {@code input} expression.
   *
   * @param input a MEMBER_SELECT or IDENTIFIER expression which should be replaced.
   * @param validParameterPredicate Predicate used to decide which method parameters are appropriate
   *     candidates for replacement (e.g.: is the appropriate type)
   */
  public static ImmutableList<Fix> fixesByReplacingExpressionWithMethodParameter(
      ExpressionTree input, Predicate<JCVariableDecl> validParameterPredicate, VisitorState state) {
    Preconditions.checkState(input.getKind() == IDENTIFIER || input.getKind() == MEMBER_SELECT);

    // find a method parameter matching the input predicate and similar name and suggest it
    // as the new argument
    Multimap<Integer, JCVariableDecl> potentialReplacements =
        ASTHelpers.findEnclosingNode(state.getPath(), JCMethodDecl.class).getParameters().stream()
            .filter(validParameterPredicate)
            .collect(collectByEditDistanceTo(simpleNameOfIdentifierOrMemberAccess(input)));

    return buildValidReplacements(
        potentialReplacements, var -> SuggestedFix.replace(input, var.sym.toString()));
  }

  private static ImmutableList<Fix> buildValidReplacements(
      Multimap<Integer, JCVariableDecl> potentialReplacements,
      Function<JCVariableDecl, Fix> replacementFunction) {
    if (potentialReplacements.isEmpty()) {
      return ImmutableList.of();
    }

    // Take all of the potential edit-distance replacements with the same minimum distance,
    // then suggest them as individual fixes.
    return potentialReplacements.get(Collections.min(potentialReplacements.keySet())).stream()
        .map(replacementFunction)
        .collect(toImmutableList());
  }

  private static Collector<JCVariableDecl, ?, ImmutableMultimap<Integer, JCVariableDecl>>
      collectByEditDistanceTo(String baseName) {
    return Collectors.collectingAndThen(
        Multimaps.toMultimap(
            (JCVariableDecl varDecl) ->
                LevenshteinEditDistance.getEditDistance(baseName, varDecl.name.toString()),
            varDecl -> varDecl,
            LinkedHashMultimap::create),
        ImmutableMultimap::copyOf);
  }

  private static String simpleNameOfIdentifierOrMemberAccess(ExpressionTree tree) {
    String name = null;
    if (tree.getKind() == IDENTIFIER) {
      name = ((JCIdent) tree).name.toString();
    } else if (tree.getKind() == MEMBER_SELECT) {
      name = ((JCFieldAccess) tree).name.toString();
    }
    return name;
  }
}
