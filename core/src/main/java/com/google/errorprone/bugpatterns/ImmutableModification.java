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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Set;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ImmutableModification",
    summary =
        "Modifying an immutable collection is guaranteed to throw an exception and leave the"
            + " collection unmodified",
    severity = ERROR)
public class ImmutableModification extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {

  public static final ImmutableSetMultimap<String, String> ILLEGAL_METHODS_BY_BASE_TYPE =
      ImmutableSetMultimap.<String, String>builder()
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

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return match(tree, ASTHelpers.getSymbol(tree), state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, ASTHelpers.getSymbol(tree), state);
  }

  private Description match(ExpressionTree tree, MethodSymbol method, VisitorState state) {
    if (method == null) {
      return NO_MATCH;
    }

    Set<String> forbiddenTypes =
        ILLEGAL_METHODS_BY_BASE_TYPE.inverse().get(method.getSimpleName().toString());
    if (forbiddenTypes == null) {
      return NO_MATCH;
    }
    Type ownerType = method.owner.type;
    for (String forbiddenType : forbiddenTypes) {
      if (ASTHelpers.isSubtype(ownerType, state.getTypeFromString(forbiddenType), state)) {
        return describeMatch(tree);
      }
    }

    return NO_MATCH;
  }
}
