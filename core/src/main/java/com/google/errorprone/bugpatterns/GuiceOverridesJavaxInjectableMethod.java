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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;

import javax.lang.model.element.TypeElement;

/**
 * This checker matches methods that
 *   1) are not themselves annotated with @Inject
 *   2) descend from a method that is annotated with @javax.inject.Inject
 *   3) do not descent from a method that is annotated with @com.google.inject.Inject
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "OverridesJavaxInjectableMethod",
    summary = "This method is not annotated with @Inject, but it overrides a  method that is "
    + " annotated with @javax.inject.Inject.", 
    explanation = "According to the JSR-330 spec, a method that overrides a method annotated "
    + "with javax.inject.Inject will not be injected unless it iself is annotated with "
    + " @Inject", 
    category = GUICE, severity = ERROR, maturity = EXPERIMENTAL)
public class GuiceOverridesJavaxInjectableMethod extends BugChecker implements MethodTreeMatcher {

  private static final String OVERRIDE_ANNOTATION = "java.lang.Override";
  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";

  private static final Matcher<MethodTree> INJECTABLE_METHOD_MATCHER = Matchers.<MethodTree>anyOf(
      hasAnnotation(GUICE_INJECT_ANNOTATION), hasAnnotation(JAVAX_INJECT_ANNOTATION));

  private static final Matcher<MethodTree> OVERRIDE_METHOD_MATCHER =
      Matchers.<MethodTree>hasAnnotation(OVERRIDE_ANNOTATION);

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    // if method is itself annotated with @Inject or it has no ancestor methods, return NO_MATCH;
    if (INJECTABLE_METHOD_MATCHER.matches(methodTree, state)
        || !OVERRIDE_METHOD_MATCHER.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }
    boolean foundJavaxInject = false;
    MethodSymbol method = (MethodSymbol) ASTHelpers.getSymbol(methodTree);
    MethodSymbol superMethod = null;
    for (boolean checkSuperClass = true; checkSuperClass; method = superMethod) {
      superMethod = findSuperMethod(method, state);
      if (isAnnotatedWith(superMethod, GUICE_INJECT_ANNOTATION)) {
        return Description.NO_MATCH;
      }
      // is not necessarily a match even if we find javax Inject on an ancestor
      // since a higher up ancestor may have @com.google.inject.Inject
      foundJavaxInject = isAnnotatedWith(superMethod, JAVAX_INJECT_ANNOTATION);
      // check if there are ancestor methods
      checkSuperClass = isAnnotatedWith(superMethod, OVERRIDE_ANNOTATION);
    }
    if (foundJavaxInject) {
      return describeMatch(methodTree,
          SuggestedFix.builder()
              .addImport("javax.inject.Inject")
              .prefixWith(methodTree, "@Inject\n")
              .build());
    }
    return Description.NO_MATCH;
  }

  private MethodSymbol findSuperMethod(MethodSymbol method, VisitorState state) {
    TypeSymbol superClass = method.enclClass().getSuperclass().tsym;
    for (Symbol s : superClass.members().getElements()) {
      if (s.name.contentEquals(method.name)
          && method.overrides(s, superClass, state.getTypes(), true)) {
        return (MethodSymbol) s;
      }
    }
    return null;
  }

  // better to use matchers?
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
