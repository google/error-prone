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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "StaticMockMember",
    summary =
        "@Mock members of test classes shouldn't share state between tests and preferably be"
            + " non-static",
    severity = WARNING)
public final class StaticMockMember extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<Tree> STATIC_MOCK =
      allOf(hasModifier(STATIC), hasAnnotation("org.mockito.Mock"));

  @Override
  public Description matchVariable(VariableTree varTree, VisitorState state) {
    if (!STATIC_MOCK.matches(varTree, state)) {
      return NO_MATCH;
    }
    Optional<SuggestedFix> optionalFix =
        SuggestedFixes.removeModifiers(varTree, state, Modifier.STATIC);

    if (!optionalFix.isPresent()) {
      return NO_MATCH;
    }
    if (SuggestedFixes.compilesWithFix(optionalFix.get(), state)) {
      return describeMatch(varTree, optionalFix.get());
    } else {
      return describeMatch(varTree);
    }
  }
}
