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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.binaryTree;
import static com.google.errorprone.matchers.Matchers.kindIs;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * @author adgar@google.com (Mike Edgar)
 */
@BugPattern(name = "ArrayToStringConcatenation",
    summary = "Implicit toString used on an array (String + Array)",
    explanation = "When concatenating an array to a string, the toString method on an array will " +
        "yield its identity, such as [I@4488aabb. This is almost never needed. Use " +
        "Arrays.toString to obtain a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ArrayToStringConcatenation extends BugChecker implements BinaryTreeMatcher {

  private static final Matcher<ExpressionTree> arrayMatcher =
      Matchers.<ExpressionTree>isArrayType();

  private static final Matcher<BinaryTree> concatenationMatcher = allOf(
      kindIs(Kind.PLUS),
      binaryTree(arrayMatcher, Matchers.<ExpressionTree>isSameType("java.lang.String")));

  /**
   * Matches strings added with arrays.
   */
  @Override
  public Description matchBinary(BinaryTree t, VisitorState state) {
    if (!concatenationMatcher.matches(t, state)) {
      return Description.NO_MATCH;
    }

    /*
     * Replace instances of implicit array toString() calls due to string concatenation with
     * Arrays.toString(array). Also adds the necessary import statement for java.util.Arrays.
     */
    final String replacement;
    String leftOperand = t.getLeftOperand().toString();
    String rightOperand = t.getRightOperand().toString();
    if (arrayMatcher.matches(t.getLeftOperand(), state)) {
      replacement = "Arrays.toString(" + leftOperand + ") + " + rightOperand;
    } else {
      replacement = leftOperand + " + Arrays.toString(" + rightOperand + ")";
    }
    Fix fix = SuggestedFix.builder()
        .replace(t, replacement)
        .addImport("java.util.Arrays")
        .build();
    return describeMatch(t, fix);
  }
}
