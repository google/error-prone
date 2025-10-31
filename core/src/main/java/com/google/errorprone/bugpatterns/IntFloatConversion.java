/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFix.prefixWith;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.TargetType.targetType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.TargetType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.TypeTag;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Conversion from int to float may lose precision; use an explicit cast to float if this"
            + " was intentional",
    severity = WARNING)
public class IntFloatConversion extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * int to float conversions aren't always problematic, this specific issue is that there are float
   * and double overloads, and when passing an int the float will be resolved in situations where
   * double precision may be desired.
   */
  private static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.staticMethod()
          .onClass("java.lang.Math")
          .named("scalb")
          .withParameters("float", "int");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    Tree arg = tree.getArguments().get(0);
    if (!getType(arg).hasTag(TypeTag.INT)) {
      return NO_MATCH;
    }
    TargetType targetType = targetType(state);
    if (targetType == null || !targetType.type().hasTag(TypeTag.DOUBLE)) {
      return NO_MATCH;
    }
    return describeMatch(arg, prefixWith(arg, "(double) "));
  }
}
