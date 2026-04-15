/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.hashCodeMethodDeclaration;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Overriding Throwable.equals() or hashCode() is discouraged.",
    severity = SeverityLevel.WARNING)
public final class ThrowableEqualsHashCode extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> MATCHER =
      anyOf(equalsMethodDeclaration(), hashCodeMethodDeclaration());

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (MATCHER.matches(tree, state)) {
      ClassSymbol classSymbol = getSymbol(tree).enclClass();
      if (classSymbol != null
          && isSubtype(classSymbol.type, state.getSymtab().throwableType, state)) {
        return describeMatch(tree, SuggestedFix.delete(tree));
      }
    }
    return Description.NO_MATCH;
  }
}
