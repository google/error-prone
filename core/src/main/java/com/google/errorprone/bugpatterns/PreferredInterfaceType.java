/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.common.collect.Multimaps.asMap;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.InjectMatchers.hasProvidesAnnotation;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getErasedTypeTree;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** Tightens types which refer to an Iterable, Map, Multimap, etc. */
@BugPattern(
    altNames = {"MutableConstantField", "MutableMethodReturnType"},
    summary = "This type can be more specific.",
    severity = WARNING)
public final class PreferredInterfaceType extends BugChecker implements CompilationUnitTreeMatcher {

  private static final ImmutableList<BetterTypes> BETTER_TYPES =
      ImmutableList.of(
          BetterTypes.of(
              isDescendantOf("java.lang.Iterable"),
              "com.google.common.collect.ImmutableSortedSet",
              "com.google.common.collect.ImmutableSortedMap",
              "com.google.common.collect.ImmutableSortedMultiset",
              "com.google.common.collect.ImmutableList",
              "com.google.common.collect.ImmutableSet",
              "com.google.common.collect.ImmutableCollection",
              "java.util.List",
              "java.util.Set",
              "java.util.Collection"),
          BetterTypes.of(isDescendantOf("java.util.Map"), "com.google.common.collect.ImmutableMap"),
          BetterTypes.of(
              isDescendantOf("com.google.common.collect.Table"),
              "com.google.common.collect.ImmutableTable"),
          BetterTypes.of(
              isDescendantOf("com.google.common.collect.RangeSet"),
              "com.google.common.collect.ImmutableRangeSet"),
          BetterTypes.of(
              isDescendantOf("com.google.common.collect.RangeMap"),
              "com.google.common.collect.ImmutableRangeMap"),
          BetterTypes.of(
              isDescendantOf("com.google.common.collect.Multimap"),
              "com.google.common.collect.ImmutableListMultimap",
              "com.google.common.collect.ImmutableSetMultimap",
              "com.google.common.collect.ImmutableMultimap",
              "com.google.common.collect.ListMultimap",
              "com.google.common.collect.SetMultimap"));

  private static final Matcher<Tree> INTERESTING_TYPE =
      anyOf(
          BETTER_TYPES.stream()
              .map(bt -> Matchers.typePredicateMatcher(bt.predicate()))
              .collect(toImmutableList()));

  public static final Matcher<Tree> SHOULD_IGNORE =
      anyOf(
          hasProvidesAnnotation(),
          annotations(AT_LEAST_ONE, anyOf(isType("com.google.inject.testing.fieldbinder.Bind"))));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<Symbol, Tree> fixableTypes = getFixableTypes(state);

    ListMultimap<Symbol, Type> symbolsToType = ArrayListMultimap.create();

    new TreePathScanner<Void, Void>() {
      private final Stack<Symbol> currentMethod = new Stack<>();

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        MethodSymbol methodSymbol = getSymbol(node);
        currentMethod.push(methodSymbol);
        super.visitMethod(node, null);
        currentMethod.pop();
        return null;
      }

      @Override
      public Void visitVariable(VariableTree node, Void unused) {
        if (node.getInitializer() != null) {
          symbolsToType.put(getSymbol(node), getType(node.getInitializer()));
        }
        return super.visitVariable(node, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree node, Void unused) {
        Symbol symbol = getSymbol(node.getVariable());
        if (fixableTypes.containsKey(symbol)) {
          symbolsToType.put(symbol, getType(node.getExpression()));
        }
        return super.visitAssignment(node, null);
      }

      @Override
      public Void visitReturn(ReturnTree node, Void unused) {
        if (!currentMethod.isEmpty() && currentMethod.peek() != null) {
          symbolsToType.put(currentMethod.peek(), getType(node.getExpression()));
        }
        return super.visitReturn(node, unused);
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        currentMethod.push(null);
        super.visitLambdaExpression(node, unused);
        currentMethod.pop();
        return null;
      }
    }.scan(state.getPath(), null);

