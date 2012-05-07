/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.refactors.covariant_equals;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refactors.RefactoringMatcher;
import com.sun.source.tree.MethodTree;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.EnclosingClass.findEnclosingClass;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "covariant equals",
    summary = "equals() method doesn't override Object.equals()",
    explanation = "To be used by many libraries, an `equals` method must override `Object.equals`. " +
        "Defining a method which looks like `equals` but doesn't have the same signature is dangerous, " +
        "since comparisons will have different results depending on which `equals` is called.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR )
public class CovariantEquals extends RefactoringMatcher<MethodTree> {
  @Override
  public boolean matches(MethodTree methodTree, VisitorState state) {
    return allOf(
        methodIsNamed("equals"),
        methodReturns(state.getSymtab().booleanType),
        methodHasParameters(variableType(isSameType(findEnclosingClass(state))))
    ).matches(methodTree, state);
  }

  @Override
  public Refactor refactor(MethodTree methodTree, VisitorState state) {
    SuggestedFix fix = new SuggestedFix().replace(methodTree.getParameters().get(0).getType(), "Object");
    return new Refactor(methodTree, refactorMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private CovariantEquals matcher = new CovariantEquals();

    @Override
    public Void visitMethod(MethodTree node, VisitorState visitorState) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) &&
          matcher.matches(node, state)) {
        reportMatch(matcher, node, state);
      }

      return super.visitMethod(node, visitorState);
    }
  }
}
