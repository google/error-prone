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
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.IS_DAGGER_COMPONENT;
import static com.google.errorprone.matchers.InjectMatchers.IS_SCOPING_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.annotations;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.util.List;

/**
 * This checker matches if a class has more than one annotation that is a scope annotation(that is,
 * the annotation is either annotated with Guice's {@code @ScopeAnnotation} or Javax's
 * {@code @Scope}).
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
  name = "InjectMoreThanOneScopeAnnotationOnClass",
  altNames = "MoreThanOneScopeAnnotationOnClass",
  summary = "A class can be annotated with at most one scope annotation.",
  explanation =
      "Annotating a class with more than one scope annotation is "
          + "invalid according to the JSR-330 specification. ",
  category = INJECT,
  severity = ERROR
)
public class MoreThanOneScopeAnnotationOnClass extends BugChecker implements ClassTreeMatcher {

  private static final MultiMatcher<Tree, AnnotationTree> SCOPE_ANNOTATION_MATCHER =
      annotations(AT_LEAST_ONE, IS_SCOPING_ANNOTATION);

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    MultiMatchResult<AnnotationTree> scopeAnnotationResult =
        SCOPE_ANNOTATION_MATCHER.multiMatchResult(classTree, state);
    if (scopeAnnotationResult.matches() && !IS_DAGGER_COMPONENT.matches(classTree, state)) {
      List<AnnotationTree> scopeAnnotations = scopeAnnotationResult.matchingNodes();
      if (scopeAnnotations.size() > 1) {
        return buildDescription(classTree)
            .setMessage(
                "This class is annotated with more than one scope annotation: "
                    + annotationDebugString(scopeAnnotations)
                    + ". However, classes can only have one scope annotation applied to them. "
                    + "Please remove all but one of them.")
            .build();
      }
    }
    return Description.NO_MATCH;
  }

  private String annotationDebugString(List<AnnotationTree> scopeAnnotations) {
    return Joiner.on(", ").join(scopeAnnotations);
  }
}
