/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.base.Enums.getIfPresent;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Multimaps.asMap;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.INSTANCE_EQUALS;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.lang.String.format;
import static java.util.EnumSet.allOf;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Stream;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "Prefer instanceof over getKind() checks where possible, as these work well with pattern"
            + " matching instanceofs",
    severity = SeverityLevel.WARNING)
public final class PreferInstanceofOverGetKind extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> GET_KIND =
      Matchers.instanceMethod().onDescendantOf(Tree.class.getName()).named("getKind");

  /**
   * A map from {@link Kind} to the {@link Class} of tree it denotes, iff that class unambiguously
   * identifies the {@link Kind}. For example, {@link Kind#EQUAL_TO} and {@link Kind#NOT_EQUAL_TO}
   * both map to {@link BinaryTree}, so are not represented here.
   */
  private static final ImmutableMap<Kind, Class<?>> KIND_TO_CLASS =
      asMap(
              allOf(Kind.class).stream()
                  .filter(k -> k.asInterface() != null)
                  .collect(toImmutableListMultimap(k -> k.asInterface(), k -> k)))
          .entrySet()
          .stream()
          .flatMap(
              e ->
                  e.getValue().size() == 1
                      ? Stream.of(new SimpleEntry<>(getOnlyElement(e.getValue()), e.getKey()))
                      : Stream.empty())
          .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO -> {}
      default -> {
        return NO_MATCH;
      }
    }
    return getDescription(
        tree,
        tree.getLeftOperand(),
        tree.getRightOperand(),
        tree.getKind() == Kind.NOT_EQUAL_TO,
        state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!INSTANCE_EQUALS.matches(tree, state)) {
      return NO_MATCH;
    }
    var receiver = getReceiver(tree);
    if (receiver == null) {
      return NO_MATCH;
    }
    var argument = getOnlyElement(tree.getArguments());
    var negated = state.getPath().getParentPath().getLeaf().getKind() == Kind.LOGICAL_COMPLEMENT;
    return getDescription(
        negated ? state.getPath().getParentPath().getLeaf() : tree,
        receiver,
        argument,
        negated,
        state);
  }

  private Description getDescription(
      Tree tree, ExpressionTree left, ExpressionTree right, boolean negated, VisitorState state) {
    if (!GET_KIND.matches(left, state)) {
      return NO_MATCH;
    }
    var rhsSymbol = getSymbol(right);
    if (rhsSymbol == null
        || !rhsSymbol.owner.equals(state.getSymbolFromString(Kind.class.getName()))) {
      return NO_MATCH;
    }
    Kind kind = getIfPresent(Kind.class, rhsSymbol.getSimpleName().toString()).orNull();
    if (kind == null) {
      return NO_MATCH;
    }
    Class<?> treeClass = KIND_TO_CLASS.get(kind);
    if (treeClass == null) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String name = SuggestedFixes.qualifyType(state, fix, treeClass.getName());
    String replacement =
        format("%s instanceof %s", state.getSourceForNode(getReceiver(left)), name);
    return describeMatch(
        tree, fix.replace(tree, !negated ? replacement : format("!(%s)", replacement)).build());
  }
}
