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
import javax.annotation.Nullable;

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
          new MethodArgMatcher("java.util.Collection", "contains(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Collection", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Deque", "removeFirstOccurrence(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Deque", "removeLastOccurrence(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Dictionary", "get(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Dictionary", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.List", "indexOf(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.List", "lastIndexOf(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "containsKey(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "containsValue(java.lang.Object)", 1, 0),
          new MethodArgMatcher("java.util.Map", "get(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Map", "getOrDefault(java.lang.Object,V)", 0, 0),
          new MethodArgMatcher("java.util.Map", "remove(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Stack", "search(java.lang.Object)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "indexOf(java.lang.Object,int)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "lastIndexOf(java.lang.Object,int)", 0, 0),
          new MethodArgMatcher("java.util.Vector", "removeElement(java.lang.Object)", 0, 0));

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
              "containsAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0), // index of the method argument's type argument to extract
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              "removeAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0), // index of the method argument's type argument to extract
          new TypeArgOfMethodArgMatcher(
              "java.util.Collection", // class that defines the method
              "retainAll(java.util.Collection<?>)", // method signature
              0, // index of the owning class's type argument to extract
              0, // index of the method argument whose type argument to extract
              "java.util.Collection", // type of the method argument
              0)); // index of the method argument's type argument to extract

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

  @Nullable
  public static MatchResult firstNonNullMatchResult(ExpressionTree tree, VisitorState state) {
    if (!FIRST_ORDER_MATCHER.matches(tree, state)) {
      return null;
    }

    for (AbstractCollectionIncompatibleTypeMatcher matcher : ContainmentMatchers.ALL_MATCHERS) {
      MatchResult result = matcher.matches(tree, state);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private ContainmentMatchers() {}
}