    reportFixes(fixableTypes, symbolsToType, state);
    return NO_MATCH;
  }

  private ImmutableMap<Symbol, Tree> getFixableTypes(VisitorState state) {
    ImmutableMap.Builder<Symbol, Tree> fixableTypes = ImmutableMap.builder();
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol symbol = getSymbol(tree);
        if (variableIsFixable(tree, symbol)) {
          fixableTypes.put(symbol, tree.getType());
        }
        return super.visitVariable(tree, null);
      }

      private boolean variableIsFixable(VariableTree tree, VarSymbol symbol) {
        if (symbol == null) {
          return false;
        }
        if (symbol.getKind() == ElementKind.PARAMETER) {
          return false;
        }
        if (SHOULD_IGNORE.matches(tree, state)) {
          return false;
        }
        if (symbol.getKind() == ElementKind.FIELD) {
          if (!isConsideredFinal(symbol)
              && stream(getCurrentPath())
                  .map(ASTHelpers::getSymbol)
                  .noneMatch(s -> s != null && s.isPrivate())) {
            return false;
          }
        }
        return variableType(INTERESTING_TYPE).matches(tree, state);
      }

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        MethodSymbol methodSymbol = getSymbol(node);
        if (methodReturns(INTERESTING_TYPE).matches(node, state)
            && methodSymbol != null
            && !methodCanBeOverridden(methodSymbol)
            && !SHOULD_IGNORE.matches(node, state)) {
          fixableTypes.put(methodSymbol, node.getReturnType());
        }
        return super.visitMethod(node, null);
      }
    }.scan(state.getPath(), null);
    return fixableTypes.buildOrThrow();
  }

  private void reportFixes(
      Map<Symbol, Tree> fixableTypes,
      ListMultimap<Symbol, Type> symbolsToType,
      VisitorState state) {
    Types types = state.getTypes();
    for (Map.Entry<Symbol, List<Type>> entry : asMap(symbolsToType).entrySet()) {
      Symbol symbol = entry.getKey();
      List<Type> assignedTypes = entry.getValue();
      Tree tree = fixableTypes.get(symbol);
      if (tree == null) {
        continue;
      }
      assignedTypes.stream()
          .filter(type -> !type.getKind().equals(TypeKind.NULL))
          .map(type -> getUpperBound(type, types))
          .reduce(types::lub)
          .flatMap(type -> toGoodReplacement(type, state))
          .filter(replacement -> !isSubtype(getType(tree), replacement, state))
          .ifPresent(
              type -> {
                SuggestedFix.Builder builder = SuggestedFix.builder();
                SuggestedFix fix =
                    builder
                        .replace(
                            getErasedTypeTree(tree), qualifyType(state, builder, type.asElement()))
                        .addImport(types.erasure(type).toString())
                        .build();
                state.reportMatch(
                    buildDescription(tree)
                        .setMessage(getMessage(symbol, type, state))
                        .addFix(fix)
                        .build());
              });
    }
  }

  private static String getMessage(Symbol symbol, Type newType, VisitorState state) {
    String messageBase =
        !isImmutable(targetType(symbol)) && isImmutable(newType)
            ? IMMUTABLE_MESSAGE
            : NON_IMMUTABLE_MESSAGE;
    if (symbol instanceof MethodSymbol) {
      if (!findSuperMethods((MethodSymbol) symbol, state.getTypes()).isEmpty()) {
        return "Method return" + messageBase + OVERRIDE_NOTE;
      } else {
        return "Method return" + messageBase;
      }
    } else {
      return "Variable" + messageBase;
    }
  }

  private static Type targetType(Symbol symbol) {
    return symbol instanceof MethodSymbol ? ((MethodSymbol) symbol).getReturnType() : symbol.type;
  }

  private static boolean isImmutable(Type type) {
    return type.tsym
        .getQualifiedName()
        .toString()
        .startsWith("com.google.common.collect.Immutable");
  }

  private static final String IMMUTABLE_MESSAGE =
      " type should use the immutable type (such as ImmutableList) instead of the general"
          + " collection interface type (such as List).";

  private static final String NON_IMMUTABLE_MESSAGE =
      " type can use a more specific type to convey more information to callers.";

  private static final String OVERRIDE_NOTE =
      " Note that it is legal to narrow the return type when overriding a parent method. And"
          + " because this method cannot be overridden, doing so cannot cause problems for any"
          + " subclasses.";

  private static Optional<Type> toGoodReplacement(Type type, VisitorState state) {
    return BETTER_TYPES.stream()
        .filter(bt -> bt.predicate().apply(type, state))
        .map(BetterTypes::betterTypes)
        .findFirst()
        .flatMap(
            betterTypes ->
                betterTypes.stream()
                    .map(typeName -> typeFromString(typeName).get(state))
                    .filter(
                        sensibleType ->
                            sensibleType != null && isSubtype(type, sensibleType, state))
                    .findFirst());
  }

  @AutoValue
  abstract static class BetterTypes {
    abstract TypePredicate predicate();

    abstract ImmutableSet<String> betterTypes();

    private static BetterTypes of(TypePredicate predicate, String... betterTypes) {
      return new AutoValue_PreferredInterfaceType_BetterTypes(
          predicate, ImmutableSet.copyOf(betterTypes));
    }
  }
}
