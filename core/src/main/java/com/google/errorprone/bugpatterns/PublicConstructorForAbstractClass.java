/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MethodVisibility;

import com.sun.source.tree.MethodTree;

import javax.lang.model.element.Modifier;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static javax.lang.model.element.Modifier.ABSTRACT;

@BugPattern(
    name = "PublicConstructorForAbstractClass",
    summary =
        "Constructor on an abstract class can be declared protected as there is never a need for it to be public",
    explanation =
        "In the case of abstract classes, their constructors are only called by their"
            + " concrete subclasses, not directly by the caller so modifier can be made more restrictive",
    category = JDK,
    severity = SUGGESTION,
    providesFix = BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class PublicConstructorForAbstractClass extends BugChecker implements
    BugChecker.MethodTreeMatcher {

  private static final Matcher<MethodTree> TO_MATCH =
      allOf(methodIsConstructor(),
          methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
          enclosingClass(hasModifier(ABSTRACT)));

  @Override
  public Description matchMethod(
      MethodTree tree, VisitorState state) {
    if (TO_MATCH.matches(tree, state)) {
      SuggestedFix suggestedFix = SuggestedFix.builder()
          .merge(SuggestedFixes.removeModifiers(tree, state, Modifier.PUBLIC))
          .merge(SuggestedFixes.addModifiers(tree, state, Modifier.PROTECTED)).build();
      return describeMatch(tree, suggestedFix);
    }
    return Description.NO_MATCH;
  }
}
