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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.Matchers.typePredicateMatcher;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Flags com.sun.tools.javac.code.Type#equals usage.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "TypeEquals",
    summary =
        "com.sun.tools.javac.code.Type doesn't override Object.equals and instances are not"
            + " interned by javac, so testing types for equality should be done with"
            + " Types#isSameType instead",
    severity = WARNING)
public class TypeEqualsChecker extends BugChecker implements MethodInvocationTreeMatcher {

  private static final TypePredicate TYPE_MIRROR =
      isDescendantOf("javax.lang.model.type.TypeMirror");

  private static final Matcher<MethodInvocationTree> TYPE_EQUALS =
      anyOf(
          toType(MethodInvocationTree.class, instanceMethod().onClass(TYPE_MIRROR).named("equals")),
          allOf(
              staticEqualsInvocation(),
              argument(0, typePredicateMatcher(TYPE_MIRROR)),
              argument(1, typePredicateMatcher(TYPE_MIRROR))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!TYPE_EQUALS.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }
}
