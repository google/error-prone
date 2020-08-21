/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.common.io.Files;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;

/**
 * Java classes shouldn't use default package.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "DefaultPackage",
    summary = "Java classes shouldn't use default package",
    severity = WARNING)
public final class DefaultPackage extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return Description.NO_MATCH;
    }
    if (tree.getTypeDecls().stream().anyMatch(s -> isSuppressed(s))) {
      return Description.NO_MATCH;
    }
    if (tree.getTypeDecls().stream()
        .map(ASTHelpers::getSymbol)
        .filter(x -> x != null)
        .anyMatch(s -> !ASTHelpers.getGeneratedBy(s, state).isEmpty())) {
      return Description.NO_MATCH;
    }
    if (tree.getPackageName() != null) {
      return Description.NO_MATCH;
    }
    // module-info.* is a special file name so ignore it.
    if (Files.getNameWithoutExtension(ASTHelpers.getFileName(tree)).equals("module-info")) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }
}
