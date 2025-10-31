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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations.annotationsRelevantToNullness;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.lang.model.type.TypeKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "void-returning methods should not be annotated with nullness annotations,"
            + " since they cannot return null",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class NullableVoid extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym.getReturnType().getKind() != TypeKind.VOID) {
      return NO_MATCH;
    }
    var relevantAnnos = annotationsRelevantToNullness(tree.getModifiers().getAnnotations());
    if (relevantAnnos.isEmpty()) {
      return NO_MATCH;
    }
    var fix = SuggestedFix.builder();
    relevantAnnos.forEach(fix::delete);
    return describeMatch(relevantAnnos.get(0), fix.build());
  }
}
