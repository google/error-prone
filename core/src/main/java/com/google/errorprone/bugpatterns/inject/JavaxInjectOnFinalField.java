/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.inject.ElementPredicates.isFinalField;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_JAVAX_INJECT;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "JavaxInjectOnFinalField",
  summary = "@javax.inject.Inject cannot be put on a final field.",
  explanation =
      "According to the JSR-330 spec, the @javax.inject.Inject annotation "
          + "cannot go on final fields.",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class JavaxInjectOnFinalField extends BugChecker implements AnnotationTreeMatcher {

  @Override
  public Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (IS_APPLICATION_OF_JAVAX_INJECT.matches(annotationTree, state)) {
      if (isFinalField(getSymbol(state.getPath().getParentPath().getParentPath().getLeaf()))) {
        return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
      }
    }
    return Description.NO_MATCH;
  }
}
