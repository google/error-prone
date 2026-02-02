/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import org.jspecify.annotations.Nullable;

/** Matchers for methods which express containment, like {@link java.util.Collection#contains}. */
public final class ContainmentMatchers {

  /**
   * The least-common ancestor of all of the types of {@link #DIRECT_MATCHERS} and {@link
   * #TYPE_ARG_MATCHERS}.
   */
  private static final Matcher<ExpressionTree> FIRST_ORDER_MATCHER =
      Matchers.anyMethod()
          .onDescendantOfAny(
              "java.util.Collection",
              "java.util.Dictionary",
              "java.util.Map",
              "java.util.Collections",
              "com.google.common.collect.Sets");

  /** The "normal" case of extracting the type of a method argument */
  private static final ImmutableList<MethodArgMatcher> DIRECT_MATCHERS =
      ImmutableList.of(
          // "Normal" cases, e.g. Collection#remove(Object)
          // Make sure to keep that the type or one of its supertype should be present in
          // FIRST_ORDER_MATCHER
          new MethodArgMatcher("java.util.Collection", 0, 0, "contains", "java.lang.Object"),
          new MethodArgMatcher("java.util.Collection", 0, 0, "remove", "java.lang.Object"),
          new MethodArgMatcher(
              "java.util.Deque", 0, 0, "removeFirstOccurrence", "java.lang.Object"),
          new MethodArgMatcher("java.util.Deque", 0, 0, "removeLastOccurrence", "java.lang.Object"),
          new MethodArgMatcher("java.util.Dictionary", 0, 0, "get", "java.lang.Object"),
          new MethodArgMatcher("java.util.Dictionary", 0, 0, "remove", "java.lang.Object"),
          new MethodArgMatcher("java.util.List", 0, 0, "indexOf", "java.lang.Object"),
          new MethodArgMatcher("java.util.List", 0, 0, "lastIndexOf", "java.lang.Object"),
          new MethodArgMatcher("java.util.Map", 0, 0, "containsKey", "java.lang.Object"),
          new MethodArgMatcher("java.util.Map", 1, 0, "containsValue", "java.lang.Object"),
          new MethodArgMatcher("java.util.Map", 0, 0, "get", "java.lang.Object"),
          new MethodArgMatcher(
              "java.util.Map", 0, 0, "getOrDefault", "java.lang.Object", "java.lang.Object"),
          new MethodArgMatcher("java.util.Map", 0, 0, "remove", "java.lang.Object"),
          new MethodArgMatcher("java.util.Stack", 0, 0, "search", "java.lang.Object"),
          new MethodArgMatcher("java.util.Vector", 0, 0, "indexOf", "java.lang.Object", "int"),
          new MethodArgMatcher("java.util.Vector", 0, 0, "lastIndexOf", "java.lang.Object", "int"),
          new MethodArgMatcher("java.util.Vector", 0, 0, "removeElement", "java.lang.Object"));

  /**
   * Cases where we need to extract the type argument from a method argument, e.g.
   * Collection#containsAll(Collection<?>)
   */
  private static final ImmutableList<TypeArgOfMethodArgMatcher> TYPE_ARG_MATCHERS =
      ImmutableList.of(
          // Make sure to keep that the type or one of its supertype should be present in
          // FIRST_ORDER_MATCHER
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0, // index of the method argument's type argument to extract
              "containsAll", // method name
              "java.util.Collection"), // method parameter
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0, // index of the method argument's type argument to extract
              "removeAll", // method name
              "java.util.Collection"), // method parameter
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0, // index of the method argument's type argument to extract
              "retainAll", // method name
              "java.util.Collection")); // method parameter

  private static final ImmutableList<BinopMatcher> STATIC_MATCHERS =
      ImmutableList.of(
          new BinopMatcher("java.util.Collection", "java.util.Collections", "disjoint"),
          new BinopMatcher("java.util.Set", "com.google.common.collect.Sets", "difference"));

  private static final ImmutableList<AbstractCollectionIncompatibleTypeMatcher> ALL_MATCHERS =
      ImmutableList.<AbstractCollectionIncompatibleTypeMatcher>builder()
          .addAll(DIRECT_MATCHERS)
          .addAll(TYPE_ARG_MATCHERS)
          .addAll(STATIC_MATCHERS)
          .build();

  public static @Nullable MatchResult firstNonNullMatchResult(
      ExpressionTree tree, VisitorState state, boolean useCapture) {
    if (!FIRST_ORDER_MATCHER.matches(tree, state)) {
      return null;
    }

    for (AbstractCollectionIncompatibleTypeMatcher matcher : ContainmentMatchers.ALL_MATCHERS) {
      MatchResult result = matcher.matches(tree, state, useCapture);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /** Backwards compatibility overload for external callers. */
  public static @Nullable MatchResult firstNonNullMatchResult(
      ExpressionTree tree, VisitorState state) {
    return firstNonNullMatchResult(tree, state, /* useCapture= */ true);
  }

  private ContainmentMatchers() {}
}
