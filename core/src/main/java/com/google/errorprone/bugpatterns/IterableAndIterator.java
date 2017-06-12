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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.util.Iterator;
import java.util.List;

/** @author jsjeon@google.com (Jinseong Jeon) */
@BugPattern(
  name = "IterableAndIterator",
  summary = "Class should not implement both `Iterable` and `Iterator`",
  explanation =
      "An `Iterator` is a *state-ful* instance that enables you to check "
          + "whether it has more elements (via `hasNext()`) "
          + "and moves to the next one if any (via `next()`), "
          + "while an `Iterable` is a representation of literally iterable elements. "
          + "An `Iterable` can generate multiple valid `Iterator`s, though.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class IterableAndIterator extends BugChecker implements ClassTreeMatcher {

  private static final String ITERABLE = Iterable.class.getCanonicalName();
  private static final String ITERATOR = Iterator.class.getCanonicalName();

  /** Matches if a class/interface is subtype of Iterable */
  private static final Matcher<Tree> ITERABLE_MATCHER = isSubtypeOf(ITERABLE);

  /** Matches if a class/interface is subtype of Iterator */
  private static final Matcher<Tree> ITERATOR_MATCHER = isSubtypeOf(ITERATOR);

  /** Matches if a class/interface is subtype of Iterable _and_ Iterator */
  private static final Matcher<Tree> ITERABLE_AND_ITERATOR_MATCHER =
      allOf(ITERABLE_MATCHER, ITERATOR_MATCHER);

  private boolean matchAnySuperType(ClassTree tree, VisitorState state) {
    List<Tree> superTypes = Lists.<Tree>newArrayList(tree.getImplementsClause());
    Tree superClass = tree.getExtendsClause();
    if (superClass != null) {
      superTypes.add(superClass);
    }

    /* NOTE: at "Eight day", use Java 8 feature below
    return superTypes.stream()
        .anyMatch(superType -> ITERABLE_AND_ITERATOR_MATCHER.matches(superType, state));
     */
    for (Tree superType : superTypes) {
      if (ITERABLE_AND_ITERATOR_MATCHER.matches(superType, state)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (ITERABLE_AND_ITERATOR_MATCHER.matches(tree, state)) {
      // Filter out inherited case to not warn again
      if (matchAnySuperType(tree, state)) {
        return Description.NO_MATCH;
      }

      // TODO: Distinguish direct implementation and indirect cases

      // TODO: Suggest removing Iterable or Iterator, along with implemented methods
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
