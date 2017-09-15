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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "DeadThread",
  summary = "Thread created but not started",
  explanation = "The Thread is created with new, but is never started, and the reference is lost.",
  severity = ERROR
)
public class DeadThread extends BugChecker implements NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> NEW_THREAD =
      constructor().forClass("java.lang.Thread");

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!NEW_THREAD.matches(tree, state)) {
      return NO_MATCH;
    }
    if (state.getPath().getParentPath().getLeaf().getKind() != Kind.EXPRESSION_STATEMENT) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.postfixWith(tree, ".start()"));
  }
}
