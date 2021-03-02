/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotationWithSimpleName;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.lang.model.element.Modifier;

/** Refactoring to suggest Immutable types for member collection that are not mutated. */
@BugPattern(
    name = "ImmutableMemberCollection",
    summary = "If you don't intend to mutate a member collection prefer using Immutable types.",
    severity = SUGGESTION)
public final class ImmutableMemberCollection extends BugChecker implements ClassTreeMatcher {

  private static final ImmutableSet<ReplaceableType<?>> REPLACEABLE_TYPES =
      ImmutableSet.of(
          ReplaceableType.create(SortedSet.class, ImmutableSortedSet.class),
          ReplaceableType.create(NavigableSet.class, ImmutableSortedSet.class),
          ReplaceableType.create(Set.class, ImmutableSet.class),
          ReplaceableType.create(List.class, ImmutableList.class),
          ReplaceableType.create(ListMultimap.class, ImmutableListMultimap.class),
          ReplaceableType.create(SetMultimap.class, ImmutableSetMultimap.class),
          ReplaceableType.create(SortedMap.class, ImmutableSortedMap.class),
          ReplaceableType.create(Map.class, ImmutableMap.class));

  private static final Matcher<Tree> PRIVATE_FINAL_VAR_MATCHER =
      allOf(kindIs(Kind.VARIABLE), hasModifier(Modifier.PRIVATE), hasModifier(Modifier.FINAL));

  // TODO(ashishkedia) : Share this with ImmutableSetForContains.
  private static final Matcher<Tree> EXCLUSIONS =
      anyOf(
          hasAnnotationWithSimpleName("Bind"),
          hasAnnotationWithSimpleName("Inject"));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    ImmutableMap<Symbol, ReplaceableVar> replaceableVars =
        classTree.getMembers().stream()
            .filter(member -> PRIVATE_FINAL_VAR_MATCHER.matches(member, state))
            .filter(member -> !EXCLUSIONS.matches(member, state))
            .map(VariableTree.class::cast)
            .flatMap(varTree -> stream(isReplaceable(varTree, state)))
            .collect(toImmutableMap(ReplaceableVar::symbol, var -> var));
    if (replaceableVars.isEmpty()) {
      return Description.NO_MATCH;
    }
    new TreeScanner<Void, VisitorState>() {
      @Override
      public Void visitAssignment(AssignmentTree assignmentTree, VisitorState visitorState) {
        Symbol varSymbol = getSymbol(assignmentTree.getVariable());
        if (replaceableVars.containsKey(varSymbol) && assignmentTree.getExpression() != null) {
          replaceableVars.get(varSymbol).initTrees().add(assignmentTree.getExpression());
        }
        return super.visitAssignment(assignmentTree, visitorState);
      }
    }.scan(classTree, state);

    SuggestedFix.Builder suggestedFix = SuggestedFix.builder();
    // TODO(ashishkedia) : Expand to non-immutable init classTree, but then also scan all usages
    // and look for any potential mutation.
    replaceableVars.values().stream()
        .filter(replaceableVar -> replaceableVar.areAllInitImmutable(state))
        .forEach(replaceableVar -> suggestedFix.merge(replaceableVar.getFix()));
    if (suggestedFix.isEmpty()) {
      return Description.NO_MATCH;
    }
    return describeMatch(classTree, suggestedFix.build());
  }

  private static Optional<ReplaceableVar> isReplaceable(VariableTree tree, VisitorState state) {
    return REPLACEABLE_TYPES.stream()
        .filter(type -> isSameType(type.interfaceType()).matches(tree, state))
        .findFirst()
        .map(type -> ReplaceableVar.create(tree, type));
  }

  @AutoValue
  abstract static class ReplaceableType<M> {
    abstract Class<M> interfaceType();

    abstract Class<? extends M> immutableType();

    static <M> ReplaceableType<M> create(Class<M> interfaceType, Class<? extends M> immutableType) {
      return new AutoValue_ImmutableMemberCollection_ReplaceableType<>(
          interfaceType, immutableType);
    }
  }

  @AutoValue
  abstract static class ReplaceableVar {
    abstract Symbol symbol();

    abstract ReplaceableType<?> type();

    abstract ArrayList<Tree> initTrees();

    abstract Tree declaredType();

    static ReplaceableVar create(VariableTree variableTree, ReplaceableType<?> type) {
      ReplaceableVar replaceableVar =
          new AutoValue_ImmutableMemberCollection_ReplaceableVar(
              getSymbol(variableTree), type, new ArrayList<>(), variableTree.getType());
      if (variableTree.getInitializer() != null) {
        replaceableVar.initTrees().add(variableTree.getInitializer());
      }
      return replaceableVar;
    }

    private boolean areAllInitImmutable(VisitorState state) {
      return initTrees().stream()
          .allMatch(initTree -> isSameType(type().immutableType()).matches(initTree, state));
    }

    private SuggestedFix getFix() {
      return SuggestedFix.builder()
          .replace(stripTypeParameters(declaredType()), type().immutableType().getSimpleName())
          .addImport(type().immutableType().getName())
          .build();
    }

    private static Tree stripTypeParameters(Tree tree) {
      return tree.getKind().equals(Kind.PARAMETERIZED_TYPE)
          ? ((ParameterizedTypeTree) tree).getType()
          : tree;
    }
  }
}
