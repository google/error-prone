/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;
import static com.google.errorprone.suppliers.Suppliers.genericTypeOf;
import static com.google.errorprone.suppliers.Suppliers.receiverInstance;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "CollectionIncompatibleType",
    summary = "Incompatible type as argument to non-generic Java collections method.",
    explanation = "Java Collections API has non-generic methods such as Collection.contains(Object). " +
        "If an argument is given which isn't of a type that may appear in the collection, these " +
        "methods always return false. This commonly happens when the type of a collection is refactored " +
        "and the developer relies on the Java compiler to detect callsites where the collection access " +
        "needs to be updated.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class CollectionIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> isGenericCollectionsMethod =
      methodSelect(Matchers.<ExpressionTree>anyOf(
          isDescendantOfMethod("java.util.Map", "get(java.lang.Object)"),
          isDescendantOfMethod("java.util.Collection", "contains(java.lang.Object)"),
          isDescendantOfMethod("java.util.Collection", "remove(java.lang.Object)")));

  private static final Matcher<ExpressionTree> castableToMethodReceiverType =
      isCastableTo(genericTypeOf(receiverInstance(), 0));

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> matcher = allOf(
      isGenericCollectionsMethod,
      argument(0, not(castableToMethodReceiverType))
  );

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, new SuggestedFix().replace(tree, "false"));
  }
}
