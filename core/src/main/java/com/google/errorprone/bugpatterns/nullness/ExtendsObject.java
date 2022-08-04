/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;

/** A bugpattern: see the summary. */
@BugPattern(
    summary = "`T extends Object` is redundant" + " (unless you are using the Checker Framework).",
    severity = SeverityLevel.WARNING)
public final class ExtendsObject extends BugChecker implements TypeParameterTreeMatcher {
  private static final String NON_NULL = "org.checkerframework.checker.nullness.qual.NonNull";

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    for (Tree bound : tree.getBounds()) {
      if (!state.getTypes().isSameType(getType(bound), state.getSymtab().objectType)) {
        continue;
      }
      if (!(bound instanceof AnnotatedTypeTree)) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String nonNull = SuggestedFixes.qualifyType(state, fix, NON_NULL);
        return describeMatch(bound, fix.prefixWith(bound, format(" @%s ", nonNull)).build());
      }
    }
    return NO_MATCH;
  }
}
