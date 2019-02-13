/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayList;
import java.util.List;

/** Migrate users who use JSR 330 scopes on Dagger modules. */
@BugPattern(
    name = "ScopeOnModule",
    summary = "Scopes on modules have no function and will soon be an error.",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class ScopeOnModule extends BugChecker implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!DaggerAnnotations.isAnyModule().matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    List<SuggestedFix> fixes = new ArrayList<>();
    for (AnnotationTree annotation : classTree.getModifiers().getAnnotations()) {
      Symbol annotationType = getSymbol(annotation.getAnnotationType());
      if (hasAnnotation(annotationType, "javax.inject.Scope", state)) {
        fixes.add(SuggestedFix.delete(annotation));
      }
    }
    if (fixes.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(classTree).addAllFixes(fixes).build();
  }
}
