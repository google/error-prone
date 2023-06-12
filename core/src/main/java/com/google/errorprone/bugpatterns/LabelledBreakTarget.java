/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LabeledStatementTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.LabeledStatementTree;

/** A BugPattern; see the summary. */
@BugPattern(summary = "Labels should only be used on loops.", severity = WARNING)
public class LabelledBreakTarget extends BugChecker implements LabeledStatementTreeMatcher {
  @Override
  public Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state) {
    switch (tree.getStatement().getKind()) {
      case DO_WHILE_LOOP:
      case ENHANCED_FOR_LOOP:
      case FOR_LOOP:
      case WHILE_LOOP:
        return NO_MATCH;
      default:
        break;
    }
    return describeMatch(tree);
  }
}
