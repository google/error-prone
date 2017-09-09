/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isField;
import static javax.lang.model.element.Modifier.FINAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.VariableTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "GuiceInjectOnFinalField",
  summary =
      "Although Guice allows injecting final fields, doing so is disallowed because the injected "
          + "value may not be visible to other threads.",
  explanation = "See https://github.com/google/guice/wiki/InjectionPoints#how-guice-injects",
  category = GUICE,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class InjectOnFinalField extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<VariableTree> FINAL_FIELD_WITH_GUICE_INJECT =
      allOf(isField(), hasModifier(FINAL), hasAnnotation(GUICE_INJECT_ANNOTATION));

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (FINAL_FIELD_WITH_GUICE_INJECT.matches(tree, state)) {
      return describeMatch(tree, SuggestedFixes.removeModifiers(tree, state, FINAL));
    }
    return Description.NO_MATCH;
  }
}
