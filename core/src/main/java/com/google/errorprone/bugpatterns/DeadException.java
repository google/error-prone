/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import static com.google.errorprone.matchers.Matchers.*;
import static com.google.errorprone.suppliers.Suppliers.EXCEPTION_TYPE;
import static com.sun.source.tree.Tree.Kind.*;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.*;
import com.sun.source.tree.*;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "DeadException", altNames = "ThrowableInstanceNeverThrown",
    summary = "Exception created but not thrown",
    explanation =
        "The exception is created with new, but is not thrown, and the reference is lost.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class DeadException extends BugChecker implements NewClassTreeMatcher {

  public static final Matcher<Tree> MATCHER = allOf(
      parentNode(kindIs(EXPRESSION_STATEMENT)),
      isSubtypeOf(EXCEPTION_TYPE),
      not(enclosingMethod(JUnitMatchers.wouldRunInJUnit4)),
      anyOf(not(enclosingMethod(JUnitMatchers.isJunit3TestCase)),
            not(enclosingClass(JUnitMatchers.isJUnit3TestClass)))
  );

  @SuppressWarnings("unchecked")
  @Override
  public Description matchNewClass(NewClassTree newClassTree, VisitorState state) {
    if (!MATCHER.matches(newClassTree, state)) {
      return Description.NO_MATCH;
    }

    StatementTree parent = (StatementTree) state.getPath().getParentPath().getLeaf();

    boolean isLastStatement = anyOf(
        new Enclosing.BlockOrCase(lastStatement(Matchers.<StatementTree>isSame(parent))),
        // it could also be a bare if statement with no braces
        parentNode(parentNode(kindIs(IF))))
        .matches(newClassTree, state);

    SuggestedFix suggestedFix = new SuggestedFix();
    if (isLastStatement) {
      suggestedFix.prefixWith(newClassTree, "throw ");
    } else {
      suggestedFix.delete(parent);
    }
    return describeMatch(newClassTree, suggestedFix);
  }
}
