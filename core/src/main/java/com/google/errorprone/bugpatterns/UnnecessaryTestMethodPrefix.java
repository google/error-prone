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

import static com.google.common.base.Ascii.toLowerCase;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethod;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_TEST_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.HashSet;

/** See the summary. */
@BugPattern(
    severity = SeverityLevel.WARNING,
    summary =
        "A `test` prefix for a JUnit4 test is redundant, and a holdover from JUnit3. The `@Test`"
            + " annotation makes it clear it's a test.")
public final class UnnecessaryTestMethodPrefix extends BugChecker
    implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    var fixBuilder = SuggestedFix.builder();
    var sites = new HashSet<Tree>();

    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (!TEST.matches(tree, state)) {
          return super.visitMethod(tree, unused);
        }
        String name = tree.getName().toString();
        if (!name.startsWith("test") || name.equals("test")) {
          return super.visitMethod(tree, unused);
        }
        var newName = toLowerCase(name.charAt(4)) + name.substring(5);
        fixBuilder.merge(renameMethod(tree, newName, state));
        sites.add(tree);
        return super.visitMethod(tree, unused);
      }
    }.scan(state.getPath(), null);

    var fix = fixBuilder.build();
    for (Tree site : sites) {
      state.reportMatch(describeMatch(site, fix));
    }

    return NO_MATCH;
  }

  private static final Matcher<Tree> TEST = hasAnnotation(JUNIT4_TEST_ANNOTATION);
}
