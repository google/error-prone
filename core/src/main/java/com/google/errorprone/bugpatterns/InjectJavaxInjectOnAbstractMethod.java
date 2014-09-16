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
import static com.sun.source.tree.Tree.Kind.METHOD;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "JavaxInjectOnAbstractMethod",
    summary = "Abstract methods are not injectable with javax.inject.Inject.", explanation =
        "The javax.inject.Inject annotation cannot go on an abstract method as per "
        + "the JSR-330 spec. This is in line with the fact that if a class overrides a "
        + "method that was annotated with javax.inject.Inject, and the subclass method"
        + "is not annotated, the subclass method will not be injected.\n\n"
        + "See http://docs.oracle.com/javaee/6/api/javax/inject/Inject.html\n"
        + "and https://code.google.com/p/google-guice/wiki/JSR330" + " ", category = INJECT,
    severity = ERROR, maturity = EXPERIMENTAL)
public class InjectJavaxInjectOnAbstractMethod extends BugChecker
    implements AnnotationTreeMatcher {

  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  Matcher<AnnotationTree> javaxInjectAnnotationMatcher = new Matcher<AnnotationTree>() {
    @Override
    public boolean matches(AnnotationTree annotationTree, VisitorState state) {
      return (ASTHelpers.getSymbol(annotationTree).equals(
          state.getSymbolFromString(JAVAX_INJECT_ANNOTATION)));
    }
  };

  @Override
  public Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (!javaxInjectAnnotationMatcher.matches(annotationTree, state)) {
      return Description.NO_MATCH;
    }
    Tree annotatedNode = state.getPath().getParentPath().getParentPath().getLeaf();
    if (isMethod(annotatedNode) && isAbstract(annotatedNode)) {
      return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
    }
    return Description.NO_MATCH;
  }


  private static boolean isMethod(Tree tree) {
    return tree.getKind().equals(METHOD);
  }

  private static boolean isAbstract(Tree tree) {
    return ((MethodTree) tree).getModifiers().getFlags().contains(ABSTRACT);
  }
}
