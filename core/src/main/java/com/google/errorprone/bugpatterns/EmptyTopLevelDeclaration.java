/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "EmptyTopLevelDeclaration",
  summary = "Empty top-level type declaration",
  explanation =
      "A semi-colon at the top level of a Java file is treated as an empty type declaration"
          + " in the grammar, but it's confusing and unnecessary.",
  category = JDK,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class EmptyTopLevelDeclaration extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    List<Tree> toDelete = new ArrayList<>();
    for (Tree member : tree.getTypeDecls()) {
      if (member.getKind() == Tree.Kind.EMPTY_STATEMENT) {
        toDelete.add(member);
      }
    }
    if (toDelete.isEmpty()) {
      return Description.NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    for (Tree member : toDelete) {
      fixBuilder.delete(member);
    }
    return describeMatch(toDelete.get(0), fixBuilder.build());
  }
}
