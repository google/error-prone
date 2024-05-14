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
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Very deeply nested code may lead to StackOverflowErrors during compilation",
    severity = WARNING)
public class DeeplyNested extends BugChecker implements CompilationUnitTreeMatcher {

  private final int maxDepth;

  @Inject
  DeeplyNested(ErrorProneFlags flags) {
    maxDepth = flags.getInteger("DeeplyNested:MaxDepth").orElse(1000);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Tree result =
        new SuppressibleTreePathScanner<Tree, Integer>(state) {

          @Override
          public Tree scan(Tree tree, Integer depth) {
            if (depth > maxDepth) {
              return tree;
            }
            return super.scan(tree, depth + 1);
          }

          @Override
          public Tree reduce(Tree r1, Tree r2) {
            return r1 != null ? r1 : r2;
          }
        }.scan(state.getPath(), 0);
    if (result != null) {
      return describeMatch(result);
    }
    return NO_MATCH;
  }
}
