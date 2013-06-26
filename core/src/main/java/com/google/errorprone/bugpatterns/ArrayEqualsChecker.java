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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "ArrayEquals",
    summary = "equals used to compare arrays",
    explanation =
        "The equals method on an array compares for reference equality. If reference equality " +
        "is needed, == should be used instead for clarity. Otherwise, use Arrays.equals to " +
        "compare the contents of the arrays.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ArrayEqualsChecker extends BugChecker {

  /**
   * Matches calls to an equals instance method in which both the receiver and the argument are
   * of an array type.
   */
  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> arrayEqualsMatcher = allOf(
      methodSelect(instanceMethod(Matchers.<ExpressionTree>isArrayType(), "equals")),
      argument(0, Matchers.<ExpressionTree>isArrayType()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (arrayEqualsMatcher.matches(tree, state)) {
      String receiver = ((JCFieldAccess) tree.getMethodSelect()).getExpression().toString();
      String arg = tree.getArguments().get(0).toString();
      SuggestedFix fix = new SuggestedFix()
          .replace(tree, "Arrays.equals(" + receiver + ", " + arg + ")")
          .addImport("java.util.Arrays");
      return new Description(tree, getDiagnosticMessage(), fix);
    }
    return null;
  }

}
