/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Empty top-level type declarations should be omitted", severity = WARNING)
public final class EmptyTopLevelDeclaration extends BugChecker
    implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableList<Tree> toDelete =
        tree.getTypeDecls().stream()
            .filter(m -> m instanceof EmptyStatementTree)
            .collect(toImmutableList());
    if (toDelete.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    toDelete.forEach(
        x -> {
          int start = getStartPosition(x);
          int end = state.getEndPosition(x);
          if (end <= start) {
            // work around https://bugs.openjdk.org/browse/JDK-8324736
            end = start + 1;
          }
          fixBuilder.replace(start, end, "");
        });
    return describeMatch(toDelete.get(0), fixBuilder.build());
  }
}
