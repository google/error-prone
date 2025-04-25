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
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Prefer -> switches for switch expressions", severity = WARNING)
public class TraditionalSwitchExpression extends BugChecker implements BugChecker.CaseTreeMatcher {

  @Override
  public Description matchCase(CaseTree tree, VisitorState state) {
    if (tree.getCaseKind().equals(CaseKind.RULE)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof SwitchExpressionTree)) {
      return NO_MATCH;
    }
    return describeMatch(parent);
  }
}
