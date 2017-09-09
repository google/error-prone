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

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * This checker matches methods that 1) are not themselves annotated with @Inject 2) descend from a
 * method that is annotated with @javax.inject.Inject 3) do not descent from a method that is
 * annotated with @com.google.inject.Inject
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
  name = "OverridesJavaxInjectableMethod",
  summary =
      "This method is not annotated with @Inject, but it overrides a method that is "
          + " annotated with @javax.inject.Inject. The method will not be Injected.",
  category = GUICE,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class OverridesJavaxInjectableMethod extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    // if method is itself annotated with @Inject or it has no ancestor methods, return NO_MATCH;
    if (hasInjectAnnotation().matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    boolean foundJavaxInject = false;
    for (MethodSymbol superMethod :
        ASTHelpers.findSuperMethods(ASTHelpers.getSymbol(methodTree), state.getTypes())) {

      // With a Guice annotation, Guice will still inject the subclass-overridden method.
      if (ASTHelpers.hasAnnotation(superMethod, GUICE_INJECT_ANNOTATION, state)) {
        return Description.NO_MATCH;
      }

      // is not necessarily a match even if we find javax Inject on an ancestor
      // since a higher up ancestor may have @com.google.inject.Inject
      foundJavaxInject |= ASTHelpers.hasAnnotation(superMethod, JAVAX_INJECT_ANNOTATION, state);
    }

    if (foundJavaxInject) {
      return describeMatch(
          methodTree,
          SuggestedFix.builder()
              .addImport(JAVAX_INJECT_ANNOTATION)
              .prefixWith(methodTree, "@Inject\n")
              .build());
    }
    return Description.NO_MATCH;
  }
}
