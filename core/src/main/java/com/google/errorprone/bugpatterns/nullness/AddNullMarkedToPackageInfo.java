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
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;

/** Refactoring to add {@code @NullMarked} annotation to package-info.java files. */
@BugPattern(summary = "Apply @NullMarked to this package", severity = SUGGESTION)
public final class AddNullMarkedToPackageInfo extends BugChecker
    implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree unit, VisitorState state) {
    if (!isPackageInfo(unit)) {
      return NO_MATCH;
    }
    boolean nullMarkedAnnotationPresent =
        unit.getPackageAnnotations().stream()
            .anyMatch(
                annotation -> ASTHelpers.getAnnotationName(annotation).contentEquals("NullMarked"));
    if (nullMarkedAnnotationPresent) {
      return NO_MATCH;
    }
    return describeMatch(
        unit.getPackage(),
        SuggestedFix.builder()
            .prefixWith(unit.getPackage(), "@NullMarked ")
            .addImport("org.jspecify.annotations.NullMarked")
            .build());
  }

  private static boolean isPackageInfo(CompilationUnitTree tree) {
    String name = ASTHelpers.getFileName(tree);
    int idx = name.lastIndexOf('/');
    if (idx != -1) {
      name = name.substring(idx + 1);
    }
    return name.equals("package-info.java");
  }
}
