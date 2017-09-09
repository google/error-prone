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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ThrowTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ThrowNull",
  category = JDK,
  summary = "Throwing 'null' always results in a NullPointerException being thrown.",
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ThrowNull extends BugChecker implements ThrowTreeMatcher {
  @Override
  public Description matchThrow(ThrowTree tree, VisitorState state) {
    return (tree.getExpression().getKind() == NULL_LITERAL)
        ? describeMatch(
            tree, SuggestedFix.replace(tree.getExpression(), "new NullPointerException()"))
        : NO_MATCH;
  }
}
