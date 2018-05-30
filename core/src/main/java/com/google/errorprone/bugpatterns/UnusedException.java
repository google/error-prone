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
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bugpattern for catch blocks which catch an exception but throw another one without wrapping the
 * original.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "UnusedException",
    summary =
        "This catch block catches an symbol and re-throws another, but swallows the caught"
            + " symbol rather than setting it as a cause. This can make debugging harder.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class UnusedException extends BugChecker implements CatchTreeMatcher {

  @Override
  public Description matchCatch(CatchTree tree, VisitorState state) {
    VarSymbol exceptionSymbol = ASTHelpers.getSymbol(tree.getParameter());
    AtomicBoolean symbolUsed = new AtomicBoolean(false);
    ((JCTree) tree)
        .accept(
            new TreeScanner() {
              @Override
              public void visitIdent(JCIdent identTree) {
                if (exceptionSymbol.equals(identTree.sym)) {
                  symbolUsed.set(true);
                }
              }
            });
    if (symbolUsed.get()) {
      return Description.NO_MATCH;
    }

    Set<JCThrow> throwTrees = new HashSet<>();
    ((JCTree) tree)
        .accept(
            new TreeScanner() {
              @Override
              public void visitThrow(JCThrow throwTree) {
                super.visitThrow(throwTree);
                throwTrees.add(throwTree);
              }

              // Don't visit nested try blocks.
              @Override
              public void visitTry(JCTry tryTree) {}
            });

    if (throwTrees.isEmpty()) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder allFixes = SuggestedFix.builder();
    throwTrees
        .stream()
        .filter(badThrow -> badThrow.getExpression() instanceof NewClassTree)
        .forEach(
            badThrow ->
                fixConstructor((NewClassTree) badThrow.getExpression(), exceptionSymbol, state)
                    .ifPresent(allFixes::merge));
    return describeMatch(tree, allFixes.build());
  }

  private static Optional<SuggestedFix> fixConstructor(
      NewClassTree constructor, VarSymbol exception, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(((JCNewClass) constructor).clazz);
    if (!(symbol instanceof ClassSymbol)) {
      return Optional.empty();
    }
    ClassSymbol classSymbol = (ClassSymbol) symbol;
    ImmutableList<MethodSymbol> constructors =
        classSymbol
            .getEnclosedElements()
            .stream()
            .filter(Symbol::isConstructor)
            .map(e -> (MethodSymbol) e)
            .collect(toImmutableList());
    MethodSymbol constructorSymbol = ASTHelpers.getSymbol(constructor);
    if (constructorSymbol == null) {
      return Optional.empty();
    }
    List<Type> types = getParameterTypes(constructorSymbol);
    for (MethodSymbol proposedConstructor : constructors) {
      List<Type> proposedTypes = getParameterTypes(proposedConstructor);
      if (proposedTypes.size() != types.size() + 1) {
        continue;
      }
      if (typesEqual(proposedTypes.subList(0, types.size()), types, state)
          && state.getTypes().isAssignable(exception.type, getLast(proposedTypes))) {
        int pos = state.getEndPosition(constructor);
        String replacement =
            String.format(
                "%s%s)", types.isEmpty() ? "" : ", ", exception.getSimpleName().toString());
        return Optional.of(SuggestedFix.replace(pos - 1, pos, replacement));
      }
    }
    return Optional.empty();
  }

  private static boolean typesEqual(List<Type> typesA, List<Type> typesB, VisitorState state) {
    return Streams.zip(
            typesA.stream(), typesB.stream(), (a, b) -> ASTHelpers.isSameType(a, b, state))
        .allMatch(x -> x);
  }

  private static List<Type> getParameterTypes(MethodSymbol constructorSymbol) {
    return constructorSymbol.type.getParameterTypes();
  }
}
