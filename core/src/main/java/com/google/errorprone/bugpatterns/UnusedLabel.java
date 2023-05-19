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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.util.TreeScanner;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;

/** A BugPattern; see the summary. */
@BugPattern(summary = "This label is unused.", severity = SeverityLevel.WARNING)
public final class UnusedLabel extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<Name, LabeledStatementTree> labels = new HashMap<>();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitLabeledStatement(LabeledStatementTree tree, Void unused) {
        labels.put(tree.getLabel(), tree);
        return super.visitLabeledStatement(tree, null);
      }
    }.scan(state.getPath(), null);

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitBreak(BreakTree tree, Void unused) {
        labels.remove(tree.getLabel());
        return super.visitBreak(tree, null);
      }

      @Override
      public Void visitContinue(ContinueTree tree, Void unused) {
        labels.remove(tree.getLabel());
        return super.visitContinue(tree, null);
      }
    }.scan(tree, null);

    for (LabeledStatementTree label : labels.values()) {
      state.reportMatch(
          describeMatch(
              label,
              SuggestedFix.replace(
                  getStartPosition(label), getStartPosition(label.getStatement()), "")));
    }
    return NO_MATCH;
  }
}
