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
import static com.google.errorprone.matchers.Matchers.allOf;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class AutoValueImmutableFields extends BugChecker implements ClassTreeMatcher {

  private static final String MESSAGE =
      "Value objects should be immutable, so if a property of one"
          + " is a collection then that collection should be immutable too."
          + " Please return %s here. Read more at "
          + "https://github.com/google/auto/blob/master/value/userguide/builders-howto.md#-use-a-collection-valued-property";

  private static final ImmutableMap<Matcher<MethodTree>, String> TYPE_MATCHER_TO_REPLACEMENT_MAP =
      ImmutableMap.<Matcher<MethodTree>, String>builder()
          .put(Matchers.methodReturns(Matchers.isArrayType()), "ImmutableList")
          .put(
              Matchers.methodReturns(Suppliers.typeFromString("java.util.Collection")),
              "ImmutableCollection")
          .put(Matchers.methodReturns(Suppliers.typeFromString("java.util.List")), "ImmutableList")
          .put(Matchers.methodReturns(Suppliers.typeFromString("java.util.Map")), "ImmutableMap")
          .put(
              Matchers.methodReturns(
                  Suppliers.typeFromString("com.google.common.collect.Multimap")),
              "ImmutableMultimap")
          .put(
              Matchers.methodReturns(
                  Suppliers.typeFromString("com.google.common.collect.ListMultimap")),
              "ImmutableListMultimap")
          .put(
              Matchers.methodReturns(
                  Suppliers.typeFromString("com.google.common.collect.SetMultimap")),
              "ImmutableSetMultimap")
          .put(
              Matchers.methodReturns(
                  Suppliers.typeFromString("com.google.common.collect.Multiset")),
              "ImmutableMultiset")
          .put(Matchers.methodReturns(Suppliers.typeFromString("java.util.Set")), "ImmutableSet")
          .put(
              Matchers.methodReturns(Suppliers.typeFromString("com.google.common.collect.Table")),
              "ImmutableTable")
          .build();

  private static final Matcher<MethodTree> METHOD_MATCHER =
      allOf(
          Matchers.<MethodTree>hasModifier(Modifier.PUBLIC),
          Matchers.<MethodTree>hasModifier(Modifier.ABSTRACT));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!ASTHelpers.hasAnnotation(tree, "com.google.auto.value.AutoValue", state)) {
      return NO_MATCH;
    }
    for (Tree memberTree : tree.getMembers()) {
      if (!(memberTree instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) memberTree;
      if (!METHOD_MATCHER.matches(methodTree, state)) {
        continue;
      }
      for (ImmutableMap.Entry<Matcher<MethodTree>, String> entry :
          TYPE_MATCHER_TO_REPLACEMENT_MAP.entrySet()) {
        if (entry.getKey().matches(methodTree, state)) {
          return buildDescription(methodTree)
              .setMessage(String.format(MESSAGE, entry.getValue()))
              .build();
        }
      }
    }
    return NO_MATCH;
  }
}
