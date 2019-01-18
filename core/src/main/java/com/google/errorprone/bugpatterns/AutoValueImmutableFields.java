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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;

import com.google.common.collect.ImmutableListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Map;
import javax.lang.model.element.Modifier;

/**
 * Flags mutable collections in AutoValue.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "AutoValueImmutableFields",
    summary = "AutoValue recommends using immutable collections",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,
    documentSuppression = false)
public class AutoValueImmutableFields extends BugChecker implements ClassTreeMatcher {

  private static final String MESSAGE =
      "AutoValue instances should be deeply immutable. Therefore, we recommend returning %s "
          + "instead. Read more at "
          + "http://goo.gl/qWo9sC"
      ;

  private static final ImmutableListMultimap<String, Matcher<MethodTree>> REPLACEMENT_TO_MATCHERS =
      ImmutableListMultimap.<String, Matcher<MethodTree>>builder()
          .put("ImmutableCollection", returning("java.util.Collection"))
          .putAll(
              "ImmutableList",
              methodReturns(isArrayType()),
              returning("java.util.List"),
              returning("java.util.ArrayList"),
              returning("java.util.LinkedList"),
              returning("com.google.common.collect.ImmutableList.Builder"))
          .putAll(
              "ImmutableMap",
              returning("java.util.Map"),
              returning("java.util.HashMap"),
              returning("java.util.LinkedHashMap"),
              returning("com.google.common.collect.ImmutableMap.Builder"))
          .putAll(
              "ImmutableSortedMap",
              returning("java.util.SortedMap"),
              returning("java.util.TreeMap"),
              returning("com.google.common.collect.ImmutableSortedMap.Builder"))
          .putAll(
              "ImmutableBiMap",
              returning("com.google.common.collect.BiMap"),
              returning("com.google.common.collect.ImmutableBiMap.Builder"))
          .putAll(
              "ImmutableSet",
              returning("java.util.Set"),
              returning("java.util.HashSet"),
              returning("java.util.LinkedHashSet"),
              returning("com.google.common.collect.ImmutableSet.Builder"))
          .putAll(
              "ImmutableSortedSet",
              returning("java.util.SortedSet"),
              returning("java.util.TreeSet"),
              returning("com.google.common.collect.ImmutableSortedSet.Builder"))
          .putAll(
              "ImmutableMultimap",
              returning("com.google.common.collect.Multimap"),
              returning("com.google.common.collect.ImmutableMultimap.Builder"))
          .putAll(
              "ImmutableListMultimap",
              returning("com.google.common.collect.ListMultimap"),
              returning("com.google.common.collect.ImmutableListMultimap.Builder"))
          .putAll(
              "ImmutableSetMultimap",
              returning("com.google.common.collect.SetMultimap"),
              returning("com.google.common.collect.ImmutableSetMultimap.Builder"))
          .putAll(
              "ImmutableMultiset",
              returning("com.google.common.collect.Multiset"),
              returning("com.google.common.collect.ImmutableMultiset.Builder"))
          .putAll(
              "ImmutableSortedMultiset",
              returning("com.google.common.collect.SortedMultiset"),
              returning("com.google.common.collect.ImmutableSortedMultiset.Builder"))
          .putAll(
              "ImmutableTable",
              returning("com.google.common.collect.Table"),
              returning("com.google.common.collect.ImmutableTable.Builder"))
          .putAll(
              "ImmutableRangeMap",
              returning("com.google.common.collect.RangeMap"),
              returning("com.google.common.collect.ImmutableRangeMap.Builder"))
          .putAll(
              "ImmutableRangeSet",
              returning("com.google.common.collect.RangeSet"),
              returning("com.google.common.collect.ImmutableRangeSet.Builder"))
          .putAll(
              "ImmutablePrefixTrie",
              returning("com.google.common.collect.PrefixTrie"),
              returning("com.google.common.collect.ImmutablePrefixTrie.Builder"))
          .build();

  private static Matcher<MethodTree> returning(String type) {
    return methodReturns(typeFromString(type));
  }

  private static final Matcher<MethodTree> ABSTRACT_MATCHER = hasModifier(Modifier.ABSTRACT);

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (ASTHelpers.hasAnnotation(tree, "com.google.auto.value.AutoValue", state)) {
      for (Tree memberTree : tree.getMembers()) {
        if (memberTree instanceof MethodTree && !isSuppressed(memberTree)) {
          MethodTree methodTree = (MethodTree) memberTree;
          if (ABSTRACT_MATCHER.matches(methodTree, state)) {
            for (Map.Entry<String, Matcher<MethodTree>> entry : REPLACEMENT_TO_MATCHERS.entries()) {
              if (entry.getValue().matches(methodTree, state)) {
                state.reportMatch(
                    buildDescription(methodTree)
                        .setMessage(String.format(MESSAGE, entry.getKey()))
                        .build());
              }
            }
          }
        }
      }
    }
    return NO_MATCH;
  }
}
