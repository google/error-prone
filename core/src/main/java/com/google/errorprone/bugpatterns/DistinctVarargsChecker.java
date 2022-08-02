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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

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
          staticMethod().onClass("com.google.common.collect.ImmutableSet").named("of"),
          staticMethod().onClass("com.google.common.collect.ImmutableSortedSet").named("of"));
  private static final Matcher<ExpressionTree> ALL_DISTINCT_ARG_MATCHER =
      anyOf(
          staticMethod()
              .onClass("com.google.common.util.concurrent.Futures")
              .withSignature(
                  "<V>whenAllSucceed(com.google.common.util.concurrent.ListenableFuture<? extends"
                      + " V>...)"),
          staticMethod()
              .onClass("com.google.common.util.concurrent.Futures")
              .withSignature(
                  "<V>whenAllComplete(com.google.common.util.concurrent.ListenableFuture<? extends"
                      + " V>...)"),
          staticMethod()
              .onClass("com.google.common.collect.Ordering")
              .withSignature("<T>explicit(T,T...)"));
  private static final Matcher<ExpressionTree> EVEN_PARITY_DISTINCT_ARG_MATCHER =
      staticMethod().onClass("com.google.common.collect.ImmutableSortedMap").named("of");
  private static final Matcher<ExpressionTree> EVEN_AND_ODD_PARITY_DISTINCT_ARG_MATCHER =
      staticMethod().onClass("com.google.common.collect.ImmutableBiMap").named("of");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // For ImmutableSet and ImmutableSortedSet fix can be constructed. For all other methods,
    // non-distinct arguments will result in the runtime exceptions.
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
