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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.REFACTORING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "TestExceptionRefactoring",
    summary = "Prefer assertThrows to @Test(expected=...)",
    severity = SUGGESTION,
    tags = REFACTORING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class TestExceptionRefactoring extends AbstractTestExceptionChecker {
  @Override
  protected Description handleStatements(
      MethodTree tree, VisitorState state, JCExpression expectedException, SuggestedFix baseFix) {
    return describeMatch(
        tree,
        buildFix(
            state,
            SuggestedFix.builder().merge(baseFix),
            expectedException,
            tree.getBody().getStatements()));
  }
}
