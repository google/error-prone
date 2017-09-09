/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_AT_INJECT;
import static com.google.errorprone.matchers.InjectMatchers.IS_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.IS_SCOPING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasProvidesAnnotation;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import java.util.ArrayList;
import java.util.List;

/** @author Nick Glorioso (glorioso@google.com) */
@BugPattern(
  name = "QualifierOrScopeOnInjectMethod",
  category = Category.INJECT,
  summary =
      "Qualifiers/Scope annotations on @Inject methods don't have any effect."
          + " Move the qualifier annotation to the binding location.",
  severity = SeverityLevel.ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class QualifierOrScopeOnInjectMethod extends BugChecker implements MethodTreeMatcher {

  private static final MultiMatcher<MethodTree, AnnotationTree> QUALIFIER_ANNOTATION_FINDER =
      annotations(AT_LEAST_ONE, anyOf(IS_BINDING_ANNOTATION, IS_SCOPING_ANNOTATION));

  private static final MultiMatcher<MethodTree, AnnotationTree> HAS_INJECT =
      annotations(AT_LEAST_ONE, IS_APPLICATION_OF_AT_INJECT);

  private static final Matcher<MethodTree> PROVIDES_METHOD = hasProvidesAnnotation();

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MultiMatchResult<AnnotationTree> qualifierAnnotations =
        QUALIFIER_ANNOTATION_FINDER.multiMatchResult(tree, state);
    MultiMatchResult<AnnotationTree> injectAnnotations = HAS_INJECT.multiMatchResult(tree, state);
    if (!(qualifierAnnotations.matches() && injectAnnotations.matches())) {
      return Description.NO_MATCH;
    }

    Builder fixBuilder = SuggestedFix.builder();
    List<AnnotationTree> matchingAnnotations = qualifierAnnotations.matchingNodes();

    // If we're looking at an @Inject constructor, move the scope annotation to the class instead,
    // and delete all of the other qualifiers
    if (ASTHelpers.getSymbol(tree).isConstructor()) {
      List<AnnotationTree> scopes = new ArrayList<>();
      List<AnnotationTree> qualifiers = new ArrayList<>();
      for (AnnotationTree annoTree : matchingAnnotations) {
        (IS_SCOPING_ANNOTATION.matches(annoTree, state) ? scopes : qualifiers).add(annoTree);
      }

      ClassTree outerClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
      scopes.forEach(
          a -> {
            fixBuilder.delete(a);
            fixBuilder.prefixWith(outerClass, state.getSourceForNode(a) + " ");
          });
      deleteAll(qualifiers, fixBuilder);
      return describeMatch(tree, fixBuilder.build());
    }

    // If it has a "@Provides" annotation as well as an @Inject annotation, removing the @Inject
    // should be semantics-preserving (since Modules aren't generally themselves @Injected).
    if (PROVIDES_METHOD.matches(tree, state)) {
      deleteAll(injectAnnotations.matchingNodes(), fixBuilder);
      return describeMatch(injectAnnotations.matchingNodes().get(0), fixBuilder.build());
    }

    // Don't know what else to do here, deleting is the no-op change.
    deleteAll(matchingAnnotations, fixBuilder);
    return describeMatch(matchingAnnotations.get(0), fixBuilder.build());
  }

  private static void deleteAll(List<AnnotationTree> scopes, Builder fixBuilder) {
    scopes.forEach(fixBuilder::delete);
  }
}
