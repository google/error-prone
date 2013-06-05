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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * @author adgar@google.com (Mike Edgar)
 */
@BugPattern(name = "ArrayToString",
    summary = "Calling toString on an array does not provide useful information",
    explanation =
        "The toString method on an array will print its identity, such as [I@4488aabb. This " +
        "is almost never needed. Use Arrays.toString to print a human-readable array summary.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ArrayToString extends DescribingMatcher<MethodInvocationTree> {

  /**
   * Matches calls to a toString instance method in which the receiver is an array type.
   */
  @Override
  public boolean matches(MethodInvocationTree t, VisitorState state) {
    return methodSelect(instanceMethod(Matchers.<ExpressionTree>isArrayType(), "toString"))
        .matches(t, state);
  }

  /**
   * Fixes instances of calling toString() on an array.  If the array is the result of calling
   * e.getStackTrace(), replaces e.getStackTrace().toString() with Guava's
   * Throwables.getStackTraceAsString(e).  Otherwise, replaces a.toString() with Arrays.toString(a).
   */
  @Override
  public Description describe(MethodInvocationTree t, VisitorState state) {
    SuggestedFix fix = new SuggestedFix();

    ExpressionTree receiverTree = ASTHelpers.getReceiver(t);
    if (receiverTree instanceof MethodInvocationTree &&
        instanceMethod(Matchers.<ExpressionTree>isSubtypeOf("java.lang.Throwable"), "getStackTrace")
        .matches(((MethodInvocationTree) receiverTree).getMethodSelect(), state)) {
      String throwable = ASTHelpers.getReceiver(receiverTree).toString();
      fix = fix.replace(t, "Throwables.getStackTraceAsString(" + throwable + ")")
          .addImport("com.google.common.base.Throwables");
    } else {
      String receiver = ((JCFieldAccess) t.getMethodSelect()).getExpression().toString();
      fix = fix.replace(t, "Arrays.toString(" + receiver + ")")
          .addImport("java.util.Arrays");
    }
    return new Description(t, getDiagnosticMessage(), fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> scannerMatcher = new ArrayToString();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, scannerMatcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
