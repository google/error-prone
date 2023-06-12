/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.fixes.SuggestedFixes.removeElement;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;

import com.google.common.collect.ImmutableMultiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.List;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = SeverityLevel.WARNING,
    summary = "This type parameter is unused and can be removed.")
public final class UnusedTypeParameter extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    var usedIdentifiers = findUsedIdentifiers(tree);
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitClass(ClassTree node, Void unused) {
        if ((getSymbol(node).flags() & Flags.FINAL) != 0) {
          handle(node, node.getTypeParameters());
        }
        return super.visitClass(node, null);
      }

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        var symbol = getSymbol(node);
        if (methodCanBeOverridden(symbol)
            || !findSuperMethods(symbol, state.getTypes()).isEmpty()) {
          return null;
        }
        handle(node, node.getTypeParameters());
        return super.visitMethod(node, null);
      }

      private void handle(Tree tree, List<? extends TypeParameterTree> typeParameters) {
        for (TypeParameterTree typeParameter : typeParameters) {
          if (usedIdentifiers.count(getSymbol(typeParameter)) == 1) {
            state.reportMatch(
                describeMatch(
                    typeParameter,
                    removeTypeParameter(tree, typeParameter, typeParameters, state)));
          }
        }
      }
    }.scan(state.getPath(), null);
    return Description.NO_MATCH;
  }

  private static ImmutableMultiset<TypeVariableSymbol> findUsedIdentifiers(
      CompilationUnitTree tree) {
    ImmutableMultiset.Builder<TypeVariableSymbol> identifiers = ImmutableMultiset.builder();
    new TreeScanner<Void, Void>() {
      @Override
      public Void scan(Tree tree, Void unused) {
        var symbol = getSymbol(tree);
        if (symbol instanceof TypeVariableSymbol) {
          identifiers.add((TypeVariableSymbol) symbol);
        }
        return super.scan(tree, unused);
      }
    }.scan(tree, null);
    return identifiers.build();
  }

  private static SuggestedFix removeTypeParameter(
      Tree tree,
      TypeParameterTree typeParameter,
      List<? extends TypeParameterTree> typeParameters,
      VisitorState state) {
    if (typeParameters.size() > 1) {
      return removeElement(typeParameter, typeParameters, state);
    }
    var tokens =
        ErrorProneTokens.getTokens(
            state.getSourceForNode(tree), getStartPosition(tree), state.context);
    int startPos =
        tokens.reverse().stream()
            .filter(
                t -> t.pos() <= getStartPosition(typeParameter) && t.kind().equals(TokenKind.LT))
            .findFirst()
            .get()
            .pos();
    int endPos =
        tokens.stream()
            .filter(
                t ->
                    t.endPos() >= state.getEndPosition(getLast(typeParameters))
                        && (t.kind().equals(TokenKind.GT) || t.kind().equals(TokenKind.GTGT)))
            .findFirst()
            .get()
            .endPos();
    return SuggestedFix.replace(startPos, endPos, "");
  }
}
