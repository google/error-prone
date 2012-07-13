// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.bugpatterns.array_equals;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Name;

/**
 * TODO(eaftan): tests -- more positive cases, start negative cases
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "ArrayEquals",
    summary = "equals used to compare arrays",
    explanation =
        "The equals method on an array compares for reference equality. If reference equality " +
        "is needed, == should be used instead for clarity. Otherwise, use Arrays.equals to " +
        "compare the contents of the arrays.",
    category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class ArrayEquals extends DescribingMatcher<MethodInvocationTree> {

  /**
   * Matches calls to an equals instance method in which both the receiver and the argument are
   * of an array type.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree t, VisitorState state) {
    return Matchers.allOf(
        methodSelect(instanceMethod(Matchers.<ExpressionTree>isArrayType(), "equals")),
        argument(0, Matchers.<ExpressionTree>isArrayType())).
        matches(t, state);
  }

  /**
   * Replaces instances of a.equals(b) with Arrays.equals(a, b). Also adds
   * the necessary import statement for java.util.Arrays.
   */
  @Override
  public Description describe(MethodInvocationTree t, VisitorState state) {
    String receiver = ((JCFieldAccess) t.getMethodSelect()).getExpression().toString();
    String arg = t.getArguments().get(0).toString();
    SuggestedFix fix = new SuggestedFix()
        .replace(t, "Arrays.equals(" + receiver + ", " + arg + ")")
        .addImport("import java.util.Arrays");
    return new Description(t, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> matcher = new ArrayEquals();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
