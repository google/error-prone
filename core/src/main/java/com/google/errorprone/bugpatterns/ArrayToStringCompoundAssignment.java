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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.compoundAssignment;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * @author adgar@google.com (Mike Edgar)
 */
@BugPattern(name = "ArrayToStringCompoundAssignment",
    summary = "Implicit toString used on an array (String += Array)",
    explanation =
        "When concatenating-and-assigning an array to a string, the implicit toString call on " +
        "the array will yield its identity, such as [I@4488aabb. This is almost never needed. " +
        "Use Arrays.toString to obtain a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ArrayToStringCompoundAssignment
    extends BugChecker implements CompoundAssignmentTreeMatcher {

  private static final Matcher<CompoundAssignmentTree> assignmentMatcher =
      compoundAssignment(
          Kind.PLUS_ASSIGNMENT,
          Matchers.<ExpressionTree>isSameType("java.lang.String"),
          Matchers.<ExpressionTree>isArrayType());

  /**
   * Matchers when a string concatenates-and-assigns an array.
   */
  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree t, VisitorState state) {
    if (!assignmentMatcher.matches(t, state)) {
      return Description.NO_MATCH;
    }

    /*
     * Replace instances of implicit array toString() calls due to string
     * concatenation-and-assignment with Arrays.toString(array). Also adds
     * the necessary import statement for java.util.Arrays.
     */
    String receiver = t.getVariable().toString();
    String expression = t.getExpression().toString();
    Fix fix = SuggestedFix.builder()
        .replace(t, receiver + " += Arrays.toString(" + expression + ")")
        .addImport("java.util.Arrays")
        .build();
    return describeMatch(t, fix);
  }
}
