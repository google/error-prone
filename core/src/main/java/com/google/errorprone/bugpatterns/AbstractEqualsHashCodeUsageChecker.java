/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.fixes.SuggestedFix.emptyFix;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.assertEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.assertNotEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.bugpatterns.collectionincompatibletype.ContainmentMatchers;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// A base class for checks that flag code relying on `equals` or `hashCode` of particular types.
///
/// This class finds the places where `equals` and `hashCode` are used, either explicitly or
/// implicitly by collections, and subclasses decide which types should not be used in which ways.
public abstract class AbstractEqualsHashCodeUsageChecker extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher, NewClassTreeMatcher {

  /// A way that code can rely on the `equals` or `hashCode` of a type.
  protected enum Usage {
    /// A direct call to `equals`, e.g. `a.equals(b)` or `Objects.equals(a, b)`.
    EQUALS,
    /// An equality assertion, e.g. `assertEquals(a, b)` or Truth's `isEqualTo`.
    ASSERTION,
    /// A method that compares elements with `equals`, e.g. `Collection.contains`.
    CONTAINMENT,
    /// Creating a hash-based container, e.g. a `Set` element or a `Map`, `Multimap`, or `Multiset`
    /// key, or a `BiMap` value.
    HASH_KEY,
  }

  /// Returns a diagnostic message if code should not rely on `type`'s `equals` or `hashCode` in the
  /// given way.
  protected abstract Optional<String> problem(Type type, Usage usage, VisitorState state);

  /// Returns a fix for an [Usage#EQUALS] or [Usage#ASSERTION] finding.
  protected Optional<SuggestedFix> equalityFix(
      MethodInvocationTree tree, Tree receiver, Tree argument, VisitorState state) {
    return Optional.empty();
  }

