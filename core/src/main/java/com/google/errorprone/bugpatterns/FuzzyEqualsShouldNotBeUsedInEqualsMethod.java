/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;

/** @author sulku@google.com (Marsela Sulku) */
@BugPattern(
  name = "FuzzyEqualsShouldNotBeUsedInEqualsMethod",
  summary = "DoubleMath.fuzzyEquals should never be used in an Object.equals() method",
  explanation =
      "From documentation: DoubleMath.fuzzyEquals is not transitive, so it is not suitable for use "
          + "in Object#equals implementations.",
  category = GUAVA,
  severity = ERROR
)
public class FuzzyEqualsShouldNotBeUsedInEqualsMethod extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodTree> EQUALS_MATCHER =
      allOf(
          methodIsNamed("equals"),
          methodHasVisibility(PUBLIC),
          methodReturns(BOOLEAN_TYPE),
          methodHasParameters(variableType(isSameType(OBJECT_TYPE))));

  private static final Matcher<MethodInvocationTree> CALL_TO_FUZZY_IN_EQUALS =
      allOf(
          staticMethod().onClass("com.google.common.math.DoubleMath").named("fuzzyEquals"),
          enclosingMethod(EQUALS_MATCHER));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (CALL_TO_FUZZY_IN_EQUALS.matches(tree, state)) {
      return describeMatch(tree);
    }

    return Description.NO_MATCH;
  }
}
