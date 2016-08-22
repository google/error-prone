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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_JAVAX_INJECT;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "JavaxInjectOnAbstractMethod",
  summary = "Abstract methods are not injectable with javax.inject.Inject.",
  explanation =
      "The javax.inject.Inject annotation cannot go on an abstract method as per "
          + "the JSR-330 spec. This is in line with the fact that if a class overrides a "
          + "method that was annotated with javax.inject.Inject, and the subclass method"
          + "is not annotated, the subclass method will not be injected.\n\n"
          + "See http://docs.oracle.com/javaee/6/api/javax/inject/Inject.html\n"
          + "and https://github.com/google/guice/wiki/JSR330"
          + " ",
  category = INJECT,
  severity = ERROR,
  maturity = EXPERIMENTAL
)
public class JavaxInjectOnAbstractMethod extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> IS_ABSTRACT = Matchers.hasModifier(ABSTRACT);

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!IS_ABSTRACT.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    for (AnnotationTree annotationTree : methodTree.getModifiers().getAnnotations()) {
      if (IS_APPLICATION_OF_JAVAX_INJECT.matches(annotationTree, state)) {
        return describeMatch(annotationTree, delete(annotationTree));
      }
    }

    return Description.NO_MATCH;
  }
}
