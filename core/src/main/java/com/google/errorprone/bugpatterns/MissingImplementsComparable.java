/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.compareToMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Classes implementing valid compareTo function should implement Comparable interface",
    severity = WARNING)
public class MissingImplementsComparable extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> COMPARABLE_WITHOUT_OVERRIDE =
      allOf(compareToMethodDeclaration(), not(hasAnnotation("java.lang.Override")));

  private static final Matcher<ClassTree> IS_COMPARABLE =
      Matchers.isSubtypeOf("java.lang.Comparable");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!COMPARABLE_WITHOUT_OVERRIDE.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    ClassTree enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (enclosingClass == null || IS_COMPARABLE.matches(enclosingClass, state)) {
      return Description.NO_MATCH;
    }
    if (!isSameType(
        getType(getOnlyElement(tree.getParameters())), getType(enclosingClass), state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }
}
