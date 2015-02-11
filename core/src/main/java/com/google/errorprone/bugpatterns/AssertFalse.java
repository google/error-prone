/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import com.google.errorprone.bugpatterns.BugChecker.AssertTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ExpressionTree;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.BOOLEAN_LITERAL;
import static com.sun.tools.javac.tree.JCTree.JCLiteral;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
@BugPattern(name = "AssertFalse",
    summary = "Assert false should not be used",
    explanation = "Assert false indicates that the code should never be"
                  + " executed except in case of a bug. It is better to"
                  + " throw an AssertionError so an exception is raised"
                  + " regardless whether assertions are enabled.",
    category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class AssertFalse extends BugChecker implements AssertTreeMatcher {

  @Override
  public Description matchAssert(AssertTree tree, VisitorState state) {
    ExpressionTree condition = tree.getCondition();
    if (kindIs(BOOLEAN_LITERAL).matches(condition, state)) {
      Boolean value = (Boolean) ((JCLiteral) condition).getValue();
      if (!value) {
        Fix fix = SuggestedFix.builder()
            .replace(tree, "throw new AssertionError()")
            .build();
        return describeMatch(tree, fix);
      } else {
        return Description.NO_MATCH;
      }
    } else {
      return Description.NO_MATCH;
    }
  }
}
