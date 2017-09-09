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
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.IS_DAGGER_COMPONENT;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "InjectScopeAnnotationOnInterfaceOrAbstractClass",
  summary = "Scope annotation on an interface or abstact class is not allowed",
  explanation = "Scoping annotations are not allowed on abstract types.",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ScopeAnnotationOnInterfaceOrAbstractClass extends BugChecker
    implements AnnotationTreeMatcher {

  /**
   * Matches annotations that are themselves annotated with with @ScopeAnnotation(Guice)
   * or @Scope(Javax).
   */
  private static final Matcher<AnnotationTree> SCOPE_ANNOTATION_MATCHER =
      Matchers.<AnnotationTree>anyOf(
          hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION));

  private static final Matcher<ClassTree> INTERFACE_AND_ABSTRACT_TYPE_MATCHER =
      new Matcher<ClassTree>() {
        @Override
        public boolean matches(ClassTree classTree, VisitorState state) {
          return classTree.getModifiers().getFlags().contains(ABSTRACT)
              || (ASTHelpers.getSymbol(classTree).flags() & Flags.INTERFACE) != 0;
        }
      };

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    Tree modified = getCurrentlyAnnotatedNode(state);
    if (SCOPE_ANNOTATION_MATCHER.matches(annotationTree, state)
        && modified instanceof ClassTree
        && !IS_DAGGER_COMPONENT.matches((ClassTree) modified, state)
        && INTERFACE_AND_ABSTRACT_TYPE_MATCHER.matches((ClassTree) modified, state)) {
      return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
    }
    return Description.NO_MATCH;
  }

  private Tree getCurrentlyAnnotatedNode(VisitorState state) {
    return state.getPath().getParentPath().getParentPath().getLeaf();
  }
}
