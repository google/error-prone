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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.AnnotationType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Matches classes that have two or more constructors annotated with @Inject.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */

@BugPattern(name = "MoreThanOneInjectableConstructor",
    summary = "A class may not have more than one injectable constructor.", 
    explanation = "Having more than one injectable constructor will throw a runtime error in "
        + "compliant JSR-330 frameworks such as Guice or Dagger",
        category = INJECT, severity = ERROR, maturity = EXPERIMENTAL)
public class InjectMoreThanOneInjectableConstructor extends BugChecker
    implements MethodTreeMatcher {

  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  private static final Matcher<MethodTree> INJECTABLE_METHOD_MATCHER = Matchers.<MethodTree>anyOf(
      hasAnnotation(GUICE_INJECT_ANNOTATION), hasAnnotation(JAVAX_INJECT_ANNOTATION));

  private static final AnnotationType JAVAX_INJECT_MATCHER =
      new AnnotationType(JAVAX_INJECT_ANNOTATION);
  
  private static final AnnotationType GUICE_INJECT_MATCHER =
      new AnnotationType(GUICE_INJECT_ANNOTATION);

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    int numberOfInjectableConstructors = 0;
    if (ASTHelpers.getSymbol(methodTree).isConstructor()
        && INJECTABLE_METHOD_MATCHER.matches(methodTree, state)) {
      for (Tree member : ((ClassTree) state.getPath().getParentPath().getLeaf()).getMembers()) {
        if (ASTHelpers.getSymbol(member).isConstructor()
            && INJECTABLE_METHOD_MATCHER.matches((MethodTree) member, state)) {
          numberOfInjectableConstructors++;
        }
      }
    }
    return numberOfInjectableConstructors > 1 ? describe(methodTree,state) : Description.NO_MATCH;
  }

  public Description describe(MethodTree methodTree, VisitorState state) {
    for (AnnotationTree annotation : methodTree.getModifiers().getAnnotations()) {
      if (JAVAX_INJECT_MATCHER.matches(annotation, state)
          || GUICE_INJECT_MATCHER.matches(annotation, state)) {
        return describeMatch(
            annotation, SuggestedFix.delete(annotation));
      }
    }
    throw new IllegalStateException(
        "Expected to find more than once constructor annotated with @Inject");
  }
}
