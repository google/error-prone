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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.compoundAssignment;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
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
    summary = "Imlpicit toString used on an array (String += Array)",
    explanation =
        "When concatenating-and-assigning an array to a string, the implicit toString call on " +
        "the array will yield its identity, such as [I@4488aabb. This is almost never needed. " +
        "Use Arrays.toString to obtain a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ArrayToStringCompoundAssignment
    extends DescribingMatcher<CompoundAssignmentTree> {

  private static final Matcher<CompoundAssignmentTree> assignmentMatcher =
      compoundAssignment(
          Kind.PLUS_ASSIGNMENT,
          Matchers.<ExpressionTree>isSameType("java.lang.String"),
          Matchers.<ExpressionTree>isArrayType());

  /**
   * Matchers when a string concatenates-and-assigns an array.
   */
  @Override
  public boolean matches(CompoundAssignmentTree t, VisitorState state) {
    return assignmentMatcher.matches(t, state);
  }

  /**
   * Replaces instances of implicit array toString() calls due to string
   * concatenation-and-assignment with Arrays.toString(array). Also adds
   * the necessary import statement for java.util.Arrays.
   */
  @Override
  public Description describe(CompoundAssignmentTree t, VisitorState state) {
    final String replacement;
    String receiver = t.getVariable().toString();
    String expression = t.getExpression().toString();
    SuggestedFix fix = new SuggestedFix()
        .replace(t, receiver + " += Arrays.toString(" + expression + ")")
        .addImport("java.util.Arrays");
    return new Description(t, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<CompoundAssignmentTree> scannerMatcher =
        new ArrayToStringCompoundAssignment();

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, scannerMatcher);
      return super.visitCompoundAssignment(node, visitorState);
    }
  }
}
