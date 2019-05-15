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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ImmutableModification",
    summary =
        "Modifying an immutable collection is guaranteed to throw an exception and leave the"
            + " collection unmodified",
    severity = ERROR)
public class ImmutableModification extends BugChecker implements MethodInvocationTreeMatcher {

  public static final ImmutableMultimap<String, String> ILLEGAL =
      ImmutableMultimap.<String, String>builder()
          .putAll("com.google.common.collect.ImmutableBiMap", "forcePut")
          .putAll("com.google.common.collect.ImmutableClassToInstanceMap", "putInstance")
          .putAll(
              "com.google.common.collect.ImmutableCollection",
              "add",
              "addAll",
              "clear",
              "remove",
              "removeAll",
              "removeIf",
              "retainAll")
          .putAll("com.google.common.collect.ImmutableList", "set", "sort")
          .putAll(
              "com.google.common.collect.ImmutableMap",
              "clear",
              "compute",
              "computeIfAbsent",
              "computeIfPresent",
              "merge",
              "put",
              "putAll",
              "putIfAbsent",
              "remove",
              "replace",
              "replaceAll")
          .putAll(
              "com.google.common.collect.ImmutableMultimap",
              "clear",
              "put",
              "putAll",
              "remove",
              "removeAll",
              "replaceValues")
          .putAll("com.google.common.collect.ImmutableMultiset", "setCount")
          .putAll("com.google.common.collect.ImmutableRangeMap", "clear", "put", "putAll", "remove")
          .putAll(
              "com.google.common.collect.ImmutableRangeSet", "add", "addAll", "remove", "removeAll")
          .putAll("com.google.common.collect.ImmutableSortedMap", "pollFirstEntry", "pollLastEntry")
          .putAll("com.google.common.collect.ImmutableSortedSet", "pollFirst", "pollLast")
          .putAll("com.google.common.collect.ImmutableTable", "clear", "put", "putAll", "remove")
          .putAll("com.google.common.collect.UnmodifiableIterator", "remove")
          .putAll("com.google.common.collect.UnmodifiableListIterator", "add", "set")
          .putAll(
              "com.google.common.collect.Sets.SetView",
              "add",
              "addAll",
              "clear",
              "remove",
              "removeAll",
              "removeIf",
              "retainAll")
          .build();

  static final Matcher<ExpressionTree> MATCHER =
      Matchers.anyOf(
          ILLEGAL.asMap().entrySet().stream()
              .map(e -> instanceMethod().onDescendantOf(e.getKey()).namedAnyOf(e.getValue()))
              .collect(toImmutableList()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return MATCHER.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
  }
}
