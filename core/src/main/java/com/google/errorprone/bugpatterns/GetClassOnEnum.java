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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "GetClassOnEnum",
  category = JDK,
  summary = "Calling getClass() on an enum may return a subclass of the enum type",
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class GetClassOnEnum extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ENUM_CLASS =
      instanceMethod().onDescendantOf(Enum.class.getName()).named("getClass").withParameters();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ENUM_CLASS.matches(tree, state)) {
      return describeMatch(
          tree,
          SuggestedFix.replace(
              state.getEndPosition(ASTHelpers.getReceiver(tree)),
              state.getEndPosition(tree),
              ".getDeclaringClass()"));
    }
    return Description.NO_MATCH;
  }
}