  @Override
  public final Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Description description = checkEquality(tree, state);
    if (description != NO_MATCH) {
      return description;
    }
    description = checkContainment(tree, state);
    if (description != NO_MATCH) {
      return description;
    }
    if (HASH_CONTAINER_FACTORIES.matches(tree, state)) {
      return checkHashKeys(tree, state);
    }
    return NO_MATCH;
  }

  @Override
  public final Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return checkContainment(tree, state);
  }

  @Override
  public final Description matchNewClass(NewClassTree tree, VisitorState state) {
    return checkHashKeys(tree, state);
  }

  private Description checkEquality(MethodInvocationTree tree, VisitorState state) {
    Tree receiver;
    Tree argument;
    Usage usage;
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (staticEqualsInvocation().matches(tree, state)) {
      receiver = arguments.get(arguments.size() - 2);
      argument = getLast(arguments);
      usage = Usage.EQUALS;
    } else if (instanceEqualsInvocation().matches(tree, state)) {
      receiver = getReceiver(tree);
      argument = arguments.getFirst();
      usage = Usage.EQUALS;
    } else if (assertEqualsInvocation().matches(tree, state)
        || assertNotEqualsInvocation().matches(tree, state)) {
      receiver = arguments.get(arguments.size() - 2);
      argument = getLast(arguments);
      usage = Usage.ASSERTION;
    } else if (ASSERT_THAT_EQUALS.matches(tree, state)) {
      receiver = getOnlyElement(arguments);
      argument = getOnlyElement(((MethodInvocationTree) getReceiver(tree)).getArguments());
      usage = Usage.ASSERTION;
    } else {
      return NO_MATCH;
    }
    return findProblem(getType(receiver), usage, state)
        .or(() -> findProblem(getType(argument), usage, state))
        .map(
            message ->
                buildDescription(tree)
                    .setMessage(message)
                    .addFix(equalityFix(tree, receiver, argument, state).orElse(emptyFix()))
                    .build())
        .orElse(NO_MATCH);
  }

  private Description checkContainment(ExpressionTree tree, VisitorState state) {
    MatchResult result = ContainmentMatchers.firstNonNullMatchResult(tree, state);
    if (result == null || CONTAINERS_NOT_USING_EQUALS.matches(tree, state)) {
      return NO_MATCH;
    }
    return findProblem(result.sourceType(), Usage.CONTAINMENT, state)
        .or(() -> findProblem(result.targetType(), Usage.CONTAINMENT, state))
        .map(message -> buildDescription(tree).setMessage(message).build())
        .orElse(NO_MATCH);
  }

  private Description checkHashKeys(ExpressionTree tree, VisitorState state) {
    for (Type keyType : hashKeyTypes(getType(tree), state)) {
      Optional<String> message = findProblem(keyType, Usage.HASH_KEY, state);
      if (message.isPresent()) {
        return buildDescription(tree).setMessage(message.get()).build();
      }
    }
    return NO_MATCH;
  }

  private Optional<String> findProblem(@Nullable Type type, Usage usage, VisitorState state) {
    if (type == null || type.hasTag(TypeTag.BOT)) {
      return Optional.empty();
    }
    return problem(type, usage, state);
  }

  /// Returns the types whose `equals` and `hashCode` are used by a container of the given type, or
  /// an empty list if the type isn't a hash-based container.
  private static ImmutableList<Type> hashKeyTypes(@Nullable Type type, VisitorState state) {
    ImmutableList.Builder<Type> result = ImmutableList.builder();
    if (isSubtype(type, MAP.get(state), state)) {
      if (!isSubtype(type, SORTED_MAP.get(state), state)
          && !isSubtype(type, IDENTITY_HASH_MAP.get(state), state)) {
        addTypeArgument(result, type, MAP.get(state), 0, state);
        // If `type` does not inherit from `BiMap`, this does nothing.
        addTypeArgument(result, type, BIMAP.get(state), 1, state);
      }
    } else if (isSubtype(type, SET.get(state), state)) {
      if (!isSubtype(type, SORTED_SET.get(state), state)) {
        addTypeArgument(result, type, SET.get(state), 0, state);
      }
    } else if (isSubtype(type, MULTISET.get(state), state)) {
      if (!isSubtype(type, SORTED_MULTISET.get(state), state)) {
        addTypeArgument(result, type, MULTISET.get(state), 0, state);
      }
    } else if (isSubtype(type, MULTIMAP.get(state), state)) {
      addTypeArgument(result, type, MULTIMAP.get(state), 0, state);
    }
    return result.build();
  }

  private static void addTypeArgument(
      ImmutableList.Builder<Type> result,
      Type type,
      @Nullable Type superType,
      int index,
      VisitorState state) {
    if (superType == null) {
      return;
    }
    Type asSuper = state.getTypes().asSuper(type, superType.tsym);
    if (asSuper == null || asSuper.getTypeArguments().size() <= index) {
      return;
    }
    result.add(asSuper.getTypeArguments().get(index));
  }

  private static final Matcher<MethodInvocationTree> ASSERT_THAT_EQUALS =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .namedAnyOf("isEqualTo", "isNotEqualTo"),
          receiverOfInvocation(
              anyOf(
                  staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
                  instanceMethod()
                      .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                      .named("that"))));

  // NOTE: this is a bit crude; methods like `containsValue` are still likely to be subject to
  // equality constraints on the value type.
  private static final Matcher<ExpressionTree> CONTAINERS_NOT_USING_EQUALS =
      anyMethod()
          .onDescendantOfAny(
              "java.util.IdentityHashMap", "java.util.SortedMap", "java.util.SortedSet");

  private static final Matcher<ExpressionTree> HASH_CONTAINER_FACTORIES =
      anyOf(
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.ImmutableSet",
                  "com.google.common.collect.ImmutableMap",
                  "com.google.common.collect.HashBiMap"),
          instanceMethod()
              .onExactClassAny(
                  "com.google.common.collect.ImmutableSet.Builder",
                  "com.google.common.collect.ImmutableMap.Builder")
              .named("build"),
          staticMethod().onClass("com.google.common.collect.Sets").named("newHashSet"),
          staticMethod()
              .onClass("com.google.common.collect.Maps")
              .namedAnyOf("newHashMap", "newLinkedHashMap"),
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.HashMultiset",
                  "com.google.common.collect.LinkedHashMultiset",
                  "com.google.common.collect.HashMultimap",
                  "com.google.common.collect.LinkedHashMultimap",
                  "com.google.common.collect.ArrayListMultimap",
                  "com.google.common.collect.LinkedListMultimap")
              .named("create"),
          instanceMethod().onDescendantOf("java.util.stream.Stream").named("collect"));

  private static final Supplier<Type> SET = typeFromString("java.util.Set");
  private static final Supplier<Type> SORTED_SET = typeFromString("java.util.SortedSet");
  private static final Supplier<Type> MAP = typeFromString("java.util.Map");
  private static final Supplier<Type> SORTED_MAP = typeFromString("java.util.SortedMap");
  private static final Supplier<Type> IDENTITY_HASH_MAP =
      typeFromString("java.util.IdentityHashMap");
  private static final Supplier<Type> BIMAP = typeFromString("com.google.common.collect.BiMap");
  private static final Supplier<Type> MULTIMAP =
      typeFromString("com.google.common.collect.Multimap");
  private static final Supplier<Type> MULTISET =
      typeFromString("com.google.common.collect.Multiset");
  private static final Supplier<Type> SORTED_MULTISET =
      typeFromString("com.google.common.collect.SortedMultiset");
}
