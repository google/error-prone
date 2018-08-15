/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Check for usage of {@code Set<T>} or {@code Map<T, E>}.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
public abstract class AbstractAsKeyOfSetOrMap extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTRUCTS_SET =
      anyOf(
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.Sets")
              .named("newHashSet"),
          Matchers.constructor().forClass("java.util.HashSet"));

  private static final Matcher<ExpressionTree> CONSTRUCTS_MULTISET =
      anyOf(
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.HashMultiset")
              .named("create")
              .withParameters(),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.LinkedHashMultiset")
              .named("create"));

  private static final Matcher<ExpressionTree> CONSTRUCTS_MAP =
      anyOf(
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.Maps")
              .named("newHashMap"),
          Matchers.constructor().forClass("java.util.HashMap"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.Maps")
              .named("newLinkedHashMap"),
          Matchers.constructor().forClass("java.util.LinkedHashMap"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.HashBiMap")
              .named("create"));

  private static final Matcher<ExpressionTree> CONSTRUCTS_MULTIMAP =
      anyOf(
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.HashMultimap")
              .named("create"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.LinkedHashMultimap")
              .named("create"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.ArrayListMultimap")
              .named("create"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.LinkedListMultimap")
              .named("create"));

  private static final Matcher<ExpressionTree> CONSTRUCTS_SET_OR_MAP =
      anyOf(CONSTRUCTS_SET, CONSTRUCTS_MAP, CONSTRUCTS_MULTIMAP, CONSTRUCTS_MULTISET);

  protected abstract boolean isBadType(Type type, VisitorState state);

  private Description matchType(ExpressionTree tree, VisitorState state) {
    if (CONSTRUCTS_SET_OR_MAP.matches(tree, state)) {
      List<Type> argumentTypes = ASTHelpers.getResultType(tree).getTypeArguments();
      if (argumentTypes.isEmpty()) {
        return Description.NO_MATCH;
      }
      Type typeArg = argumentTypes.get(0);
      if (isBadType(typeArg, state)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchType(tree, state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchType(tree, state);
  }
}
