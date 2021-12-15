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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypesWithUndefinedEquality;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Arrays;

/**
 * Highlights use of {@code Collection#contains} (and others) with types that do not have
 * well-defined equals.
 */
@BugPattern(
    name = "CollectionUndefinedEquality",
    summary = "This type does not have well-defined equals behavior.",
    tags = StandardTags.FRAGILE_CODE,
    severity = WARNING)
public final class CollectionUndefinedEquality extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {
  // NOTE: this is a bit crude; methods like `containsValue` are still likely to be subject to
  // equality constraints on the value type.
  private static final Matcher<ExpressionTree> TYPES_NOT_DEPENDING_ON_OBJECT_EQUALITY =
      anyMethod()
          .onDescendantOfAny(
              "java.util.IdentityHashMap",
              "java.util.IdentityHashSet",
              "java.util.SortedMap",
              "java.util.SortedSet");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return match(tree, state);
  }

  public Description match(ExpressionTree tree, VisitorState state) {
    MatchResult result = ContainmentMatchers.firstNonNullMatchResult(tree, state);
    if (result == null) {
      return NO_MATCH;
    }
    if (TYPES_NOT_DEPENDING_ON_OBJECT_EQUALITY.matches(tree, state)) {
      return NO_MATCH;
    }

    return Arrays.stream(TypesWithUndefinedEquality.values())
        .filter(
            b ->
                b.matchesType(result.sourceType(), state)
                    || b.matchesType(result.targetType(), state))
        .findFirst()
        .map(
            b ->
                buildDescription(tree)
                    .setMessage(b.shortName() + " does not have well-defined equals behavior.")
                    .build())
        .orElse(NO_MATCH);
  }
}
