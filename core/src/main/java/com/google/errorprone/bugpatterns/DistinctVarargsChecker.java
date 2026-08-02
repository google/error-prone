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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE_ARRAY;
import static com.google.errorprone.suppliers.Suppliers.arrayOf;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;

import com.google.common.collect.ImmutableListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ErrorProne checker to generate warning when method expecting distinct varargs is invoked with
 * same variable argument.
 */
@BugPattern(summary = "Method expects distinct arguments at some/all positions", severity = WARNING)
public final class DistinctVarargsChecker extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> IMMUTABLE_SET_VARARGS_MATCHER =
      anyOf(
          staticMethod().onClass("java.util.Set").named("of"),
          staticMethod().onClass("com.google.common.collect.ImmutableSet").named("of"),
          staticMethod().onClass("com.google.common.collect.ImmutableSortedSet").named("of"));
  private static final Matcher<ExpressionTree> ALL_DISTINCT_ARG_MATCHER =
      anyOf(
          // JDK Utilities
          staticMethod().onClass("java.lang.Math").namedAnyOf("max", "min"),
          staticMethod().onClass("java.lang.StrictMath").namedAnyOf("max", "min"),
          staticMethod()
              .onClassAny(
                  "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Float")
              .namedAnyOf("max", "min"),
          staticMethod()
              .onClass("java.nio.file.Files")
              .namedAnyOf("copy", "move", "isSameFile", "mismatch", "write", "writeString"),
          staticMethod().onClass("java.util.Collections").named("disjoint"),
          staticMethod().onClass("java.util.EnumSet").named("of"),
          staticMethod().onClass("java.util.Objects").namedAnyOf("hash", "requireNonNullElse"),
          staticMethod()
              .onClass("java.util.concurrent.CompletableFuture")
              .namedAnyOf("allOf", "anyOf"),
          staticMethod()
              .onClassAny(
                  "java.util.stream.Stream",
                  "java.util.stream.IntStream",
                  "java.util.stream.LongStream",
                  "java.util.stream.DoubleStream")
              .named("concat"),

          // Guava Utilities
          staticMethod().onClass("com.google.common.base.MoreObjects").named("firstNonNull"),
          staticMethod().onClass("com.google.common.base.Objects").named("hashCode"),
          staticMethod().onClass("com.google.common.base.Predicates").namedAnyOf("and", "or"),
          staticMethod().onClass("com.google.common.collect.Comparators").namedAnyOf("min", "max"),
          staticMethod().onClass("com.google.common.collect.Iterables").named("concat"),
          staticMethod().onClass("com.google.common.collect.Maps").named("difference"),
          staticMethod()
              .onClass("com.google.common.collect.Ordering")
              .named("explicit")
              .withParametersOfType(OBJECT_TYPE, OBJECT_TYPE_ARRAY),
          staticMethod()
              .onClass("com.google.common.collect.Sets")
              .namedAnyOf("intersection", "union", "difference", "symmetricDifference"),
          staticMethod()
              .onClass("com.google.common.util.concurrent.Futures")
              .namedAnyOf("whenAllSucceed", "whenAllComplete")
              .withParametersOfType(
                  arrayOf(typeFromString("com.google.common.util.concurrent.ListenableFuture"))),
          staticMethod()
              .onClassAny(
                  "com.google.common.primitives.Ints",
                  "com.google.common.primitives.Longs",
                  "com.google.common.primitives.Doubles",
                  "com.google.common.primitives.Floats",
                  "com.google.common.primitives.Shorts",
                  "com.google.common.primitives.Chars",
                  "com.google.common.primitives.SignedBytes",
                  "com.google.common.primitives.UnsignedBytes")
              .namedAnyOf("max", "min"),

          // Protobuf Utilities
          staticMethod()
              .onClass("com.google.protobuf.util.FieldMaskUtil")
              .namedAnyOf("union", "intersection", "subtract"),

          // ErrorProne Matchers
          staticMethod()
              .onClass("com.google.errorprone.matchers.Matchers")
              .namedAnyOf("anyOf", "allOf"),
          instanceMethod()
              .onDescendantOf(
                  "com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher")
              .named("namedAnyOf"),
          instanceMethod()
              .onDescendantOfAny(
                  "com.google.errorprone.matchers.method.MethodMatchers.StaticMethodMatcher",
                  "com.google.errorprone.matchers.method.MethodMatchers.InstanceMethodMatcher",
                  "com.google.errorprone.matchers.method.MethodMatchers.AnyMethodMatcher")
              .namedAnyOf("onClassAny", "onExactClassAny", "onDescendantOfAny"),

          // Testing Frameworks
          staticMethod()
              .onClass("org.mockito.Mockito")
              .namedAnyOf(
                  "verifyNoInteractions", "verifyNoMoreInteractions", "inOrder", "ignoreStubs"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .namedAnyOf("isAnyOf", "isNoneOf"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.IterableSubject")
              .named("containsAnyOf"));
  private static final Matcher<ExpressionTree> EVEN_PARITY_DISTINCT_ARG_MATCHER =
      anyOf(
          // ImmutableMap.of is covered by AlwaysThrows.
          staticMethod().onClass("com.google.common.collect.ImmutableSortedMap").named("of"),
          staticMethod().onClass("java.util.Map").named("of"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.MapSubject")
              .named("containsExactly"));
  private static final Matcher<ExpressionTree> EVEN_AND_ODD_PARITY_DISTINCT_ARG_MATCHER =
      staticMethod().onClass("com.google.common.collect.ImmutableBiMap").named("of");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    /*
     * For set construction, a fix can be constructed. TODO(cpovirk): Also generate a fix for the
     * Futures methods.
     *
     * For other methods, we'd often need the user's judgment. TODO(cpovirk): Generate multiple
     * fixes for those—or one fix for map construction if the values for a given key all match.
     */
    if (IMMUTABLE_SET_VARARGS_MATCHER.matches(tree, state)) {
      return checkDistinctArgumentsWithFix(tree, state);
    }
    if (ALL_DISTINCT_ARG_MATCHER.matches(tree, state)) {
      return checkDistinctArguments(state, tree.getArguments());
    }
    if (EVEN_PARITY_DISTINCT_ARG_MATCHER.matches(tree, state)) {
      List<ExpressionTree> arguments = new ArrayList<>();
      for (int index = 0; index < tree.getArguments().size(); index += 2) {
        arguments.add(tree.getArguments().get(index));
      }
      return checkDistinctArguments(state, arguments);
    }
    if (EVEN_AND_ODD_PARITY_DISTINCT_ARG_MATCHER.matches(tree, state)) {
      List<ExpressionTree> evenParityArguments = new ArrayList<>();
      List<ExpressionTree> oddParityArguments = new ArrayList<>();
      for (int index = 0; index < tree.getArguments().size(); index++) {
        if (index % 2 == 0) {
          evenParityArguments.add(tree.getArguments().get(index));
        } else {
          oddParityArguments.add(tree.getArguments().get(index));
        }
      }
      return checkDistinctArguments(state, evenParityArguments, oddParityArguments);
    }
    return Description.NO_MATCH;
  }

  private static ImmutableListMultimap<String, Integer> argumentsByString(
      VisitorState state, List<? extends ExpressionTree> arguments) {
    ImmutableListMultimap.Builder<String, Integer> result = ImmutableListMultimap.builder();
    for (int i = 0; i < arguments.size(); i++) {
      result.put(state.getSourceForNode(arguments.get(i)), i);
    }
    return result.build();
  }

  private Description checkDistinctArgumentsWithFix(MethodInvocationTree tree, VisitorState state) {
    SuggestedFix.Builder suggestedFix = SuggestedFix.builder();
    List<? extends ExpressionTree> arguments = tree.getArguments();
    ImmutableListMultimap<String, Integer> argumentsByString = argumentsByString(state, arguments);
    for (Map.Entry<String, Collection<Integer>> entry : argumentsByString.asMap().entrySet()) {
      entry.getValue().stream()
          .skip(1)
          .forEachOrdered(
              index ->
                  suggestedFix.merge(
                      SuggestedFix.replace(
                          state.getEndPosition(arguments.get(index - 1)),
                          state.getEndPosition(arguments.get(index)),
                          "")));
    }
    if (suggestedFix.isEmpty()) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, suggestedFix.build());
  }

  private Description checkDistinctArguments(
      VisitorState state, List<? extends ExpressionTree>... argumentsList) {
    for (List<? extends ExpressionTree> arguments : argumentsList) {
      ImmutableListMultimap<String, Integer> argumentsByString =
          argumentsByString(state, arguments);
      for (Map.Entry<String, Collection<Integer>> entry : argumentsByString.asMap().entrySet()) {
        entry.getValue().stream()
            .skip(1)
            .forEachOrdered(index -> state.reportMatch(describeMatch(arguments.get(index))));
      }
    }
    return Description.NO_MATCH;
  }
}
