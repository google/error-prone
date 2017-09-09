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
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "AssistedInjectAndInjectOnSameConstructor",
  summary = "@AssistedInject and @Inject cannot be used on the same constructor.",
  explanation =
      "Using @AssistedInject and @Inject on the same constructor is a runtime" + "error in Guice.",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class AssistedInjectAndInjectOnSameConstructor extends BugChecker
    implements AnnotationTreeMatcher {

  /** Matches a method/constructor that is annotated with an @AssistedInject annotation. */
  private static final Matcher<MethodTree> HAS_ASSISTED_INJECT_MATCHER =
      hasAnnotation(ASSISTED_INJECT_ANNOTATION);

  /** Matches the @Inject and @Assisted inject annotations. */
  private static final Matcher<AnnotationTree> injectOrAssistedInjectMatcher =
      anyOf(
          isType(JAVAX_INJECT_ANNOTATION),
          isType(GUICE_INJECT_ANNOTATION),
          isType(ASSISTED_INJECT_ANNOTATION));

  @Override
  public Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (injectOrAssistedInjectMatcher.matches(annotationTree, state)) {
      Tree treeWithAnnotation = state.getPath().getParentPath().getParentPath().getLeaf();
      if (ASTHelpers.getSymbol(treeWithAnnotation).isConstructor()
          && hasInjectAnnotation().matches(treeWithAnnotation, state)
          && HAS_ASSISTED_INJECT_MATCHER.matches((MethodTree) treeWithAnnotation, state)) {
        return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
      }
    }
    return Description.NO_MATCH;
  }
}
