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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.StandardTags.REFACTORING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.annotation.Nullable;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ExpectedExceptionRefactoring",
    category = JUNIT,
    summary = "Prefer assertThrows to ExpectedException",
    severity = SUGGESTION,
    tags = REFACTORING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ExpectedExceptionRefactoring extends AbstractExpectedExceptionChecker
    implements VariableTreeMatcher {
  @Override
  protected Description handleMatch(
      MethodTree tree,
      VisitorState state,
      List<Tree> expectations,
      List<StatementTree> suffix,
      @Nullable StatementTree failure) {
    return describeMatch(tree, buildBaseFix(state, expectations, failure).build(suffix));
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!hasAnnotation(getSymbol(tree), "org.junit.Rule", state)) {
      return NO_MATCH;
    }
    if (!isSameType(
        getType(tree), state.getTypeFromString("org.junit.rules.ExpectedException"), state)) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.delete(tree));
  }
}
