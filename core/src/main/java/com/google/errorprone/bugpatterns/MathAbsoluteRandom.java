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
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;

/** @author kayco@google.com (Kayla Walker) */
@BugPattern(
    name = "MathAbsoluteRandom",
    summary =
        "Math.abs does not always give a positive result. Please consider other "
            + "methods for positive random numbers.",
    severity = WARNING)
public class MathAbsoluteRandom extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> RANDOM_ABS_VAL =
      allOf(
          staticMethod().onClass("java.lang.Math").named("abs"),
          argument(
              0,
              anyOf(
                  instanceMethod().onDescendantOf("java.util.Random"),
                  staticMethod().onClass("java.lang.Math").named("random"))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {

    if (RANDOM_ABS_VAL.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
