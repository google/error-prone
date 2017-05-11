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
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.receiverSameAsArgument;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Points out if an object is compared to itself.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "SelfComparison",
  summary = "An object is compared to itself",
  category = JDK,
  severity = ERROR
)
public class SelfComparison extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to any instance method called "compareTo" with exactly one argument in which the
   * receiver is the same reference as the argument.
   *
   * <p>Example: foo.compareTo(foo)
   */
  private static final Matcher<MethodInvocationTree> COMPARE_TO_MATCHER =
      allOf(
          instanceMethod().onDescendantOf("java.lang.Comparable").named("compareTo"),
          receiverSameAsArgument(0));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!COMPARE_TO_MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(methodInvocationTree);
  }
}
