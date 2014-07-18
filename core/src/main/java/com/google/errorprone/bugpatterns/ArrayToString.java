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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * @author adgar@google.com (Mike Edgar)
 */
@BugPattern(name = "ArrayToString",
    summary = "Calling toString on an array does not provide useful information",
    explanation =
        "The toString method on an array will print its identity, such as [I@4488aabb. This " +
        "is almost never needed. Use Arrays.toString to print a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ArrayToString extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to Throwable.getStackTrace().
   */
  private static final Matcher<MethodInvocationTree> getStackTraceMatcher = methodSelect(
      instanceMethod(Matchers.<ExpressionTree>isSubtypeOf("java.lang.Throwable"), "getStackTrace"));

  /**
   * Matches calls to a toString instance method in which the receiver is an array type.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree methodTree, VisitorState state) {
    if (!methodSelect(instanceMethod(Matchers.<ExpressionTree>isArrayType(), "toString"))
        .matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    /*
     * Fixes instances of calling toString() on an array.  If the array is the result of calling
     * e.getStackTrace(), replaces e.getStackTrace().toString() with Guava's
     * Throwables.getStackTraceAsString(e).
     * Otherwise, replaces a.toString() with Arrays.toString(a).
     */
    Fix fix;

    ExpressionTree receiverTree = ASTHelpers.getReceiver(methodTree);
    if (receiverTree instanceof MethodInvocationTree &&
        getStackTraceMatcher.matches((MethodInvocationTree) receiverTree, state)) {
      String throwable = ASTHelpers.getReceiver(receiverTree).toString();
      fix = SuggestedFix.builder()
          .replace(methodTree, "Throwables.getStackTraceAsString(" + throwable + ")")
          .addImport("com.google.common.base.Throwables")
          .build();
    } else {
      fix = SuggestedFix.builder()
          .replace(methodTree, "Arrays.toString(" + receiverTree + ")")
          .addImport("java.util.Arrays")
          .build();
    }
    return describeMatch(methodTree, fix);
  }
}
