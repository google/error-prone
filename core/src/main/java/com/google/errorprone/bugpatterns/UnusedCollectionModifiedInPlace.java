/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "UnusedCollectionModifiedInPlace",
  summary = "Collection is modified in place, but the result is not used",
  category = JDK,
  severity = ERROR
)
public class UnusedCollectionModifiedInPlace extends BugChecker
    implements MethodInvocationTreeMatcher {
  /**
   * Matches "destructive" methods in java.util.Collections, which modify their first argument in
   * place and return void.
   */
  private static final Matcher<MethodInvocationTree> COLLECTIONS_DESTRUCTIVE =
      anyOf(
          staticMethod().onClass("java.util.Collections").named("copy"),
          staticMethod().onClass("java.util.Collections").named("fill"),
          staticMethod().onClass("java.util.Collections").named("reverse"),
          staticMethod().onClass("java.util.Collections").named("rotate"),
          staticMethod().onClass("java.util.Collections").named("shuffle"),
          staticMethod().onClass("java.util.Collections").named("sort"),
          staticMethod().onClass("java.util.Collections").named("swap"));

  /**
   * Matches cases where the first argument to the method call constructs a new list, but the result
   * is not assigned.
   */
  private static final Matcher<MethodInvocationTree> FIRST_ARG_CONSTRUCTS_NEW_LIST =
      argument(
          0,
          anyOf(
              staticMethod().onClass("com.google.common.collect.Lists").named("newArrayList"),
              staticMethod().onClass("com.google.common.collect.Lists").named("newLinkedList"),
              kindIs(Kind.NEW_CLASS)));

  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(COLLECTIONS_DESTRUCTIVE, FIRST_ARG_CONSTRUCTS_NEW_LIST);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree);
  }
}
