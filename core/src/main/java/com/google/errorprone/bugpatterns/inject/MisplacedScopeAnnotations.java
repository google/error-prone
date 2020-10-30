/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_AT_INJECT;
import static com.google.errorprone.matchers.InjectMatchers.IS_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.IS_SCOPING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasProvidesAnnotation;
import static com.google.errorprone.matchers.Matchers.annotations;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.List;

/**
 * Bug checker for when a scope annotation is used at injection site, which does not have any effect
 * on the injected values.
 */
@BugPattern(
    name = "MisplacedScopeAnnotations",
    summary =
        "Scope annotations used as qualifier annotations don't have any effect."
            + " Move the scope annotation to the binding location or delete it.",
    severity = SeverityLevel.ERROR)
public class MisplacedScopeAnnotations extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {

  private static final MultiMatcher<VariableTree, AnnotationTree> IS_SCOPE_ANNOTATION =
      annotations(AT_LEAST_ONE, IS_SCOPING_ANNOTATION);

  private static final MultiMatcher<MethodTree, AnnotationTree> HAS_INJECT =
      annotations(AT_LEAST_ONE, IS_APPLICATION_OF_AT_INJECT);

  private static final Matcher<MethodTree> HAS_PROVIDES = hasProvidesAnnotation();

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!HAS_INJECT.matches(tree, state) && !HAS_PROVIDES.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    List<AnnotationTree> scopeAnnotations =
        tree.getParameters().stream()
            .flatMap(
                variable ->
                    IS_SCOPE_ANNOTATION.multiMatchResult(variable, state).matchingNodes().stream())
            .filter(annotation -> !IS_BINDING_ANNOTATION.matches(annotation, state))
            .collect(toImmutableList());

    if (scopeAnnotations.isEmpty()) {
      return Description.NO_MATCH;
    }
    return deleteAll(scopeAnnotations);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    boolean hasInject =
        tree.getModifiers().getAnnotations().stream()
            .anyMatch(annotation -> IS_APPLICATION_OF_AT_INJECT.matches(annotation, state));
    if (!hasInject) {
      return Description.NO_MATCH;
    }
    List<AnnotationTree> scopeAnnotations =
        tree.getModifiers().getAnnotations().stream()
            .filter(annotation -> IS_SCOPING_ANNOTATION.matches(annotation, state))
            .filter(annotation -> !IS_BINDING_ANNOTATION.matches(annotation, state))
            .collect(toImmutableList());
    if (scopeAnnotations.isEmpty()) {
      return Description.NO_MATCH;
    }
    return deleteAll(scopeAnnotations);
  }

  private Description deleteAll(List<AnnotationTree> scopeAnnotations) {
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    scopeAnnotations.forEach(fixBuilder::delete);
    return describeMatch(scopeAnnotations.get(0), fixBuilder.build());
  }
}
