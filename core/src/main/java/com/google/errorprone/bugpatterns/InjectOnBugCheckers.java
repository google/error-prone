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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;

/** A BugPattern; see the summary. */
@BugPattern(severity = WARNING, summary = "BugChecker constructors should be marked @Inject.")
public final class InjectOnBugCheckers extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    if (!symbol.isConstructor()) {
      return NO_MATCH;
    }
    if (isGeneratedConstructor(tree)) {
      return NO_MATCH;
    }
    if (hasDirectAnnotationWithSimpleName(tree, "Inject")) {
      return NO_MATCH;
    }
    if (!isSubtype(
            symbol.owner.type, state.getTypeFromString(BugChecker.class.getCanonicalName()), state)
        || !hasAnnotation(symbol.owner, BugPattern.class, state)) {
      return NO_MATCH;
    }
    if (tree.getParameters().isEmpty()
        || !tree.getParameters().stream()
            .allMatch(
                p ->
                    isSubtype(
                        getType(p),
                        state.getTypeFromString(ErrorProneFlags.class.getCanonicalName()),
                        state))) {
      return NO_MATCH;
    }
    var fix = SuggestedFix.builder();
    return describeMatch(
        tree,
        fix.prefixWith(tree, "@" + qualifyType(state, fix, "javax.inject.Inject") + " ").build());
  }
}
