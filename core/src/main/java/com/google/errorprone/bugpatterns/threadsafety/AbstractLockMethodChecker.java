/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract implementation of checkers for {@code @LockMethod} and{@code @UnlockMethod}.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public abstract class AbstractLockMethodChecker extends BugChecker
    implements BugChecker.MethodTreeMatcher {

  /**
   * Returns the lock expressions in the {@code @LockMethod}/{@code @UnlockMethod} annotation, if
   * any.
   */
  protected abstract ImmutableList<String> getLockExpressions(MethodTree tree);

  /** Searches the method body for locks that are acquired/released. */
  protected abstract Set<GuardedByExpression> getActual(MethodTree tree, VisitorState state);

  /**
   * Searches the method body for the incorrect lock operation (e.g. releasing a lock in
   * {@code @LockMethod}, or acquiring a lock in {@code @UnlockMethod}).
   */
  protected abstract Set<GuardedByExpression> getUnwanted(MethodTree tree, VisitorState state);

  /** Builds the error message, given the list of locks that were not handled. */
  protected abstract String buildMessage(String unhandled);

  @Override
  public Description matchMethod(MethodTree tree, final VisitorState state) {

    ImmutableList<String> lockExpressions = getLockExpressions(tree);
    if (lockExpressions.isEmpty()) {
      return Description.NO_MATCH;
    }

    Optional<ImmutableSet<GuardedByExpression>> expected =
        parseLockExpressions(lockExpressions, tree, state);
    if (!expected.isPresent()) {
      return buildDescription(tree).setMessage("Could not resolve lock expression.").build();
    }

    Set<GuardedByExpression> unwanted = getUnwanted(tree, state);
    SetView<GuardedByExpression> mishandled = Sets.intersection(expected.get(), unwanted);
    if (!mishandled.isEmpty()) {
      String message = buildMessage(formatLockString(mishandled));
      return buildDescription(tree).setMessage(message).build();
    }

    Set<GuardedByExpression> actual = getActual(tree, state);
    SetView<GuardedByExpression> unhandled = Sets.difference(expected.get(), actual);
    if (!unhandled.isEmpty()) {
      String message = buildMessage(formatLockString(unhandled));
      return buildDescription(tree).setMessage(message).build();
    }

    return Description.NO_MATCH;
  }

  private static String formatLockString(Set<GuardedByExpression> locks) {
    ImmutableList<String> sortedUnhandled =
        FluentIterable.from(locks)
            .transform(Functions.toStringFunction())
            .toSortedList(Ordering.natural());
    return Joiner.on(", ").join(sortedUnhandled);
  }

  private static Optional<ImmutableSet<GuardedByExpression>> parseLockExpressions(
      List<String> lockExpressions, Tree tree, VisitorState state) {
    ImmutableSet.Builder<GuardedByExpression> builder = ImmutableSet.builder();
    for (String lockExpression : lockExpressions) {
      Optional<GuardedByExpression> guard =
          GuardedByBinder.bindString(lockExpression, GuardedBySymbolResolver.from(tree, state));
      if (!guard.isPresent()) {
        return Optional.empty();
      }
      builder.add(guard.get());
    }
    return Optional.of(builder.build());
  }
}
