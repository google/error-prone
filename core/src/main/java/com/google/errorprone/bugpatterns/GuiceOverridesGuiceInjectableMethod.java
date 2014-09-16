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

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

import javax.lang.model.element.TypeElement;

/**
 * This checker matches methods that 
 *   1) are not themselves annotated with @Inject
 *     (neither javax.inject.Inject nor com.google.inject.Inject)
 *   2) descend from a method that is annotated with @com.google.inject.Inject
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "OverridesGuiceInjectableMethod", summary =
    "This method is not annotated with @Inject, but it overrides a "
    + "method that is annotated with @com.google.inject.Inject. Guice will inject this method,"
    + "and it is recommended to annotate it explicitly.",
    explanation =
        "Unlike with @javax.inject.Inject, if a method overrides a method annoatated with "
        + "@com.google.inject.Inject, Guice will inject it even if it itself is not annotated. "
        + "This differs from the behavior of methods that override @javax.inject.Inject methods "
        + "since according to the JSR-330 spec, a method that overrides a method annotated with "
        + "javax.inject.Inject will not be injected unless it iself is annotated with @Inject. " 
        + "Because of this difference, it is recommended that you annotate this method explicitly.",
         category = GUICE, severity = WARNING, maturity = EXPERIMENTAL)
public class GuiceOverridesGuiceInjectableMethod extends BugChecker implements MethodTreeMatcher {

  private static final String OVERRIDE_ANNOTATION = "java.lang.Override";
  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  private static final Matcher<MethodTree> INJECTABLE_METHOD_MATCHER = Matchers.<MethodTree>anyOf(
      hasAnnotation(GUICE_INJECT_ANNOTATION), hasAnnotation(JAVAX_INJECT_ANNOTATION));

  private static final Matcher<MethodTree> OVERRIDE_METHOD_MATCHER =
      Matchers.<MethodTree>hasAnnotation(OVERRIDE_ANNOTATION);

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    // if method is itself annotated with @Inject or it has no ancestor methods, return No_MATCH;
    if (!INJECTABLE_METHOD_MATCHER.matches(methodTree, state)
        && OVERRIDE_METHOD_MATCHER.matches(methodTree, state)) {
      MethodSymbol method = (MethodSymbol) ASTHelpers.getSymbol(methodTree);
      MethodSymbol superMethod = null;
      for (boolean checkSuperClass = true; checkSuperClass; method = superMethod) {
        superMethod = ASTHelpers.findSuperMethod(method, state.getTypes());
        if (isAnnotatedWith(superMethod, GUICE_INJECT_ANNOTATION)) {
          return describeMatch(methodTree, SuggestedFix.builder()
              .addImport("javax.inject.Inject")
              .prefixWith(methodTree, "@Inject\n")
              .build());
        }
        checkSuperClass = isAnnotatedWith(superMethod, OVERRIDE_ANNOTATION);
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean isAnnotatedWith(MethodSymbol method, String annotation) {
    for (Compound c : method.getAnnotationMirrors()) {
      if (((TypeElement) c.getAnnotationType().asElement()).getQualifiedName()
          .contentEquals(annotation)) {
        return true;
      }
    }
    return false;
  }
}
