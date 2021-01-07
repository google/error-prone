/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.nothing;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;

import com.google.auto.value.AutoValue;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.lang.model.type.TypeKind;

/**
 * Flags methods which return mutable collections from some code paths, but immutable ones from
 * others.
 */
@BugPattern(
    name = "MixedMutabilityReturnType",
    summary =
        "This method returns both mutable and immutable collections or maps from different "
            + "paths. This may be confusing for users of the method.",
    severity = SeverityLevel.WARNING)
public final class MixedMutabilityReturnType extends BugChecker
    implements CompilationUnitTreeMatcher {

  private static final Matcher<ExpressionTree> IMMUTABLE_FACTORY =
      staticMethod()
          .onClass("java.util.Collections")
          .namedAnyOf("emptyList", "emptyMap", "emptySet", "singleton", "singletonList");

  private static final Matcher<ExpressionTree> EMPTY_INITIALIZER =
      anyOf(
          constructor().forClass("java.util.ArrayList").withParameters(),
          constructor().forClass("java.util.HashMap").withParameters(),
          staticMethod()
              .onClass("com.google.common.collect.Lists")
              .namedAnyOf("newArrayList", "newLinkedList")
              .withParameters(),
          staticMethod()
              .onClass("com.google.common.collect.Sets")
              .namedAnyOf("newHashSet", "newLinkedHashSet")
              .withParameters());

  private static final Matcher<ExpressionTree> IMMUTABLE =
      anyOf(
          IMMUTABLE_FACTORY,
          isSubtypeOf(ImmutableCollection.class),
          isSubtypeOf(ImmutableMap.class));

  private static final Matcher<ExpressionTree> MUTABLE =
      anyOf(
          isSubtypeOf(ArrayList.class),
          isSubtypeOf(LinkedHashSet.class),
          isSubtypeOf(LinkedHashMap.class),
          isSubtypeOf(LinkedList.class),
          isSubtypeOf(HashMap.class),
          isSubtypeOf(HashBiMap.class),
          isSubtypeOf(TreeMap.class));

  private static final Matcher<Tree> RETURNS_COLLECTION =
      anyOf(isSubtypeOf(Collection.class), isSubtypeOf(Map.class));

  private static final ImmutableMap<Matcher<Tree>, TypeDetails> REFACTORING_DETAILS =
      ImmutableMap.of(
          isSubtypeOf(BiMap.class),
              TypeDetails.of(
                  "com.google.common.collect.ImmutableBiMap",
                  instanceMethod()
                      .onDescendantOf(BiMap.class.getName())
                      .namedAnyOf("put", "putAll"),
                  nothing()),
          isSubtypeOf(Map.class),
              TypeDetails.of(
                  "com.google.common.collect.ImmutableMap",
                  instanceMethod().onDescendantOf(Map.class.getName()).namedAnyOf("put", "putAll"),
                  isSubtypeOf(SortedMap.class)),
          isSubtypeOf(List.class),
              TypeDetails.of(
                  "com.google.common.collect.ImmutableList",
                  instanceMethod().onDescendantOf(List.class.getName()).namedAnyOf("add", "addAll"),
                  nothing()),
          isSubtypeOf(Set.class),
              TypeDetails.of(
                  "com.google.common.collect.ImmutableSet",
                  instanceMethod().onDescendantOf(Set.class.getName()).namedAnyOf("add", "addAll"),
                  nothing()));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    VariableMutabilityScanner variableMutabilityScanner = new VariableMutabilityScanner(state);
    variableMutabilityScanner.scan(state.getPath(), null);
    new ReturnTypesScanner(
            state, variableMutabilityScanner.immutable, variableMutabilityScanner.mutable)
        .scan(state.getPath(), null);
    return Description.NO_MATCH;
  }

  private static final class VariableMutabilityScanner extends TreePathScanner<Void, Void> {
    private final VisitorState state;

    private final Set<VarSymbol> mutable = new HashSet<>();
    private final Set<VarSymbol> immutable = new HashSet<>();

    private VariableMutabilityScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitVariable(VariableTree variableTree, Void unused) {
      VarSymbol symbol = getSymbol(variableTree);
      ExpressionTree initializer = variableTree.getInitializer();
      if (initializer != null
          && getType(initializer) != null
          && getType(initializer).getKind() != TypeKind.NULL
          && RETURNS_COLLECTION.matches(initializer, state)) {
        if (IMMUTABLE.matches(initializer, state)) {
          immutable.add(symbol);
        }
        if (MUTABLE.matches(initializer, state)) {
          mutable.add(symbol);
        }
      }
      return super.visitVariable(variableTree, unused);
    }
  }

  private final class ReturnTypesScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final VisitorState state;

    private final Set<VarSymbol> mutable;
    private final Set<VarSymbol> immutable;

    private ReturnTypesScanner(
        VisitorState state, Set<VarSymbol> immutable, Set<VarSymbol> mutable) {
      this.state = state;
      this.immutable = immutable;
      this.mutable = mutable;
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Void unused) {
      if (!RETURNS_COLLECTION.matches(methodTree.getReturnType(), state)) {
        return super.visitMethod(methodTree, unused);
      }
      MethodScanner scanner = new MethodScanner();
      scanner.scan(getCurrentPath(), null);
      if (!scanner.immutableReturns.isEmpty() && !scanner.mutableReturns.isEmpty()) {
        state.reportMatch(
            buildDescription(methodTree)
                .addAllFixes(
                    generateFixes(
                        ImmutableList.<ReturnTree>builder()
                            .addAll(scanner.mutableReturns)
                            .addAll(scanner.immutableReturns)
                            .build(),
                        getCurrentPath(),
                        state))
                .build());
      }
      return super.visitMethod(methodTree, unused);
    }

    private final class MethodScanner extends TreePathScanner<Void, Void> {
      private final List<ReturnTree> immutableReturns = new ArrayList<>();
      private final List<ReturnTree> mutableReturns = new ArrayList<>();
      private boolean skipMethods = false;

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        if (skipMethods) {
          return null;
        }
        skipMethods = true;
        return super.visitMethod(node, null);
      }

      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        if (returnTree.getExpression() instanceof IdentifierTree) {
          Symbol symbol = getSymbol(returnTree.getExpression());
          if (mutable.contains(symbol)) {
            mutableReturns.add(returnTree);
            return super.visitReturn(returnTree, null);
          }
          if (immutable.contains(symbol)) {
            immutableReturns.add(returnTree);
            return super.visitReturn(returnTree, null);
          }
        }
        Type type = getType(returnTree.getExpression());
        if (type == null || type.getKind() == TypeKind.NULL) {
          return super.visitReturn(returnTree, null);
        }
        if (IMMUTABLE.matches(returnTree.getExpression(), state)) {
          immutableReturns.add(returnTree);
        }
        if (MUTABLE.matches(returnTree.getExpression(), state)) {
          mutableReturns.add(returnTree);
        }
        return super.visitReturn(returnTree, null);
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        return null;
      }
    }
  }

  private static ImmutableList<SuggestedFix> generateFixes(
      List<ReturnTree> returnTrees, TreePath methodTree, VisitorState state) {
    SuggestedFix.Builder simpleFix = SuggestedFix.builder();
    SuggestedFix.Builder fixWithBuilders = SuggestedFix.builder();
    boolean anyBuilderFixes = false;

    Matcher<Tree> returnTypeMatcher = null;
    for (Map.Entry<Matcher<Tree>, TypeDetails> entry : REFACTORING_DETAILS.entrySet()) {
      Tree returnType = ((MethodTree) methodTree.getLeaf()).getReturnType();
      Matcher<Tree> matcher = entry.getKey();
      if (matcher.matches(returnType, state)) {
        // Only change the return type if the method is not overridable, otherwise this could
        // break builds.
        if (!methodCanBeOverridden(getSymbol((MethodTree) methodTree.getLeaf()))) {
          SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
          fixBuilder.replace(
              ASTHelpers.getErasedTypeTree(returnType),
              qualifyType(state, fixBuilder, entry.getValue().immutableType()));
          simpleFix.merge(fixBuilder);
          fixWithBuilders.merge(fixBuilder);
        }
        returnTypeMatcher = isSubtypeOf(entry.getValue().immutableType());
        break;
      }
    }
    if (returnTypeMatcher == null) {
      return ImmutableList.of();
    }
    for (ReturnTree returnTree : returnTrees) {
      if (returnTypeMatcher.matches(returnTree.getExpression(), state)) {
        break;
      }
      for (Map.Entry<Matcher<Tree>, TypeDetails> entry : REFACTORING_DETAILS.entrySet()) {
        Matcher<Tree> predicate = entry.getKey();
        TypeDetails typeDetails = entry.getValue();
        ExpressionTree expression = returnTree.getExpression();
        // Skip already immutable returns.
        if (!predicate.matches(expression, state)) {
          continue;
        }
        if (expression instanceof IdentifierTree) {
          SuggestedFix simple = applySimpleFix(typeDetails.immutableType(), expression, state);
          // If we're returning an identifier of this mutable type, try to turn it into a Builder.
          ReturnTypeFixer returnTypeFixer =
              new ReturnTypeFixer(getSymbol(expression), typeDetails, state);
          returnTypeFixer.scan(methodTree, null);

          anyBuilderFixes |= !returnTypeFixer.failed;
          simpleFix.merge(simple);
          fixWithBuilders.merge(returnTypeFixer.failed ? simple : returnTypeFixer.fix.build());
          continue;
        }
        if (IMMUTABLE_FACTORY.matches(expression, state)) {
          SuggestedFix.Builder fix = SuggestedFix.builder();
          fix.replace(
              ((MethodInvocationTree) expression).getMethodSelect(),
              qualifyType(state, fix, typeDetails.immutableType()) + ".of");
          simpleFix.merge(fix);
          fixWithBuilders.merge(fix);
          continue;
        }

        SuggestedFix simple = applySimpleFix(typeDetails.immutableType(), expression, state);
        simpleFix.merge(simple);
        fixWithBuilders.merge(simple);
      }
    }
    if (!anyBuilderFixes) {
      return ImmutableList.of(simpleFix.build());
    }
    return ImmutableList.of(
        simpleFix.build(),
        fixWithBuilders
            .setShortDescription(
                "Fix using builders. Warning: this may change behaviour "
                    + "if duplicate keys are added to ImmutableMap.Builder.")
            .build());
  }

  private static SuggestedFix applySimpleFix(
      String immutableType, ExpressionTree expression, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        expression,
        String.format(
            "%s.copyOf(%s)",
            qualifyType(state, fix, immutableType), state.getSourceForNode(expression)));
    return fix.build();
  }

  private static final class ReturnTypeFixer extends TreePathScanner<Void, Void> {
    private final Symbol symbol;
    private final TypeDetails details;
    private final VisitorState state;
    private final SuggestedFix.Builder fix = SuggestedFix.builder();
    private boolean builderifiedVariable = false;
    private boolean failed = false;

    private ReturnTypeFixer(Symbol symbol, TypeDetails details, VisitorState state) {
      this.symbol = symbol;
      this.details = details;
      this.state = state;
    }

    @Override
    public Void visitVariable(VariableTree variableTree, Void unused) {
      if (!getSymbol(variableTree).equals(symbol)) {
        return super.visitVariable(variableTree, null);
      }
      if (variableTree.getInitializer() == null
          || !EMPTY_INITIALIZER.matches(variableTree.getInitializer(), state)
          || details.skipTypes().matches(variableTree.getInitializer(), state)) {
        failed = true;
        return null;
      }

      Tree erasedType = ASTHelpers.getErasedTypeTree(variableTree.getType());
      // don't try to replace synthetic nodes for `var`
      if (ASTHelpers.getStartPosition(erasedType) != -1) {
        fix.replace(erasedType, qualifyType(state, fix, details.builderType()));
      }
      if (variableTree.getInitializer() != null) {
        fix.replace(
            variableTree.getInitializer(),
            qualifyType(state, fix, details.immutableType()) + ".builder()");
      }
      builderifiedVariable = true;
      return super.visitVariable(variableTree, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifier, Void unused) {
      Tree parent = getCurrentPath().getParentPath().getLeaf();
      if (!getSymbol(identifier).equals(symbol)) {
        return null;
      }
      if (parent instanceof VariableTree) {
        VariableTree variable = (VariableTree) parent;
        fix.replace(variable.getType(), qualifyType(state, fix, details.builderType()));
        return null;
      }
      if (parent instanceof MemberSelectTree) {
        Tree grandParent = getCurrentPath().getParentPath().getParentPath().getLeaf();
        if (grandParent instanceof MethodInvocationTree) {
          if (!details.appendMethods().matches((MethodInvocationTree) grandParent, state)) {
            failed = true;
            return null;
          }
        }
        return null;
      }
      if (!builderifiedVariable) {
        failed = true;
        return null;
      }
      if (parent instanceof ReturnTree) {
        fix.postfixWith(identifier, ".build()");
      }
      return null;
    }
  }

  @AutoValue
  abstract static class TypeDetails {
    abstract String immutableType();

    abstract String builderType();

    abstract Matcher<ExpressionTree> appendMethods();

    abstract Matcher<Tree> skipTypes();

    static TypeDetails of(
        String immutableType, Matcher<ExpressionTree> appendMethods, Matcher<Tree> skipTypes) {
      return new AutoValue_MixedMutabilityReturnType_TypeDetails(
          immutableType, immutableType + ".Builder", appendMethods, skipTypes);
    }
  }
}
