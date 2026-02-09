/*
 * Copyright 2025 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/** Refactoring to add {@code @NullMarked} annotation to all top level Java class declarations. */
@BugPattern(summary = "Apply @NullMarked to this class", severity = SUGGESTION)
public final class AddNullMarkedToClass extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree unit, VisitorState state) {
    for (Tree typeDecl : unit.getTypeDecls()) {
      if (!(typeDecl instanceof ClassTree classTree)) {
        continue;
      }
      if (!ASTHelpers.hasDirectAnnotationWithSimpleName(classTree, "NullMarked")) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String nullMarked =
            SuggestedFixes.qualifyType(state, fix, "org.jspecify.annotations.NullMarked");
        fix.prefixWith(classTree, "@" + nullMarked + " ");
        state.reportMatch(describeMatch(classTree, fix.build()));
      }
    }
    return NO_MATCH;
  }
}
