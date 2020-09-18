/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MethodVisibility;
import com.sun.source.tree.MethodTree;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "PublicConstructorForAbstractClass",
    summary =
        "Constructors of an abstract class can be declared protected as there is never a need"
            + " for them to be public",
    explanation =
        "Abstract classes' constructors are only ever called by subclasses, never directly by"
            + " another class. Therefore they never need public constructors: protected is"
            + " accessible enough.",
    severity = SUGGESTION)
public class PublicConstructorForAbstractClass extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> PUBLIC_ABSTRACT_CONSTRUCTOR =
      allOf(
          methodIsConstructor(),
          methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
          enclosingClass(hasModifier(ABSTRACT)));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!PUBLIC_ABSTRACT_CONSTRUCTOR.matches(tree, state)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    SuggestedFixes.removeModifiers(tree, state, Modifier.PUBLIC).ifPresent(fix::merge);
    SuggestedFixes.addModifiers(tree, state, Modifier.PROTECTED).ifPresent(fix::merge);
    return describeMatch(tree, fix.build());
  }
}
